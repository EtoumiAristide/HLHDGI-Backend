package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

@Data
public class FactureAvoirPayload {
    private String id;
    private String designation;
    private Integer quantite;
}
