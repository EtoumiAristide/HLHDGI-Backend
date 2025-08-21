package com.elpandor.hlh.modules.parametrage.menus.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.menus.model.Menu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuDto;

import java.util.List;

public interface MenuService extends GenericService<Menu, Integer, MenuDto> {
    List<MenuDto> findAllByMenuSection(Integer menuSectionId);

    MenuDto findByMenuValue(String menuValue);
}
