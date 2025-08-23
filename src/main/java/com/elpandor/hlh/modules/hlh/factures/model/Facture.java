package com.elpandor.hlh.modules.hlh.factures.model;


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

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Cacheable(cacheNames = "factures_hlh")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE factures_hlh SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "factures_hlh")
@NoArgsConstructor
@AllArgsConstructor
public class Facture extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    private String numFacture;
    private String reference;
    private LocalDate dateFacture;
    private String nomClient;
    private String lienFichier;
    @Column(name = "extracted_data", columnDefinition = "TEXT DEFAULT NULL")
    private String extractedData;
    @Column(name = "data_send_request", columnDefinition = "TEXT DEFAULT NULL")
    private String dataSend;
    @Column(name = "reponse_fne", columnDefinition = "TEXT DEFAULT NULL")
    private String reponseFNE;

    private TypeFacture typeFacture;
    private TypeClient typeClient;
    private ModePaiement modePaiement;

    @ManyToOne
    @JoinColumn(name = "point_vente_id")
    private PointVente pointVente;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
