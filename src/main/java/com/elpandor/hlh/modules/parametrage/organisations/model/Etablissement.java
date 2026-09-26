package com.elpandor.hlh.modules.parametrage.organisations.model;

import com.elpandor.hlh.common.entities.AuditModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.cache.annotation.Cacheable;

@Getter
@Setter
@Entity
@Cacheable(cacheNames = "etablissements")
@Cache(usage= CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE etablissements SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "etablissements")
public class Etablissement extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 150)
    @Column(name = "nom", length = 150)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
