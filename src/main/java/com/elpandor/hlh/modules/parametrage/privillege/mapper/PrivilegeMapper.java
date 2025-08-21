package com.elpandor.hlh.modules.parametrage.privillege.mapper;

import com.elpandor.hlh.modules.parametrage.privillege.model.Privilege;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PrivilegeMapper {
    PrivilegeMapper INSTANCE = Mappers.getMapper(PrivilegeMapper.class);

    Privilege toEntity(PrivilegeDto privilegeDto);

    PrivilegeDto toDto(Privilege privilege);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Privilege partialUpdate(PrivilegeDto privilegeDto, @MappingTarget Privilege privilege);
}
