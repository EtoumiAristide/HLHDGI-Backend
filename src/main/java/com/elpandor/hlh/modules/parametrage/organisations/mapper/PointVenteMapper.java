package com.elpandor.hlh.modules.parametrage.organisations.mapper;

import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PointVenteMapper {
    PointVenteMapper INSTANCE = Mappers.getMapper(PointVenteMapper.class);

    PointVente toEntity(PointVenteDto pointVenteDto);

    PointVenteDto toDto(PointVente pointVente);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    PointVente partialUpdate(PointVenteDto pointVenteDto, @MappingTarget PointVente pointVente);
}
