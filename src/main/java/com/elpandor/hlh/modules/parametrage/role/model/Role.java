package com.elpandor.hlh.modules.parametrage.role.model;

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

@Entity
@Getter
@Setter
@Cacheable(cacheNames = "role")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE roles SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
public class Role extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 200)
    @Column(name = "libelle", length = 200)
    private String libelle;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
