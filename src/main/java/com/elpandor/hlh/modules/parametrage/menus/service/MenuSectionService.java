package com.elpandor.hlh.modules.parametrage.menus.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.menus.model.MenuSection;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;

public interface MenuSectionService extends GenericService<MenuSection, Integer, MenuSectionDto> {
    MenuSectionDto findByTittle(String tittle);
}
