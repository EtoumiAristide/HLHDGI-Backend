package com.elpandor.hlh.modules.automatisationzino.domain.repository;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;

import java.util.List;
import java.util.Optional;

public interface FichierSourceRepository {
    List<FichierSource> findAll();
    List<FichierSource> findByStatut(String statut);
    Optional<FichierSource> findById(Long id);
    Optional<FichierSource> findByNomFichier(String nomFichier);
    boolean existsByNomFichier(String nomFichier);
    FichierSource save(FichierSource fichierSource);
    void updateStatut(Long id, String statut, String messageErreur);
    void incrementerTentative(Long id);

    void sauvegarderDonneesExtraites(Long id, String jsonData);
}