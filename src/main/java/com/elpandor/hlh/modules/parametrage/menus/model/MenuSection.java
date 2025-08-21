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
@Cacheable(cacheNames = "menu_section")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(sql = "UPDATE menu_section SET isdelete = true WHERE id=?")
@SQLRestriction("isdelete = false")
@Table(name = "menu_section")
@NoArgsConstructor
@AllArgsConstructor
public class MenuSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String label;
    private boolean isTitle;

    /*@OneToMany(mappedBy = "menuSection")
    private List<Menu> menu;*/

    //Represente le positionnement du menu dans l'interface
    private Integer position;

    @Column(name = "isdelete")
    private Boolean isdelete;
}
