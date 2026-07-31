package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.HistoriqueEnvoiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoriqueJpaRepository extends JpaRepository<HistoriqueEnvoiEntity, Long> {

    List<HistoriqueEnvoiEntity> findByStatut(String statut);

    List<HistoriqueEnvoiEntity> findByNomFichier(String nomFichier);

    @Query("SELECT h FROM HistoriqueEnvoiEntity h WHERE h.nomFichier = :nomFichier ORDER BY h.dateEnvoi DESC LIMIT 1")
    HistoriqueEnvoiEntity findLatestByNomFichier(@Param("nomFichier") String nomFichier);

    @Query("SELECT h FROM HistoriqueEnvoiEntity h WHERE h.statut = 'ECHEC' ORDER BY h.dateEnvoi DESC LIMIT :limit")
    List<HistoriqueEnvoiEntity> findRecentFailures(@Param("limit") int limit);
}