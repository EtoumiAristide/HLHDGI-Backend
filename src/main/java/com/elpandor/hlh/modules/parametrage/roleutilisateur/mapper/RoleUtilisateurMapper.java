package com.elpandor.hlh.modules.parametrage.roleutilisateur.mapper;

import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.RoleUtilisateur;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto.RoleUtilisateurDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleUtilisateurMapper {
    RoleUtilisateurMapper INSTANCE = Mappers.getMapper(RoleUtilisateurMapper.class);

    RoleUtilisateur toEntity(RoleUtilisateurDto directionMissionDto);

    RoleUtilisateurDto toDto(RoleUtilisateur directionMission);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RoleUtilisateur partialUpdate(RoleUtilisateurDto directionMissionDto, @MappingTarget RoleUtilisateur directionMission);
}
