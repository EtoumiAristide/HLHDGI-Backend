package com.elpandor.hlh.modules.automatisationzino.batch.configuration;

import com.elpandor.hlh.modules.automatisationzino.batch.listener.IntegrationJobListener;
import com.elpandor.hlh.modules.automatisationzino.batch.processor.FichierProcessor;
import com.elpandor.hlh.modules.automatisationzino.batch.reader.FichierSourceReader;
import com.elpandor.hlh.modules.automatisationzino.batch.tasklet.DecouverteFichiersTasklet;
import com.elpandor.hlh.modules.automatisationzino.batch.writer.FichierWriter;
import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DecouverteFichiersTasklet decouverteFichiersTasklet;
    private final FichierSourceReader reader;
    private final FichierProcessor processor;
    private final FichierWriter writer;
    private final IntegrationJobListener jobListener;

    @Bean
    public Job integrationJob() {
        return new JobBuilder("integrationJob", jobRepository)
                .listener(jobListener)
                .start(stepDecouverte())
                .next(stepTraitement())
                .build();
    }

    @Bean
    public Step stepDecouverte() {
        return new StepBuilder("stepDecouverte", jobRepository)
                .tasklet(decouverteFichiersTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step stepTraitement() {
        return new StepBuilder("stepTraitement", jobRepository)
                .<FichierSource, FichierSource>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
