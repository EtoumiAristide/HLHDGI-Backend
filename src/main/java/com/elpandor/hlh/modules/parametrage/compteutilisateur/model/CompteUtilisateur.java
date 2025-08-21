package com.elpandor.hlh.modules.parametrage.compteutilisateur.model;

import com.elpandor.hlh.common.entities.AuditModel;
import com.elpandor.hlh.modules.parametrage.menus.model.MenuSection;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
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
@Cacheable(cacheNames = "comptes-utilisateur")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE comptes_utilisateur SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "comptes_utilisateur")
@NoArgsConstructor
@AllArgsConstructor
public class CompteUtilisateur extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Size(max = 200)
    @Column(name = "login", length = 200)
    private String login;

    @Column(name = "email", length = Integer.MAX_VALUE)
    private String email;

    @Column(name = "isdelete")
    private Boolean isdelete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Column(name = "is_active")
    private boolean enable;

}
