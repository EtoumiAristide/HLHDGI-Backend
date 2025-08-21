package com.elpandor.hlh.modules.parametrage.menus.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.menus.mapper.MenuMapper;
import com.elpandor.hlh.modules.parametrage.menus.model.Menu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuDto;
import com.elpandor.hlh.modules.parametrage.menus.repository.MenuRepository;
import com.elpandor.hlh.modules.parametrage.menus.service.MenuService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends GenericServiceImpl<Menu, Integer, MenuDto> implements MenuService {

    private final MenuRepository menuRepository;

    public MenuServiceImpl(JpaRepository<Menu, Integer> repository, MenuRepository menuRepository) {
        super(repository);
        this.menuRepository = menuRepository;
    }

    @Override
    public Menu transformDTOToEntity(MenuDto element) {
        Menu menus = MenuMapper.INSTANCE.toEntity(element);
        menus.setIsdelete(false);
        return menus;
    }

    @Override
    public MenuDto transformEntityToDTO(Menu element) {
        return MenuMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<MenuDto> findAllByMenuSection(Integer menuSectionId) {
        return menuRepository.findAllByMenuSection_IdOrderByPosition(menuSectionId).stream().map(this::transformEntityToDTO).collect(Collectors.toList());
    }

    @Override
    public MenuDto findByMenuValue(String menuValue) {
        return transformEntityToDTO(menuRepository.findByLabel(menuValue));
    }
}
