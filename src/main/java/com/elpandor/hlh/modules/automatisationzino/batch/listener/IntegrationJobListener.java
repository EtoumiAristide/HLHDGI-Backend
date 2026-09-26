package com.elpandor.hlh.modules.automatisationzino.batch.listener;

import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.services.FichierSystemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationJobListener implements JobExecutionListener {

    private final FichierSystemeService fichierSystemeService;
    private final FichierSourceRepository fichierSourceRepository;

    private static final String STATUT_ERROR = "ERROR";

    private Instant startTime;
    private LocalDateTime jobStartTime;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = Instant.now();
        // Référence utilisée en fin de job pour ne compter que les erreurs survenues PENDANT ce run
        // (et non des erreurs plus anciennes déjà en base issues d'un run précédent).
        jobStartTime = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault());
        fichierSystemeService.creerRepertoireTemp();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duration = Duration.between(startTime, Instant.now());

        fichierSystemeService.nettoyerFichiersTemporaires();

        // Erreurs remontées au niveau du job lui-même (ex: échec de l'étape de découverte).
        // Depuis que FichierProcessor n'interrompt plus le chunk sur erreur, les échecs de
        // traitement fichier par fichier ne se retrouvent plus ici : ils sont comptés
        // séparément ci-dessous, directement depuis fichiers_source.
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("Erreurs rencontrées pendant l'exécution:");
            jobExecution.getAllFailureExceptions().forEach(e ->
                    log.error("   - {}", e.getMessage())
            );
        }

        Integer nbNouveauxFichiers = (Integer) jobExecution.getExecutionContext()
                .get("nbNouveauxFichiers");
        if (nbNouveauxFichiers != null) {
            log.info("Nouveaux fichiers découverts: {}", nbNouveauxFichiers);
        }

        long nbFichiersEnErreur = fichierSourceRepository.countByStatutDepuis(STATUT_ERROR, jobStartTime);
        if (nbFichiersEnErreur > 0) {
            log.error("⚠️  ALERTE: {} fichier(s) en statut ERROR suite à ce run (voir table fichiers_source / historique_envois pour le détail)",
                    nbFichiersEnErreur);
        } else {
            log.info("Aucun fichier en erreur suite à ce run");
        }

        log.info("Durée totale du job: {}s", duration.toSeconds());
        log.info("═══════════════════════════════════════════════════════");
    }
}