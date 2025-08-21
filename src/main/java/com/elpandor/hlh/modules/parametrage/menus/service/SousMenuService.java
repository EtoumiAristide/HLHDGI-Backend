package com.elpandor.hlh.modules.parametrage.menus.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.menus.model.SubMenu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;

import java.util.List;

public interface SousMenuService extends GenericService<SubMenu, Integer, SubMenuDto> {
    List<SubMenuDto> findAllByMenu(Integer menuId);
}
