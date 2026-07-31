package com.elpandor.hlh.modules.automatisationzino.batch.reader;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FichierSourceReader implements ItemReader<FichierSource> {

    private final FichierSourceRepository fichierSourceRepository;

    @Value("${batch.max-tentatives:3}")
    private int maxTentatives;

    private Iterator<FichierSource> iterator;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("Préparation du reader avec maxTentatives: {}", maxTentatives);

        List<FichierSource> fichiersATraiter = new ArrayList<>();

        // Fichiers en attente
        fichiersATraiter.addAll(fichierSourceRepository.findByStatut("PENDING"));

        // Fichiers en erreur avec tentative < max
        List<FichierSource> fichiersEnErreur = fichierSourceRepository.findByStatut("ERROR");
        fichiersEnErreur.stream()
                .filter(f -> f.getTentativeEnvoi() < maxTentatives)
                .forEach(fichiersATraiter::add);

        log.info("{} fichiers à traiter trouvés", fichiersATraiter.size());
        iterator = fichiersATraiter.iterator();
    }

    @Override
    public FichierSource read() {
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}

