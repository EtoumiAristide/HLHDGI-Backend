package com.elpandor.hlh.modules.impressionzino.application;

import com.elpandor.hlh.modules.impressionzino.application.event.PrintJobsCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tâche d'arrière-plan qui « déclenche le module d'impression ».
 * <ul>
 *   <li><b>Réveil immédiat</b> : dès que les jobs d'une facturation sont validés en base, les agents en attente
 *       du point de vente concerné sont réveillés. Le traitement est asynchrone : la réponse HTTP à l'appelant
 *       de {@code /tickets/toFacture} n'attend jamais l'impression.</li>
 *   <li><b>Filet de sécurité</b> (toutes les quelques secondes) : remise en file des baux expirés et réveil des
 *       établissements ayant des jobs livrables (retards après échec, plusieurs instances du backend...).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImpressionDispatchTask {

    private final PrintSignalRegistry signalRegistry;
    private final PrintJobService printJobService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onJobsCreated(PrintJobsCreatedEvent event) {
        log.info("Réveil des agents d'impression de l'établissement {}", event.codeEtablissement());
        signalRegistry.signaler(event.codeEtablissement());
    }

    @Scheduled(fixedDelayString = "${impression.housekeeping-ms:5000}")
    public void maintenance() {
        try {
            printJobService.recycler();
            for (String code : printJobService.codesAvecJobsLivrables()) {
                signalRegistry.signaler(code);
            }
        } catch (Exception e) {
            log.error("Maintenance de la file d'impression en échec : {}", e.getMessage(), e);
        }
    }
}
