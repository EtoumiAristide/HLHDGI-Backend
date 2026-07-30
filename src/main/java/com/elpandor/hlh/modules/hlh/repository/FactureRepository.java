package com.elpandor.hlh.modules.hlh.repository;

import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.stats.model.DashboardMonthly;
import com.elpandor.hlh.modules.stats.model.DashboardStats;
import com.elpandor.hlh.modules.stats.model.FactureTimbre;
import com.elpandor.hlh.modules.stats.model.FactureTimbreTotaux;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FactureRepository extends JpaRepository<Facture, Integer> {
    Page<Facture> findByPointVente_Etablissement_Organisation_RaisonSocialOrderByIdDesc(Pageable pageable, String entreprise);

    Facture findByReponseFNEContainingIgnoreCase(String numFacture);
    List<Facture> findByAutomatisationFileName(String automatisationFileName);

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
                                NULLIF((f.data_send_request::jsonb) -> 'invoice' ->> 'totalDue', ''),
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
                            CASE
                                WHEN f.type_facture = 2 THEN
                                    -- Pour les avoirs, on prend depuis data_send_request
                                    COALESCE(
                                        NULLIF((f.data_send_request::jsonb) -> 'invoice' ->> 'totalDue', ''),
                                        '0'
                                    )::numeric
                                ELSE
                                    -- Pour les ventes et achats, on prend depuis reponse_fne
                                    COALESCE(
                                        NULLIF((f.reponse_fne::jsonb) -> 'invoice' ->> 'totalDue', ''),
                                        '0'
                                    )::numeric
                            END,
                            0
                        ) AS total_due
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

    @Query(value = """
            WITH factures_sans_avoir AS (
                SELECT
                    fac.*,
                    etb.nom as nom_etablissement,
                    pv.nom as nom_point_vente,
                    fac.date_facture::date as jour_ca,
                    DATE_TRUNC('month', fac.date_facture) as mois,
                    TO_CHAR(fac.date_facture, 'MM YYYY') as nom_mois,
                    fac.nom_client as nom_client_fac,
                    (fac.reponse_fne::jsonb)->>'reference' as reference_fne
                FROM factures_hlh fac
                INNER JOIN point_ventes pv ON pv.id = fac.point_vente_id
                INNER JOIN etablissements etb ON etb.id = pv.etablissement_id
                INNER JOIN organisations org ON org.id = etb.organisation_id
                WHERE org.num_cc = :numcc
                AND fac.type_facture = 0
                AND fac.date_facture >= :dateDebut
                AND fac.date_facture < :dateFin
                AND (:pointDeVente IS NULL OR pv.nom ILIKE CONCAT('%', :pointDeVente, '%'))
                AND NOT EXISTS (
                    SELECT 1 FROM factures_hlh avoir
                    WHERE avoir.type_facture = 2
                    AND (avoir.data_send_request::jsonb)->>'reference' = (fac.reponse_fne::jsonb)->>'reference'
                )
            ),
            nb_ticket_par_facture AS (
                SELECT
                    nom_mois as mois,
                    nom_etablissement as bkName,
                    reference_fne as nFacture,
                    jour_ca as jourCa,
                    CASE
                        WHEN UPPER(fac.nom_client_fac) LIKE '%CASH%' THEN 'CASH'
                        WHEN UPPER(fac.nom_client_fac) LIKE '%GLOVO%' THEN 'HD GLOVO'
                    END as moyenDePaiement,
                    COALESCE(SUM(CASE
                        WHEN (item->>'prixUnitaireHT')::numeric >= 5000
                        THEN (item->>'quantite')::integer
                        ELSE 0
                    END), 0) as nbreTicket5000
                FROM factures_sans_avoir fac
                CROSS JOIN jsonb_array_elements((fac.data_send_request::jsonb)->'lignes') AS item
                WHERE UPPER(fac.nom_client_fac) LIKE '%CASH%'
                   OR UPPER(fac.nom_client_fac) LIKE '%GLOVO%'
                GROUP BY
                    nom_mois,
                    nom_etablissement,
                    reference_fne,
                    jour_ca,
                    fac.nom_client_fac
            )
            SELECT
                mois,
                bkName,
                nFacture,
                jourCa,
                moyenDePaiement,
                nbreTicket5000,
                100 as montantTimbre,
                (COALESCE(nbreTicket5000, 0) * 100)::numeric as total
            FROM nb_ticket_par_facture
            WHERE nbreTicket5000 > 0
            ORDER BY
                mois,
                bkName,
                moyenDePaiement,
                jourCa
            """,
            countQuery = """
                        WITH factures_sans_avoir AS (
                            SELECT
                                fac.*,
                                etb.nom as nom_etablissement,
                                pv.nom as nom_point_vente,
                                fac.date_facture::date as jour_ca,
                                DATE_TRUNC('month', fac.date_facture) as mois,
                                TO_CHAR(fac.date_facture, 'MM YYYY') as nom_mois,
                                fac.nom_client as nom_client_fac,
                                (fac.reponse_fne::jsonb)->>'reference' as reference_fne
                            FROM factures_hlh fac
                            INNER JOIN point_ventes pv ON pv.id = fac.point_vente_id
                            INNER JOIN etablissements etb ON etb.id = pv.etablissement_id
                            INNER JOIN organisations org ON org.id = etb.organisation_id
                            WHERE org.num_cc = :numcc
                            AND fac.type_facture = 0
                            AND fac.date_facture >= :dateDebut
                            AND fac.date_facture < :dateFin
                            AND (:pointDeVente IS NULL OR pv.nom ILIKE CONCAT('%', :pointDeVente, '%'))
                            AND NOT EXISTS (
                                SELECT 1 FROM factures_hlh avoir
                                WHERE avoir.type_facture = 2
                                AND (avoir.data_send_request::jsonb)->>'reference' = (fac.reponse_fne::jsonb)->>'reference'
                            )
                        ),
                        nb_ticket_par_facture AS (
                            SELECT
                                nom_mois as mois,
                                nom_etablissement as bkName,
                                reference_fne as nFacture,
                                jour_ca as jourCa,
                                CASE
                                    WHEN UPPER(fac.nom_client_fac) LIKE '%CASH%' THEN 'CASH'
                                    WHEN UPPER(fac.nom_client_fac) LIKE '%GLOVO%' THEN 'HD GLOVO'
                                END as moyenDePaiement,
                                COALESCE(SUM(CASE
                                    WHEN (item->>'prixUnitaireHT')::numeric >= 5000
                                    THEN (item->>'quantite')::integer
                                    ELSE 0
                                END), 0) as nbreTicket5000
                            FROM factures_sans_avoir fac
                            CROSS JOIN jsonb_array_elements((fac.data_send_request::jsonb)->'lignes') AS item
                            WHERE UPPER(fac.nom_client_fac) LIKE '%CASH%'
                               OR UPPER(fac.nom_client_fac) LIKE '%GLOVO%'
                            GROUP BY
                                nom_mois,
                                nom_etablissement,
                                reference_fne,
                                jour_ca,
                                fac.nom_client_fac
                        )
                        SELECT COUNT(nFacture)
                        FROM nb_ticket_par_facture
                        WHERE nbreTicket5000 > 0
                    """,
            nativeQuery = true)
    public Page<FactureTimbre> getFactureTimbrePaginate(Pageable pageable, @Param("numcc") String numcc, @Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin, @Param("pointDeVente") String pointDeVente);

    @Query(value = """
            WITH factures_sans_avoir AS (
                SELECT
                    fac.*,
                    etb.nom as nom_etablissement,
                    pv.nom as nom_point_vente,
                    fac.date_facture::date as jour_ca,
                    DATE_TRUNC('month', fac.date_facture) as mois,
                    TO_CHAR(fac.date_facture, 'MM YYYY') as nom_mois,
                    fac.nom_client as nom_client_fac,
                    (fac.reponse_fne::jsonb)->>'reference' as reference_fne
                FROM factures_hlh fac
                INNER JOIN point_ventes pv ON pv.id = fac.point_vente_id
                INNER JOIN etablissements etb ON etb.id = pv.etablissement_id
                INNER JOIN organisations org ON org.id = etb.organisation_id
                WHERE org.num_cc = :numcc
                AND fac.type_facture = 0
                AND fac.date_facture >= :dateDebut
                AND fac.date_facture < :dateFin
                AND (:pointDeVente IS NULL OR pv.nom ILIKE CONCAT('%', :pointDeVente, '%'))
                AND NOT EXISTS (
                    SELECT 1 FROM factures_hlh avoir
                    WHERE avoir.type_facture = 2
                    AND (avoir.data_send_request::jsonb)->>'reference' = (fac.reponse_fne::jsonb)->>'reference'
                )
            ),
            nb_ticket_par_facture AS (
                SELECT
                    nom_mois as mois,
                    nom_etablissement as bkName,
                    reference_fne as nFacture,
                    jour_ca as jourCa,
                    CASE
                        WHEN UPPER(fac.nom_client_fac) LIKE '%CASH%' THEN 'CASH'
                        WHEN UPPER(fac.nom_client_fac) LIKE '%GLOVO%' THEN 'HD GLOVO'
                    END as moyenDePaiement,
                    COALESCE(SUM(CASE
                        WHEN (item->>'prixUnitaireHT')::numeric >= 5000
                        THEN (item->>'quantite')::integer
                        ELSE 0
                    END), 0) as nbreTicket5000
                FROM factures_sans_avoir fac
                CROSS JOIN jsonb_array_elements((fac.data_send_request::jsonb)->'lignes') AS item
                WHERE UPPER(fac.nom_client_fac) LIKE '%CASH%'
                   OR UPPER(fac.nom_client_fac) LIKE '%GLOVO%'
                GROUP BY
                    nom_mois,
                    nom_etablissement,
                    reference_fne,
                    jour_ca,
                    fac.nom_client_fac
            )
            SELECT
                mois,
                bkName,
                nFacture,
                jourCa,
                moyenDePaiement,
                nbreTicket5000,
                100 as montantTimbre,
                (COALESCE(nbreTicket5000, 0) * 100)::numeric as total
            FROM nb_ticket_par_facture
            WHERE nbreTicket5000 > 0
            ORDER BY
                mois,
                bkName,
                moyenDePaiement,
                jourCa
            """,
            nativeQuery = true)
    public List<FactureTimbre> getFactureTimbre(@Param("numcc") String numcc, @Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin, @Param("pointDeVente") String pointDeVente);

    @Query(value = """
                WITH factures_sans_avoir AS (
                    SELECT
                        fac.*,
                        etb.nom as nom_etablissement,
                        pv.nom as nom_point_vente,
                        fac.date_facture::date as jour_ca,
                        DATE_TRUNC('month', fac.date_facture) as mois,
                        TO_CHAR(fac.date_facture, 'MM YYYY') as nom_mois,
                        fac.nom_client as nom_client_fac,
                        (fac.reponse_fne::jsonb)->>'reference' as reference_fne
                    FROM factures_hlh fac
                    INNER JOIN point_ventes pv ON pv.id = fac.point_vente_id
                    INNER JOIN etablissements etb ON etb.id = pv.etablissement_id
                    INNER JOIN organisations org ON org.id = etb.organisation_id
                    WHERE org.num_cc = :numcc
                    AND fac.type_facture = 0
                    AND fac.date_facture >= :dateDebut
                    AND fac.date_facture < :dateFin
                    AND (:pointDeVente IS NULL OR pv.nom ILIKE CONCAT('%', :pointDeVente, '%'))
                    AND NOT EXISTS (
                        SELECT 1 FROM factures_hlh avoir
                        WHERE avoir.type_facture = 2
                        AND (avoir.data_send_request::jsonb)->>'reference' = (fac.reponse_fne::jsonb)->>'reference'
                    )
                ),
                nb_ticket_par_facture AS (
                    SELECT
                        nom_mois as mois,
                        nom_etablissement as bkName,
                        nom_point_vente as pointDeVente,
                        reference_fne as nFacture,
                        jour_ca as jourCa,
                        CASE
                            WHEN UPPER(fac.nom_client_fac) LIKE '%CASH%' THEN 'CASH'
                            WHEN UPPER(fac.nom_client_fac) LIKE '%GLOVO%' THEN 'HD GLOVO'
                        END as moyenDePaiement,
                        COALESCE(SUM(CASE
                            WHEN (item->>'prixUnitaireHT')::numeric >= 5000
                            THEN (item->>'quantite')::integer
                            ELSE 0
                        END), 0) as nbreTicket5000
                    FROM factures_sans_avoir fac
                    CROSS JOIN jsonb_array_elements((fac.data_send_request::jsonb)->'lignes') AS item
                    WHERE UPPER(fac.nom_client_fac) LIKE '%CASH%'
                       OR UPPER(fac.nom_client_fac) LIKE '%GLOVO%'
                    GROUP BY
                        nom_mois,
                        nom_etablissement,
                        nom_point_vente,
                        reference_fne,
                        jour_ca,
                        fac.nom_client_fac
                )
                SELECT
                    mois,
                    pointDeVente,
                    moyenDePaiement,
                    COUNT(*) as nombreFactures,
                    SUM(nbreTicket5000) as totalTickets,
                    SUM(nbreTicket5000 * 100)::numeric as totalMontant
                FROM nb_ticket_par_facture
                WHERE nbreTicket5000 > 0
                GROUP BY mois, pointDeVente, moyenDePaiement
                ORDER BY mois, pointDeVente, moyenDePaiement
            """, nativeQuery = true)
    List<FactureTimbreTotaux> getFactureTimbreTotaux(
            @Param("numcc") String numcc,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("pointDeVente") String pointDeVente
    );
}
