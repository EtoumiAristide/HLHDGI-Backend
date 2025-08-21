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
@Cacheable(cacheNames = "organisations")
@Cache(usage= CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE organisations SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organisations")
public class Organisation extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @Column(name = "num_cc", length = 50)
    private String numcc;

    @Size(max = 200)
    @Column(name = "raison_social", length = 200)
    private String raisonSocial;

    @Size(max = 200)
    @Column(name = "logo", length = 200)
    private String logo;

    @Size(max = 50)
    @Column(name = "sigle", length = 50)
    private String sigle;

    @Column(name = "isdelete")
    private Boolean isdelete;

}