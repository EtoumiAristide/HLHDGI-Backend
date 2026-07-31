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
@Table(name = "repartition_paiements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionPaiementEntity extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_repartition")
    private LocalDate dateRepartition;

    @Column(name = "mode_paiement", length = 100)
    private String modePaiement;

    @Column(name = "total_montant_ht", precision = 15, scale = 2)
    private BigDecimal totalMontantHT;

    @Column(name = "total_tva", precision = 15, scale = 2)
    private BigDecimal totalTVA;

    @Column(name = "total_montant_ttc", precision = 15, scale = 2)
    private BigDecimal totalMontantTTC;

    @Column(name = "nombre_transactions")
    private Integer nombreTransactions;

    @Column(name = "designation_principale", length = 500)
    private String designationPrincipale;

    @Column(name = "date_import")
    private LocalDateTime dateImport;

    @PrePersist
    protected void onCreate() {
        if (dateImport == null) {
            dateImport = LocalDateTime.now();
        }
    }
}
