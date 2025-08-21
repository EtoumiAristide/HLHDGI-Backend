package com.elpandor.hlh.modules.parametrage.menus.model;

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
@Cacheable(cacheNames = "sous_menu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE sous_menu SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "sous_menu")
@NoArgsConstructor
@AllArgsConstructor
public class SubMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String label;
    private String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Menu parent;

    //Represente le positionnement du menu dans l'interface
    private Integer position;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
