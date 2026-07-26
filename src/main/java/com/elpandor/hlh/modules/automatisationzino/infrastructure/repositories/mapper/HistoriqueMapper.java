package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper;

import com.elpandor.hlh.modules.automatisationzino.domain.model.HistoriqueEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.model.StatutEnvoi;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.HistoriqueEnvoiEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HistoriqueMapper {

    @Mapping(target = "statut", source = "statut", qualifiedByName = "stringToStatut")
    HistoriqueEnvoi toDomain(HistoriqueEnvoiEntity entity);

    @Mapping(target = "statut", source = "statut", qualifiedByName = "statutToString")
    HistoriqueEnvoiEntity toEntity(HistoriqueEnvoi domain);

    @Named("stringToStatut")
    default StatutEnvoi stringToStatut(String statut) {
        return statut != null ? StatutEnvoi.valueOf(statut) : null;
    }

    @Named("statutToString")
    default String statutToString(StatutEnvoi statut) {
        return statut != null ? statut.name() : null;
    }
}