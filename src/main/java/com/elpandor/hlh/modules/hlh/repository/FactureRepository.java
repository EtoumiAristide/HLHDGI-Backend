package com.elpandor.hlh.modules.hlh.repository;

import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.stats.model.DashboardMonthly;
import com.elpandor.hlh.modules.stats.model.DashboardStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface FactureRepository extends JpaRepository<Facture, Integer> {
    Page<Facture> findByPointVente_Etablissement_Organisation_RaisonSocialOrderByIdDesc(Pageable pageable, String entreprise);

    Facture findByReponseFNEContainingIgnoreCase(String numFacture);

    @Query("select f from Facture f where f.dataSend is not null and f.dateCreation >= :from and f.dateCreation < :to")
    List<Facture> findBkFacturesByDateCreationBetween(@Param("from") Instant from, @Param("to") Instant to);

    // Rechercher les factures par dateFacture (date du ticket) pour filtrer exactement la période demandée
    List<Facture> findByDateFactureBetween(LocalDate from, LocalDate to);

    @Query(value = """
                SELECT
                    COALESCE(SUM(
                        CASE 
                            WHEN f.type_facture = 0
                            THEN COALESCE(
                                NULLIF((f.reponse_fne::jsonb) -> 'invoice' ->> 'totalDue', ''),
                                '0'
                            )::numeric
                        END
                    ), 0) AS totalSale,
            
                    COALESCE(SUM(
                        CASE 
                            WHEN f.type_facture = 1
                            THEN COALESCE(
                                NULLIF((f.reponse_fne::jsonb) -> 'invoice' ->> 'totalDue', ''),
                                '0'
                            )::numeric
                        END
                    ), 0) AS totalPurchase,
            
                    COALESCE(SUM(
                        CASE 
                            WHEN f.type_facture = 2
                            THEN COALESCE(
                                NULLIF((f.reponse_fne::jsonb) -> 'invoice' ->> 'totalDue', ''),
                                '0'
                            )::numeric
                        END
                    ), 0) AS totalAvoir
            
                FROM factures_hlh f
                INNER JOIN point_ventes pv ON f.point_vente_id = pv.id AND pv.isdelete = false
                INNER JOIN etablissements e ON pv.etablissement_id = e.id AND e.isdelete = false
                INNER JOIN organisations o ON e.organisation_id = o.id
                WHERE f.date_creation >= CAST(CONCAT(:annee, '-01-01') AS DATE)
                  AND f.date_creation < CAST(CONCAT(:annee + 1, '-01-01') AS DATE)
                  AND f.isdelete = false
                  -- Filtre organisation (0 = pas de filtre)
                  AND (:orgId = 0 OR o.id = :orgId)
                  -- Filtre établissement (0 = pas de filtre, mais dépend de orgId)
                  AND (
                      :orgId = 0 
                      OR :etabId = 0 
                      OR (:orgId != 0 AND :etabId != 0 AND e.id = :etabId)
                  )
                  -- Filtre point de vente (0 = pas de filtre, mais dépend de etabId et orgId)
                  AND (
                      :orgId = 0 OR :etabId = 0 OR :pdvId = 0
                      OR (:orgId != 0 AND :etabId != 0 AND :pdvId != 0 AND pv.id = :pdvId)
                  )
                  -- Filtre client (vide = pas de filtre)
                  AND (
                      :client IS NULL OR :client = '' OR :client = ' '
                      OR (f.reponse_fne IS NOT NULL 
                          AND (f.reponse_fne::jsonb) -> 'invoice' ->> 'clientCompanyName' ILIKE '%' || TRIM(:client) || '%')
                  )
            """, nativeQuery = true)
    DashboardStats getDashboardStats(
            @Param("annee") int annee,
            @Param("orgId") long orgId,      // 0 = pas de filtre
            @Param("etabId") long etabId,    // 0 = pas de filtre
            @Param("pdvId") long pdvId,      // 0 = pas de filtre
            @Param("client") String client   // null/empty = pas de filtre
    );

    @Query(value = """
                WITH months AS (
                    SELECT generate_series(1, 12) AS month
                ),
                factures_filtrees AS (
                    SELECT 
                        EXTRACT(MONTH FROM f.date_creation) AS month_num,
                        f.type_facture,
                        COALESCE(
                            NULLIF((f.reponse_fne::jsonb) -> 'invoice' ->> 'totalDue', ''),
                            '0'
                        )::numeric AS total_due
                    FROM factures_hlh f
                    INNER JOIN point_ventes pv ON f.point_vente_id = pv.id AND pv.isdelete = false
                    INNER JOIN etablissements e ON pv.etablissement_id = e.id AND e.isdelete = false
                    INNER JOIN organisations o ON e.organisation_id = o.id
                    WHERE f.date_creation >= CAST(CONCAT(:annee, '-01-01') AS DATE)
                      AND f.date_creation < CAST(CONCAT(:annee + 1, '-01-01') AS DATE)
                      AND f.isdelete = false
                      AND (:orgId = 0 OR o.id = :orgId)
                      AND (:etabId = 0 OR e.id = :etabId)
                      AND (:pdvId = 0 OR pv.id = :pdvId)
                      AND (
                          :client IS NULL OR :client = '' OR :client = ' '
                          OR (f.reponse_fne IS NOT NULL 
                              AND (f.reponse_fne::jsonb) -> 'invoice' ->> 'clientCompanyName' 
                              ILIKE '%' || TRIM(:client) || '%')
                      )
                )
                SELECT
                    m.month,
                    COALESCE(SUM(
                        CASE 
                            WHEN ff.type_facture = 0
                            THEN ff.total_due
                        END
                    ), 0) AS sale,
                    COALESCE(SUM(
                        CASE 
                            WHEN ff.type_facture = 1
                            THEN ff.total_due
                        END
                    ), 0) AS purchase,
                    COALESCE(SUM(
                        CASE 
                            WHEN ff.type_facture = 2
                            THEN ff.total_due
                        END
                    ), 0) AS avoir
                FROM months m
                LEFT JOIN factures_filtrees ff ON m.month = ff.month_num
                GROUP BY m.month
                ORDER BY m.month
            """, nativeQuery = true)
    List<DashboardMonthly> getMonthlyStats(
            @Param("annee") int annee,
            @Param("orgId") long orgId,
            @Param("etabId") long etabId,
            @Param("pdvId") long pdvId,
            @Param("client") String client
    );
}
