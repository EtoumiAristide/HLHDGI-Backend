package com.elpandor.hlh.modules.impressionzino.infrastructure;

import com.elpandor.hlh.modules.impressionzino.domain.PrintJobStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PrintJobJpaRepository extends JpaRepository<PrintJobEntity, UUID> {

    /**
     * Verrouille (et renvoie) les jobs livrables d'un établissement. {@code SKIP LOCKED} permet à plusieurs
     * agents (ou plusieurs instances du backend) de se partager la file sans jamais réserver deux fois le même job.
     * À appeler dans une transaction.
     */
    @Query(value = """
            SELECT * FROM print_jobs
            WHERE code_etablissement = :code
              AND statut = 'PENDING'
              AND available_at <= :now
            ORDER BY date_creation
            LIMIT :max
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PrintJobEntity> verrouillerLivrables(@Param("code") String code,
                                              @Param("now") LocalDateTime now,
                                              @Param("max") int max);

    boolean existsByNumeroFacture(String numeroFacture);

    @Query("select distinct j.codeEtablissement from PrintJobEntity j "
            + "where j.statut = :statut and j.availableAt <= :now")
    List<String> codesAvecJobsLivrables(@Param("statut") PrintJobStatut statut,
                                        @Param("now") LocalDateTime now);

    /** Baux expirés avec tentatives restantes : le job repart en file. */
    @Modifying
    @Query("update PrintJobEntity j set j.statut = :pending, j.agentId = null, j.leaseUntil = null, "
            + "j.availableAt = :now, j.dateDerniereModification = :now, j.dernierMessageErreur = :message "
            + "where j.statut = :dispatched and j.leaseUntil < :now and j.tentatives < :max")
    int remettreEnFileBauxExpires(@Param("pending") PrintJobStatut pending,
                                  @Param("dispatched") PrintJobStatut dispatched,
                                  @Param("now") LocalDateTime now,
                                  @Param("max") int max,
                                  @Param("message") String message);

    /** Baux expirés sans tentative restante : échec définitif. */
    @Modifying
    @Query("update PrintJobEntity j set j.statut = :failed, j.leaseUntil = null, "
            + "j.dateDerniereModification = :now, j.dernierMessageErreur = :message "
            + "where j.statut = :dispatched and j.leaseUntil < :now and j.tentatives >= :max")
    int echouerBauxExpires(@Param("failed") PrintJobStatut failed,
                           @Param("dispatched") PrintJobStatut dispatched,
                           @Param("now") LocalDateTime now,
                           @Param("max") int max,
                           @Param("message") String message);

    Page<PrintJobEntity> findByCodeEtablissementAndStatut(String codeEtablissement, PrintJobStatut statut, Pageable pageable);

    Page<PrintJobEntity> findByCodeEtablissement(String codeEtablissement, Pageable pageable);

    Page<PrintJobEntity> findByStatut(PrintJobStatut statut, Pageable pageable);
}
