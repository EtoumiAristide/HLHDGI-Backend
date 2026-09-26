package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.TicketVenteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketVenteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateImport", ignore = true)
    @Mapping(target = "nomFichierSource", ignore = true)
    @Mapping(target = "numTicket", source = "numTicket", qualifiedByName = "integerToString")
    @Mapping(target = "dateVente", source = "date", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "montantHT", source = "montantHT", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "tva", source = "tva", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "montantTTC", source = "montantTTC", qualifiedByName = "doubleToBigDecimal")
    TicketVenteEntity toEntity(TicketVenteZino domain);

    @Mapping(target = "numTicket", source = "numTicket", qualifiedByName = "stringToInteger")
    @Mapping(target = "date", source = "dateVente", qualifiedByName = "localDateToDate")
    @Mapping(target = "montantHT", source = "montantHT", qualifiedByName = "bigDecimalToDouble")
    @Mapping(target = "tva", source = "tva", qualifiedByName = "bigDecimalToDouble")
    @Mapping(target = "montantTTC", source = "montantTTC", qualifiedByName = "bigDecimalToDouble")
    TicketVenteZino toDomain(TicketVenteEntity entity);

    @Named("integerToString")
    default String integerToString(Integer value) {
        return value != null ? String.valueOf(value) : null;
    }

    @Named("stringToInteger")
    default Integer stringToInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
