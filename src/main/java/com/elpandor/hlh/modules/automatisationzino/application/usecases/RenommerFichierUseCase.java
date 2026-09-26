package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.domain.exception.RenommageException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.ApiTelechargementClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RenommerFichierUseCase {

    private final ApiTelechargementClient apiTelechargementClient;

    /**
     * Demande le renommage, côté API Zino, d'un fichier déjà traité avec succès
     * (appel à automatisation/{filename}/rename), afin qu'il ne soit plus proposé
     * par list-files lors des prochaines découvertes.
     */
    public void executer(String nomFichier) throws RenommageException {
        log.info("Renommage du fichier: {}", nomFichier);

        try {
            apiTelechargementClient.renommerFichier(nomFichier);
            log.info("Fichier renommé avec succès: {}", nomFichier);

        } catch (Exception e) {
            log.error("Erreur lors du renommage du fichier {}: {}", nomFichier, e.getMessage());
            throw new RenommageException("Erreur de renommage: " + e.getMessage(), e);
        }
    }
}