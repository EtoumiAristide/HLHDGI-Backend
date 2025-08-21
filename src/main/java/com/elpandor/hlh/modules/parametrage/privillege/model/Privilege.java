package com.elpandor.hlh.modules.parametrage.privillege.model;

import com.elpandor.hlh.common.entities.AuditModel;
import com.elpandor.hlh.modules.parametrage.menus.model.SubMenu;
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

@Entity
@Getter
@Setter
@Cacheable(cacheNames = "privilege")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE privileges SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "privileges")
@NoArgsConstructor
@AllArgsConstructor
public class Privilege extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

//    @Size(max = 200)
//    @Column(name = "libelle", length = 200)
//    private String libelle;

//    @Column(name = "description", length = Integer.MAX_VALUE)
//    private String description;

    @Column(name = "isdelete")
    private Boolean isdelete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roles_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sous_menu_id")
    private SubMenu subMenu;


}
