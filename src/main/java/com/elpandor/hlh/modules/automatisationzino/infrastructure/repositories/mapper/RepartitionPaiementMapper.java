package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.RepartitionParModePaiement;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.RepartitionPaiementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RepartitionPaiementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateImport", ignore = true)
    @Mapping(target = "dateRepartition", source = "date", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "totalMontantHT", source = "totalMontantHT", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "totalTVA", source = "totalTVA", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "totalMontantTTC", source = "totalMontantTTC", qualifiedByName = "doubleToBigDecimal")
    RepartitionPaiementEntity toEntity(RepartitionParModePaiement domain);

    @Mapping(target = "date", source = "dateRepartition", qualifiedByName = "localDateToDate")
    @Mapping(target = "totalMontantHT", source = "totalMontantHT", qualifiedByName = "bigDecimalToDouble")
    @Mapping(target = "totalTVA", source = "totalTVA", qualifiedByName = "bigDecimalToDouble")
    @Mapping(target = "totalMontantTTC", source = "totalMontantTTC", qualifiedByName = "bigDecimalToDouble")
    RepartitionParModePaiement toDomain(RepartitionPaiementEntity entity);

    @Named("dateToLocalDate")
    default LocalDate dateToLocalDate(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
    }

    @Named("localDateToDate")
    default Date localDateToDate(LocalDate localDate) {
        return localDate != null ? Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
    }

    @Named("doubleToBigDecimal")
    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Named("bigDecimalToDouble")
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
