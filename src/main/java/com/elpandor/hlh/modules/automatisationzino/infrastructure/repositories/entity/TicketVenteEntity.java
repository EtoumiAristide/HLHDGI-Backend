package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity;

import com.elpandor.hlh.common.entities.AuditModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets_vente_zino")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketVenteEntity extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_ticket", length = 50)
    private String numTicket;

    @Column(name = "date_vente")
    private LocalDate dateVente;

    @Column(name = "nom_client")
    private String nomClient;

    @Column(name = "code_produit_principal", length = 100)
    private String codeProduitPrincipal;

    @Column(name = "designation_principale", length = 500)
    private String designationPrincipale;

    @Column(name = "mode_paiement", length = 100)
    private String modePaiement;

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "tva", precision = 15, scale = 2)
    private BigDecimal tva;

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "num_compte_client", length = 50)
    private String numCompteClient;

    @Column(name = "nom_fichier_source")
    private String nomFichierSource;

    @Column(name = "date_import")
    private LocalDateTime dateImport;

    @PrePersist
    protected void onCreate() {
        if (dateImport == null) {
            dateImport = LocalDateTime.now();
        }
    }
}
