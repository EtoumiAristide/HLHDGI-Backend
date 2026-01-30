package com.elpandor.hlh.modules.hlh.mapper;

import com.elpandor.hlh.modules.hlh.model.FactureLoad;
import com.elpandor.hlh.modules.hlh.model.dto.FactureLoadDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FactureLoadMapper {
//    FactureMapper INSTANCE = Mappers.getMapper(FactureMapper.class);

    FactureLoad toEntity(FactureLoadDto factureDto);

    FactureLoadDto toDto(FactureLoad facture);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FactureLoad partialUpdate(FactureLoadDto factureDto, @MappingTarget FactureLoad facture);
}
