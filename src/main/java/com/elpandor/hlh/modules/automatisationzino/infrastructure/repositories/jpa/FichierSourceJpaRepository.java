package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.FichierSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FichierSourceJpaRepository extends JpaRepository<FichierSourceEntity, Long> {

    List<FichierSourceEntity> findByStatut(String statut);

    Optional<FichierSourceEntity> findByNomFichier(String nomFichier);

    boolean existsByNomFichier(String nomFichier);

    @Modifying
    @Transactional
    @Query("UPDATE FichierSourceEntity f SET f.statut = :statut, f.dernierMessageErreur = :messageErreur WHERE f.id = :id")
    void updateStatut(@Param("id") Long id,
                      @Param("statut") String statut,
                      @Param("messageErreur") String messageErreur);

    @Modifying
    @Transactional
    @Query("UPDATE FichierSourceEntity f SET f.tentativeEnvoi = f.tentativeEnvoi + 1 WHERE f.id = :id")
    void incrementerTentative(@Param("id") Long id);


    @Modifying
    @Transactional
    @Query("UPDATE FichierSourceEntity f SET f.donneesExtraitesJson = :jsonData, f.extractionEffectuee = true WHERE f.id = :id")
    void sauvegarderDonneesExtraites(@Param("id") Long id,
                                     @Param("jsonData") String jsonData);
}