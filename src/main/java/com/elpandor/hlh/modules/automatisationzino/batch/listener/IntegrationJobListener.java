package com.elpandor.hlh.modules.automatisationzino.batch.listener;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.services.FichierSystemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationJobListener implements JobExecutionListener {

    private final FichierSystemeService fichierSystemeService;
    private Instant startTime;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = Instant.now();
        fichierSystemeService.creerRepertoireTemp();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duration = Duration.between(startTime, Instant.now());


        fichierSystemeService.nettoyerFichiersTemporaires();

        if (jobExecution.getAllFailureExceptions() != null &&
                !jobExecution.getAllFailureExceptions().isEmpty()) {
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

        log.info("═══════════════════════════════════════════════════════");
    }
}

