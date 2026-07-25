package com.elpandor.hlh.modules.hlh.model.dto;

import java.util.List;

public record ItemDto(
        String reference, String description, double amount,
        double quantity, double discount, String measurementUnit,
        List<TaxDto> taxes
) {}
