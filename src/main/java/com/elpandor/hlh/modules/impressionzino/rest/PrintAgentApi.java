package com.elpandor.hlh.modules.impressionzino.rest;

import com.elpandor.hlh.modules.impressionzino.application.PrintJobService;
import com.elpandor.hlh.modules.impressionzino.application.PrintSignalRegistry;
import com.elpandor.hlh.modules.impressionzino.application.dto.PrintAckRequest;
import com.elpandor.hlh.modules.impressionzino.application.dto.PrintJobDto;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentEntity;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentJpaRepository;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintJobEntity;
import com.elpandor.hlh.modules.impressionzino.security.PrintAgentAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * API consommée par les agents d'impression (application desktop des points de vente).
 * Toutes les connexions sont initiées par le poste : aucun port à ouvrir chez le client.
 * <p>
 * Authentification : en-tête {@code X-Agent-Key} (voir {@code PrintAgentAuthFilter}).
 * <ul>
 *   <li>{@code GET  /me}            : identité de l'agent (vérification de la clé au démarrage)</li>
 *   <li>{@code POST /jobs/poll}     : long-polling, renvoie les jobs de SON établissement (204 si rien)</li>
 *   <li>{@code POST /jobs/{id}/ack} : accusé de réception PRINTED / FAILED</li>
 * </ul>
 * Le long-polling garde un thread du serveur pendant l'attente (50 s maximum, sous le délai usuel de 60 s des
 * reverse proxies). C'est adapté à quelques dizaines de postes ; au-delà, voir la note d'évolution du document
 * d'architecture (DeferredResult ou WebSocket, le contrat de job ne change pas).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/print-agent")
@RequiredArgsConstructor
public class PrintAgentApi {

    private static final int ATTENTE_MAX_SECONDES = 50;
    private static final int LOT_MAX = 20;

    private final PrintJobService printJobService;
    private final PrintSignalRegistry signalRegistry;
    private final PrintAgentJpaRepository agentRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        PrintAgentEntity agent = agentCourant(request);
        agentRepository.toucher(agent.getId(), LocalDateTime.now());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("agentId", agent.getAgentId());
        body.put("codeEtablissement", agent.getCodeEtablissement());
        body.put("serverTime", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/jobs/poll")
    public ResponseEntity<List<PrintJobDto>> poll(
            HttpServletRequest request,
            @RequestParam(name = "waitSeconds", defaultValue = "25") int waitSeconds,
            @RequestParam(name = "max", defaultValue = "5") int max) {

        PrintAgentEntity agent = agentCourant(request);
        String code = agent.getCodeEtablissement();
        int attente = Math.max(0, Math.min(waitSeconds, ATTENTE_MAX_SECONDES));
        int lot = Math.max(1, Math.min(max, LOT_MAX));

        agentRepository.toucher(agent.getId(), LocalDateTime.now());

        List<PrintJobEntity> jobs = printJobService.reserver(code, agent.getAgentId(), lot);

        if (jobs.isEmpty() && attente > 0) {
            CompletableFuture<Void> signal = signalRegistry.enregistrer(code);
            try {
                // Revérification APRÈS enregistrement : évite de rater un job créé entre les deux étapes.
                jobs = printJobService.reserver(code, agent.getAgentId(), lot);
                if (jobs.isEmpty()) {
                    try {
                        signal.get(attente, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        // Pas de nouveau job : cas normal, l'agent se reconnectera.
                    } catch (ExecutionException e) {
                        log.warn("Signal d'impression en erreur pour {} : {}", code, e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return ResponseEntity.noContent().build();
                    }
                    jobs = printJobService.reserver(code, agent.getAgentId(), lot);
                }
            } finally {
                signalRegistry.retirer(code, signal);
            }
        }

        if (jobs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        log.info("{} job(s) d'impression remis à l'agent {} (établissement {})", jobs.size(), agent.getAgentId(), code);
        return ResponseEntity.ok(jobs.stream().map(j -> PrintJobDto.from(j, objectMapper)).toList());
    }

    @PostMapping("/jobs/{id}/ack")
    public ResponseEntity<Map<String, Object>> ack(HttpServletRequest request,
                                                   @PathVariable("id") UUID id,
                                                   @RequestBody PrintAckRequest body) {
        PrintAgentEntity agent = agentCourant(request);
        String statut = body.statut() == null ? "" : body.statut().trim().toUpperCase(Locale.ROOT);
        if (!PrintJobService.ACK_PRINTED.equals(statut) && !PrintJobService.ACK_FAILED.equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statut attendu : PRINTED ou FAILED");
        }

        PrintJobEntity job = printJobService.acquitter(id, agent.getCodeEtablissement(), agent.getAgentId(),
                statut, body.imprimante(), body.message());

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("jobId", job.getId().toString());
        reponse.put("statut", job.getStatut().name());
        return ResponseEntity.ok(reponse);
    }

    private PrintAgentEntity agentCourant(HttpServletRequest request) {
        Object agent = request.getAttribute(PrintAgentAuthFilter.ATTR_AGENT);
        if (agent instanceof PrintAgentEntity entity) {
            return entity;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agent non authentifié");
    }
}
