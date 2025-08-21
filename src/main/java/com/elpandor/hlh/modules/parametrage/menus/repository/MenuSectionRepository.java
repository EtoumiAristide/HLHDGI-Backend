package com.elpandor.hlh.modules.parametrage.menus.repository;

import com.elpandor.hlh.modules.parametrage.menus.model.MenuSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuSectionRepository extends JpaRepository<MenuSection, Integer> {
    MenuSection findByLabel(String tittle);
}
