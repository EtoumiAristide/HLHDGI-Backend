package com.elpandor.hlh.modules.automatisationzino.batch.tasklet;

import com.elpandor.hlh.modules.automatisationzino.application.usecases.DecouvrirNouveauxFichiersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecouverteFichiersTasklet implements Tasklet {

    private final DecouvrirNouveauxFichiersUseCase decouvrirNouveauxFichiersUseCase;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Exécution de la tâche de découverte des fichiers");

        try {
            int nbNouveauxFichiers = decouvrirNouveauxFichiersUseCase.executer();

            chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("nbNouveauxFichiers", nbNouveauxFichiers);

            log.info("Découverte terminée. {} nouveaux fichiers trouvés.", nbNouveauxFichiers);

        } catch (Exception e) {
            log.error("Erreur lors de la découverte des fichiers: {}", e.getMessage(), e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }
}

