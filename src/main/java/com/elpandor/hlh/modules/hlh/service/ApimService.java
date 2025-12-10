package com.elpandor.hlh.modules.hlh.service;

import com.elpandor.hlh.modules.hlh.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;

public interface ApimService {
    public TokenResponse auth();

    public ResponseEntity<String> sendData(String accessToken, FacturePayload facturePayload);

    public ResponseEntity<String> sendData(String accessToken, JsonObject facturePayload);
}
