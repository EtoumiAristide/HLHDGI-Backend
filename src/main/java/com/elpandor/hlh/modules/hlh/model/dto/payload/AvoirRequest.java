package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

import java.util.List;

@Data
public class AvoirRequest {
    private String type;
    private String numeroFacture;
    private String messageCommercial;

    private List<FactureAvoirPayload> selectedLines;
}
