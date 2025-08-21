package com.elpandor.hlh.modules.parametrage.menus.mapper;

import com.elpandor.hlh.modules.parametrage.menus.model.SubMenu;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SousMenuMapper {
    SousMenuMapper INSTANCE = Mappers.getMapper(SousMenuMapper.class);

    SubMenu toEntity(SubMenuDto subMenuDto);

    SubMenuDto toDto(SubMenu subMenu);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SubMenu partialUpdate(SubMenuDto subMenuDto, @MappingTarget SubMenu subMenu);
}
