package com.elpandor.hlh.modules.automatisationzino.application.mapper;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.FichierSourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FichierSourceMapper {

    FichierSource toDomain(FichierSourceEntity entity);

    FichierSourceEntity toEntity(FichierSource domain);
}
