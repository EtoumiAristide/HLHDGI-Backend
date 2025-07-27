package com.elpandor.hlh.modules.factures.service;

import com.elpandor.hlh.modules.factures.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.factures.model.dto.payload.TokenResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ApimService {
    public TokenResponse auth();

    public ResponseEntity<String> sendData(String accessToken, FacturePayload facturePayload);
}
