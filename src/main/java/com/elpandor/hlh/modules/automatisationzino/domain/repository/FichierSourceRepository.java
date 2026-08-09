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

    /**
     * Compte les fichiers ayant le statut donné et modifiés après la date indiquée.
     * Utilisé pour détecter, à la fin d'un run, les fichiers passés en erreur pendant CE run.
     */
    long countByStatutDepuis(String statut, java.time.LocalDateTime depuis);

    /**
     * Fichiers créés dans l'intervalle [debut, fin], triés par date de création croissante.
     * Utilisé pour le rapport d'état des extractions.
     */
    List<FichierSource> findByPeriode(java.time.LocalDateTime debut, java.time.LocalDateTime fin);
}