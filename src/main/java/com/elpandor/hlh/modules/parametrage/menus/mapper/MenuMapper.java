package com.elpandor.hlh.modules.parametrage.menus.mapper;

import com.elpandor.hlh.modules.parametrage.menus.model.Menu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface MenuMapper {
    MenuMapper INSTANCE = Mappers.getMapper(MenuMapper.class);

    Menu toEntity(MenuDto menuDto);

    MenuDto toDto(Menu menus);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Menu partialUpdate(MenuDto menuDto, @MappingTarget Menu menus);
}
