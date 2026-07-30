package com.elpandor.hlh.modules.hlh.service.impl;

import com.elpandor.hlh.modules.automatisation.model.zino.dto.payload.ListeFilesDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ZinoApimServiceImpl implements ApimService {

    @Value("${zino.auth.user}")
    private String username;

    @Value("${zino.auth.password}")
    private String password;

    @Value("${auth.api.url}")
    private String tokenUrl;

    @Value("${zino.api.url}")
    private String apiUrl;


    @Value("${zino.automatisation-liste-file.api.url}")
    private String automatisationListeFileApiUrl;

    @Value("${zino.automatisation-download-file.api.url}")
    private String automatisationDownloadFileApiUrl;

    private final RestTemplate restTemplate;

    public ZinoApimServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public TokenResponse auth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        //System.out.println("username: " + username);
        //System.out.println("password: " + password);
        //System.out.println("body " + body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TokenResponse> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, TokenResponse.class);

        return response.getBody();
    }

    @Override
    public ResponseEntity<String> sendData(String accessToken, FacturePayload facturePayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<FacturePayload> request = new HttpEntity<>(facturePayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            log.info("Réponse API : {}", response.getBody());
            return response;

        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            log.error("sendData - Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            e.printStackTrace();
            log.error("sendData - Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (Exception e) {
            e.printStackTrace();
            log.error("sendData - Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
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

            HttpEntity<String> request = new HttpEntity<>(facturePayload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            log.info("sendData - Réponse API : {}", response.getBody());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("sendData - Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.error("sendData - Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("sendData - Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne : " + e.getMessage());
        }
    }

    public ResponseEntity<List<ListeFilesDto>> listeFiles() {
        try {

            ResponseEntity<List<ListeFilesDto>> response = ResponseEntity.ok(null);
            //Authentification
            TokenResponse tokenResponse = null;
            tokenResponse = auth();

            if (tokenResponse != null) {

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tokenResponse.getAccessToken());
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<List<ListeFilesDto>> request = new HttpEntity<>(headers);

                response = restTemplate.exchange(automatisationListeFileApiUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<ListeFilesDto>>() {});

                log.info("Réponse API Liste fichiers : {}", response.getBody());
            }
            return response;


        } catch (HttpClientErrorException e) {
            log.error("sendData - Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
//                    .body(e.getResponseBodyAsString());
                    .body(null);

        } catch (HttpServerErrorException e) {
            log.error("sendData - Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(null);

        } catch (Exception e) {
            log.error("sendData - Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Erreur interne : " + e.getMessage());
                    .body(null);
        }
    }

    public ResponseEntity<Resource> downloadFile(String filename) {
        try {

            ResponseEntity<Resource> response = ResponseEntity.ok(null);
            //Authentification
            TokenResponse tokenResponse = null;
            tokenResponse = auth();

            if (tokenResponse != null) {

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tokenResponse.getAccessToken());
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                HttpEntity<Object> request = new HttpEntity<>(headers);

                response = restTemplate.exchange(automatisationDownloadFileApiUrl.replace("{filename}", filename), HttpMethod.GET, request, Resource.class);

                log.info("Réponse API Liste fichiers : {}", response.getBody());
            }

            return response;

        } catch (HttpClientErrorException e) {
            log.error("sendData - Erreur HTTP CLIENT {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
//                    .body(e.getResponseBodyAsString());
                    .body(null);

        } catch (HttpServerErrorException e) {
            log.error("sendData - Erreur HTTP SERVEUR {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(null);

        } catch (Exception e) {
            log.error("sendData - Erreur inattendue lors de l’appel API : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Erreur interne : " + e.getMessage());
                    .body(null);
        }
    }
}
