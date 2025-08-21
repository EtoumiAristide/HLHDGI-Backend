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
@Cacheable(cacheNames = "menu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE menu SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "menu")
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String label;
    private String icon;
    private String link;
    private boolean isLayout;
    private boolean isTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_section_id")
    private MenuSection menuSection;

    //Represente le positionnement du menu dans l'interface
    private Integer position;

    /*@OneToMany(mappedBy = "menu")
    private List<SubMenu> subMenus;*/

    @Column(name = "isdelete")
    private Boolean isdelete;
}
