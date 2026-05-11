package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientPayload {
    private String nom;
    private String numeroCC;
}
