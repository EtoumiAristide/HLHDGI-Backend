package com.elpandor.hlh.modules.parametrage.sendmail.mapper;

import com.elpandor.hlh.modules.parametrage.sendmail.model.SendMail;
import com.elpandor.hlh.modules.parametrage.sendmail.model.dto.SendMailDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SendMailMapper {
    SendMailMapper INSTANCE = Mappers.getMapper(SendMailMapper.class);

    SendMailDTO toDto(SendMail sendMail);

    SendMail toEntity(SendMailDTO sendMailDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SendMail partialUpdate(SendMailDTO sendMailDTO, @MappingTarget SendMail sendMail);
}
