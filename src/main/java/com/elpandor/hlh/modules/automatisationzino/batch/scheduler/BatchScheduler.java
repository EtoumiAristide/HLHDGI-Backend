package com.elpandor.hlh.modules.automatisationzino.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job integrationJob;

    @Scheduled(cron = "${batch.scheduler.cron:0 0 */2 * * ?}")
    public void executerJob() {
        log.info("Déclenchement du job planifié");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(integrationJob, params);

            log.info("Job exécuté avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution du job planifié: {}", e.getMessage(), e);
        }
    }
}