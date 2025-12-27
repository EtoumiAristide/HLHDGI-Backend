package com.elpandor.hlh.modules.hlh.service.impl;

import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Service
public class PAGIMApimServiceImpl implements ApimService {

    @Value("${pagim.auth.user}")
    private String username;

    @Value("${pagim.auth.password}")
    private String password;

    @Value("${auth.api.url}")
    private String tokenUrl;

    @Value("${pagim.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public PAGIMApimServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public TokenResponse auth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        System.out.println(username);
        System.out.println(password);

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TokenResponse> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, TokenResponse.class);


        return response.getBody();
    }

    @Override
    public ResponseEntity<String> sendData(String accessToken, FacturePayload facturePayload) {
        try {

            System.out.println("data send "+facturePayload);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<FacturePayload> request = new HttpEntity<>(facturePayload, headers);

            System.out.println("Api Url : " + apiUrl);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            log.info("Réponse API : {}", response.getBody());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.error("Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne : " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> sendData(String accessToken, JsonObject facturePayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
//            System.out.println("facturePayload " + facturePayload);
            HttpEntity<String> request = new HttpEntity<>(facturePayload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            log.info("Réponse API : {}", response.getBody());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.error("Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne : " + e.getMessage());
        }
    }
}
