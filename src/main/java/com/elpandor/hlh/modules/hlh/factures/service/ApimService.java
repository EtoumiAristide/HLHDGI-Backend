package com.elpandor.hlh.modules.hlh.factures.service;

import com.elpandor.hlh.modules.hlh.factures.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.hlh.factures.model.dto.payload.TokenResponse;
import org.springframework.http.ResponseEntity;

public interface ApimService {
    public TokenResponse auth();

    public ResponseEntity<String> sendData(String accessToken, FacturePayload facturePayload);
}
