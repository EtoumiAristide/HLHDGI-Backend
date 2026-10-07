package com.elpandor.hlh.modules.impressionzino.application;

import com.elpandor.hlh.modules.impressionzino.application.dto.TicketAImprimer;
import com.elpandor.hlh.modules.impressionzino.application.event.PrintJobsCreatedEvent;
import com.elpandor.hlh.modules.impressionzino.domain.PrintJobStatut;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintJobEntity;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintJobJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cœur de la file d'impression (outbox) : création des jobs, réservation par les agents,
 * accusés de réception, reprise sur incident.
 * <p>
 * Garantie : livraison « au moins une fois ». Un job réservé mais jamais acquitté (poste éteint, réseau coupé)
 * est remis en file à l'expiration de son bail ; c'est l'agent qui évite la double impression grâce à son journal local.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintJobService {

    public static final String ACK_PRINTED = "PRINTED";
    public static final String ACK_FAILED = "FAILED";

    private static final int MESSAGE_MAX = 1000;

    private final PrintJobJpaRepository repository;
    private final ApplicationEventPublisher events;

    /** Durée pendant laquelle un job remis à un agent lui est réservé avant d'être redistribué. */
    @Value("${impression.lease-seconds:60}")
    private int leaseSeconds;

    /** Nombre total de tentatives avant échec définitif. */
    @Value("${impression.max-tentatives:5}")
    private int maxTentatives;

    /** Délai avant nouvelle tentative après un échec signalé par l'agent : tentative n × ce délai. */
    @Value("${impression.retry-backoff-seconds:30}")
    private int retryBackoffSeconds;

    // ------------------------------------------------------------------ création

    /**
     * Crée un job par ticket. Transaction indépendante : l'impression dépend de l'acceptation par la FNE,
     * pas de la réussite de la comptabilité locale, et un incident ici ne doit pas annuler le traitement appelant.
     *
     * @return le nombre de jobs réellement créés (les numéros de facture déjà connus sont ignorés)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int planifier(String codeEtablissement, Long fichierSourceId, String nomFichierSource,
                         List<TicketAImprimer> tickets) {
        String code = CodeEtablissement.normaliser(codeEtablissement);
        if (code == null) {
            log.error("Impression non planifiée pour la source {} : code établissement absent", nomFichierSource);
            return 0;
        }

        int crees = 0;
        for (TicketAImprimer ticket : tickets) {
            if (repository.existsByNumeroFacture(ticket.numeroFacture())) {
                log.warn("Un job d'impression existe déjà pour la facture {} : ignoré", ticket.numeroFacture());
                continue;
            }
            repository.save(PrintJobEntity.builder()
                    .codeEtablissement(code)
                    .fichierSourceId(fichierSourceId)
                    .nomFichierSource(nomFichierSource)
                    .numeroFacture(ticket.numeroFacture())
                    .typeDocument(ticket.typeDocument())
                    .payloadJson(ticket.payloadJson())
                    .statut(PrintJobStatut.PENDING)
                    .build());
            crees++;
        }

        if (crees > 0) {
            // Traité APRÈS validation de cette transaction : le job est alors visible des agents.
            events.publishEvent(new PrintJobsCreatedEvent(code));
        }
        log.info("{} job(s) d'impression créé(s) pour l'établissement {} (source {})", crees, code, nomFichierSource);
        return crees;
    }

    // ------------------------------------------------------------------ côté agent

    /**
     * Réserve jusqu'à {@code max} jobs livrables pour l'agent (statut DISPATCHED + bail).
     * Transaction courte, jamais maintenue pendant l'attente du long-polling.
     */
    @Transactional
    public List<PrintJobEntity> reserver(String codeEtablissement, String agentId, int max) {
        LocalDateTime now = LocalDateTime.now();
        List<PrintJobEntity> jobs = repository.verrouillerLivrables(codeEtablissement, now, max);
        for (PrintJobEntity job : jobs) {
            job.setStatut(PrintJobStatut.DISPATCHED);
            job.setAgentId(agentId);
            job.setLeaseUntil(now.plusSeconds(leaseSeconds));
            job.setTentatives(job.getTentatives() + 1);
        }
        return jobs;
    }

    /**
     * Enregistre l'accusé de réception d'un agent. Idempotent : un second accusé PRINTED est sans effet.
     * L'impression physique fait foi : PRINTED est accepté même si le bail avait expiré entre-temps.
     */
    @Transactional
    public PrintJobEntity acquitter(UUID jobId, String codeEtablissementAgent, String agentId,
                                    String statutAgent, String imprimante, String message) {
        PrintJobEntity job = repository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job d'impression inconnu"));

        if (!job.getCodeEtablissement().equals(codeEtablissementAgent)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce job concerne un autre établissement");
        }

        LocalDateTime now = LocalDateTime.now();

        if (ACK_PRINTED.equals(statutAgent)) {
            if (job.getStatut() != PrintJobStatut.PRINTED) {
                job.setStatut(PrintJobStatut.PRINTED);
                job.setDateImpression(now);
                job.setImprimante(imprimante);
                job.setAgentId(agentId);
                job.setLeaseUntil(null);
                job.setDernierMessageErreur(null);
                log.info("Job {} (facture {}) imprimé par {} sur {}", jobId, job.getNumeroFacture(), agentId, imprimante);
            }
            return job;
        }

        // ACK_FAILED : seul l'agent qui détient le bail peut le signaler
        if (job.getStatut() != PrintJobStatut.DISPATCHED || !agentId.equals(job.getAgentId())) {
            log.info("Échec ignoré pour le job {} (statut {}, agent {}) : l'état courant fait foi",
                    jobId, job.getStatut(), job.getAgentId());
            return job;
        }

        job.setDernierMessageErreur(tronquer(message));
        job.setLeaseUntil(null);
        if (job.getTentatives() >= maxTentatives) {
            job.setStatut(PrintJobStatut.FAILED);
            log.error("Job {} (facture {}) en échec définitif après {} tentatives : {}",
                    jobId, job.getNumeroFacture(), job.getTentatives(), message);
        } else {
            job.setStatut(PrintJobStatut.PENDING);
            job.setAgentId(null);
            job.setAvailableAt(now.plusSeconds((long) retryBackoffSeconds * job.getTentatives()));
            log.warn("Job {} en échec (tentative {}/{}), nouvelle tentative à {} : {}",
                    jobId, job.getTentatives(), maxTentatives, job.getAvailableAt(), message);
        }
        return job;
    }

    // ------------------------------------------------------------------ maintenance

    /** Remet en file les jobs dont le bail a expiré sans accusé, ou les passe en échec définitif. */
    @Transactional
    public void recycler() {
        LocalDateTime now = LocalDateTime.now();
        int remis = repository.remettreEnFileBauxExpires(PrintJobStatut.PENDING, PrintJobStatut.DISPATCHED,
                now, maxTentatives, "Bail expiré sans accusé de réception de l'agent");
        int echecs = repository.echouerBauxExpires(PrintJobStatut.FAILED, PrintJobStatut.DISPATCHED,
                now, maxTentatives, "Échec définitif : aucun accusé de réception après " + maxTentatives + " tentatives");
        if (remis > 0 || echecs > 0) {
            log.warn("Baux expirés : {} job(s) remis en file, {} en échec définitif", remis, echecs);
        }
    }

    @Transactional(readOnly = true)
    public List<String> codesAvecJobsLivrables() {
        return repository.codesAvecJobsLivrables(PrintJobStatut.PENDING, LocalDateTime.now());
    }

    // ------------------------------------------------------------------ administration

    @Transactional(readOnly = true)
    public Page<PrintJobEntity> lister(String codeEtablissement, PrintJobStatut statut, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        String code = CodeEtablissement.normaliser(codeEtablissement);
        if (code != null && statut != null) {
            return repository.findByCodeEtablissementAndStatut(code, statut, pageable);
        }
        if (code != null) {
            return repository.findByCodeEtablissement(code, pageable);
        }
        if (statut != null) {
            return repository.findByStatut(statut, pageable);
        }
        return repository.findAll(pageable);
    }

    /** Relance manuelle d'un job en échec définitif (remise à zéro des tentatives). */
    @Transactional
    public PrintJobEntity relancer(UUID jobId) {
        PrintJobEntity job = repository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job d'impression inconnu"));
        if (job.getStatut() != PrintJobStatut.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seul un job en échec définitif peut être relancé (statut actuel : " + job.getStatut() + ")");
        }
        job.setStatut(PrintJobStatut.PENDING);
        job.setTentatives(0);
        job.setAgentId(null);
        job.setLeaseUntil(null);
        job.setAvailableAt(LocalDateTime.now());
        events.publishEvent(new PrintJobsCreatedEvent(job.getCodeEtablissement()));
        log.info("Job {} (facture {}) relancé manuellement", jobId, job.getNumeroFacture());
        return job;
    }

    private static String tronquer(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MESSAGE_MAX ? message : message.substring(0, MESSAGE_MAX);
    }
}
