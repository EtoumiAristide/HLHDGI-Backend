package com.elpandor.hlh.modules.hlh.model;


import com.elpandor.hlh.common.entities.AuditModel;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.cache.annotation.Cacheable;

import java.util.UUID;

@Entity
@Getter
@Setter
@Cacheable(cacheNames = "factures_load")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE factures_load SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "factures_load")
@NoArgsConstructor
@AllArgsConstructor
public class FactureLoad extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    private String lienFichier;
    @Column(name = "data_facture", columnDefinition = "TEXT DEFAULT NULL")
    private String dataFacture;

    @Column(name = "bk_extracted_data", columnDefinition = "TEXT DEFAULT NULL")
    private String bkExtractedData;

    @ManyToOne
    @JoinColumn(name = "point_vente_id")
    private PointVente pointVente;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
