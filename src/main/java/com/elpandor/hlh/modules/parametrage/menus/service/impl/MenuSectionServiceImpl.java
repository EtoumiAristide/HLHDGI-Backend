package com.elpandor.hlh.modules.parametrage.menus.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.menus.mapper.MenuSectionMapper;
import com.elpandor.hlh.modules.parametrage.menus.model.MenuSection;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;
import com.elpandor.hlh.modules.parametrage.menus.repository.MenuSectionRepository;
import com.elpandor.hlh.modules.parametrage.menus.service.MenuSectionService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class MenuSectionServiceImpl extends GenericServiceImpl<MenuSection, Integer, MenuSectionDto> implements MenuSectionService {
    private final MenuSectionRepository menuSectionRepository;

    public MenuSectionServiceImpl(JpaRepository<MenuSection, Integer> repository, MenuSectionRepository menuSectionRepository) {
        super(repository);
        this.menuSectionRepository = menuSectionRepository;
    }

    @Override
    public MenuSection transformDTOToEntity(MenuSectionDto element) {
        MenuSection menuSection = MenuSectionMapper.INSTANCE.toEntity(element);
        menuSection.setIsdelete(false);
        return menuSection;
    }

    @Override
    public MenuSectionDto transformEntityToDTO(MenuSection element) {
        return MenuSectionMapper.INSTANCE.toDto(element);
    }

    @Override
    public MenuSectionDto findByTittle(String tittle) {
        return this.transformEntityToDTO(menuSectionRepository.findByLabel(tittle));
    }
}
