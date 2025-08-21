package com.elpandor.hlh.modules.parametrage.menus.repository;

import com.elpandor.hlh.modules.parametrage.menus.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Integer> {
    List<Menu> findAllByMenuSection_IdOrderByPosition(Integer menuSectionId);

    Menu findByLabel(String menuValue);
}
