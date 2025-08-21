package com.elpandor.hlh.modules.parametrage.organisations.mapper;

import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganisationMapper {
    OrganisationMapper INSTANCE = Mappers.getMapper(OrganisationMapper.class);

    Organisation toEntity(OrganisationDto organisationDto);

    OrganisationDto toDto(Organisation organisation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Organisation partialUpdate(OrganisationDto organisationDto, @MappingTarget Organisation organisation);
}
