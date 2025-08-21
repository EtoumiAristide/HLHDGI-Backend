package com.elpandor.hlh.modules.parametrage.roleutilisateur.model;

import com.elpandor.hlh.common.entities.AuditModel;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import com.elpandor.hlh.modules.parametrage.role.model.Role;
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
@Cacheable(cacheNames = "roles_utilisateur")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE roles_utilisateur SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "roles_utilisateur")
@NoArgsConstructor
@AllArgsConstructor
public class RoleUtilisateur extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    private LocalDate date;

    @Column(name = "isdelete")
    private Boolean isdelete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_utilisateur_id")
    private CompteUtilisateur compteUtilisateur;

}
