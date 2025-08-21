package com.elpandor.hlh.modules.parametrage.compteutilisateur.mapper;

import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompteUtilisateurMapper {
    CompteUtilisateurMapper INSTANCE = Mappers.getMapper(CompteUtilisateurMapper.class);

    CompteUtilisateur toEntity(CompteUtilisateurDto compteUtilisateurDto);

    CompteUtilisateurDto toDto(CompteUtilisateur compteUtilisateur);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CompteUtilisateur partialUpdate(CompteUtilisateurDto compteUtilisateurDto, @MappingTarget CompteUtilisateur compteUtilisateur);
}
