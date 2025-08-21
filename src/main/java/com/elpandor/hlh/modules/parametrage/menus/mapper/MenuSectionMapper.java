package com.elpandor.hlh.modules.parametrage.menus.mapper;

import com.elpandor.hlh.modules.parametrage.menus.model.MenuSection;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface MenuSectionMapper {
    MenuSectionMapper INSTANCE = Mappers.getMapper(MenuSectionMapper.class);

    MenuSection toEntity(MenuSectionDto menuSectionDto);

    MenuSectionDto toDto(MenuSection menuSection);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    MenuSection partialUpdate(MenuSectionDto menuSectionDto, @MappingTarget MenuSection menuSection);
}
