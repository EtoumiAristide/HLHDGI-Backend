package com.elpandor.hlh.modules.hlh.model.dto;

import java.util.List;

public record InvoiceDto(
        String date, String paymentMethod, double totalBeforeTaxes,
        double vatAmount, double fiscalStamp, double totalCustomTaxes,
        double totalDue, String clientCompanyName, String clientNcc,
        String clientEstablishment, String clientPointOfSale,
        String clientTaxRegime, String commercialMessage,
        List<ItemDto> items
) {}
