package com.elpandor.hlh.modules.parametrage.menus.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.menus.mapper.SousMenuMapper;
import com.elpandor.hlh.modules.parametrage.menus.model.SubMenu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import com.elpandor.hlh.modules.parametrage.menus.repository.SousMenuRepository;
import com.elpandor.hlh.modules.parametrage.menus.service.SousMenuService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SousMenuServiceImpl extends GenericServiceImpl<SubMenu, Integer, SubMenuDto> implements SousMenuService {

    private final SousMenuRepository sousMenuRepository;

    public SousMenuServiceImpl(JpaRepository<SubMenu, Integer> repository, SousMenuRepository sousMenuRepository) {
        super(repository);
        this.sousMenuRepository = sousMenuRepository;
    }

    @Override
    public SubMenu transformDTOToEntity(SubMenuDto element) {
        SubMenu subMenu = SousMenuMapper.INSTANCE.toEntity(element);
        subMenu.setIsdelete(false);
        return subMenu;
    }

    @Override
    public SubMenuDto transformEntityToDTO(SubMenu element) {
        return SousMenuMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<SubMenuDto> findAllByMenu(Integer menuId) {
        return sousMenuRepository.findAllByParent_IdOrderByPosition(menuId).stream().map(this::transformEntityToDTO).toList();
    }
}
