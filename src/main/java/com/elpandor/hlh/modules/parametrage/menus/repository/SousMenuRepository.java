package com.elpandor.hlh.modules.parametrage.menus.repository;

import com.elpandor.hlh.modules.parametrage.menus.model.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SousMenuRepository extends JpaRepository<SubMenu, Integer> {
    List<SubMenu> findAllByParent_IdOrderByPosition(Integer menuId);
}
