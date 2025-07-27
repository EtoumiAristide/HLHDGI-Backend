package com.elpandor.hlh.modules.factures.model;


import com.elpandor.hlh.common.entities.AuditModel;
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
@Cacheable(cacheNames = "factures")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE factures SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "factures")
@NoArgsConstructor
@AllArgsConstructor
public class Facture extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    private String numFacture;
    private LocalDate dateFacture;
    private String nomClient;
    private String lienFichier;
    @Column(name = "data_send_request", columnDefinition = "TEXT DEFAULT NULL")
    private String dataSend;
    @Column(name = "reponse_fne", columnDefinition = "TEXT DEFAULT NULL")
    private String reponseFNE;

    private TypeFacture typeFacture;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
