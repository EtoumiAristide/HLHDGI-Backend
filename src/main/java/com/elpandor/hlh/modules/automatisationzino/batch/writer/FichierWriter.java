package com.elpandor.hlh.modules.automatisationzino.batch.writer;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FichierWriter implements ItemWriter<FichierSource> {

    @Override
    public void write(Chunk<? extends FichierSource> chunk) throws Exception {
        log.info("Écriture des résultats pour {} fichiers", chunk.size());

        for (FichierSource fichier : chunk) {
            log.debug("Fichier {} - Statut final: {}",
                    fichier.getNomFichier(),
                    fichier.getStatut());
        }
    }
}

