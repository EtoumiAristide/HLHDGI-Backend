package com.elpandor.hlh.modules.factures.mapper;

import com.elpandor.hlh.modules.factures.model.Facture;
import com.elpandor.hlh.modules.factures.model.dto.FactureDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FactureMapper {
//    FactureMapper INSTANCE = Mappers.getMapper(FactureMapper.class);

    Facture toEntity(FactureDto factureDto);

    FactureDto toDto(Facture facture);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Facture partialUpdate(FactureDto factureDto, @MappingTarget Facture facture);
}
