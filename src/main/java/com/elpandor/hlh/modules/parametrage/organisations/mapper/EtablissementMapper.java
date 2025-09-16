package com.elpandor.hlh.modules.parametrage.organisations.mapper;

import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.Etablissement;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EtablissementMapper {
    EtablissementMapper INSTANCE = Mappers.getMapper(EtablissementMapper.class);

    Etablissement toEntity(EtablissementDto etablissementDto);

    EtablissementDto toDto(Etablissement etablissement);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Etablissement partialUpdate(EtablissementDto etablissementDto, @MappingTarget Etablissement etablissement);
}
