package com.elpandor.hlh.modules.automatisationzino.infrastructure.clients;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.dto.FichierDisponibleDTO;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTelechargementClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ===== Authentification ZINO (pour générer le token) =====
    @Value("${zino.auth.user}")
    private String username;

    @Value("${zino.auth.password}")
    private String password;

    @Value("${auth.api.url}")
    private String tokenUrl;

    // ===== URL de l'API de téléchargement =====
    @Value("${integration.api.telechargement.url}")
    private String baseUrl;

    // ===== Endpoints =====
    private static final String LIST_FILES_ENDPOINT = "/zino/api/v1/automatisation/list-files";
    private static final String DOWNLOAD_ENDPOINT = "/zino/api/v1/automatisation/";

    // ===== Gestion du token en cache =====
    private String cachedToken;
    private LocalDateTime tokenExpiry;


     //Récupère un token d'authentification depuis l'API Manager (avec cache)

    private synchronized String getToken() {
        // Vérifier si le token est encore valide (5 min de marge)
        if (cachedToken != null && tokenExpiry != null &&
                LocalDateTime.now().isBefore(tokenExpiry)) {
            log.debug("Token encore valide (expire dans {} min)",
                    java.time.Duration.between(LocalDateTime.now(), tokenExpiry).toMinutes());
            return cachedToken;
        }

        log.info("Génération d'un nouveau token...");
        log.debug("   Username: {}", username);
        log.debug("   Token URL: {}", tokenUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    TokenResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                cachedToken = tokenResponse.getAccessToken();

                int expiresIn = tokenResponse.getExpiresIn() != 0 ?
                        tokenResponse.getExpiresIn() : 3600;
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300);

                log.info("Token généré avec succès (expire dans {} sec)", expiresIn);
                return cachedToken;
            } else {
                log.error("Échec de l'authentification: {}", response.getStatusCode());
                throw new RuntimeException("Échec de l'authentification");
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'authentification: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur d'authentification", e);
        }
    }


     //Récupère la liste des fichiers disponibles depuis l'API

    public List<FichierDisponibleDTO> listerFichiersDisponibles() {
        String url = baseUrl + LIST_FILES_ENDPOINT;
        log.info("Récupération de la liste des fichiers disponibles");
        log.info("   URL: {}", url);

        try {
            String token = getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<FichierDisponibleDTO> fichiers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<FichierDisponibleDTO>>() {}
                );

                log.info("{} fichiers disponibles trouvés", fichiers.size());
                fichiers.forEach(f -> log.debug("   - {} ({} octets)", f.getNomFichier(), f.getTaille()));

                return fichiers;
            } else {
                log.warn("Réponse inattendue: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la liste: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


     //Télécharge un fichier spécifique depuis l'API

    public MultipartFile telechargerFichier(String nomFichier) {
        String downloadUrl = baseUrl + DOWNLOAD_ENDPOINT + nomFichier + "/download";

        log.info("Téléchargement du fichier: {}", nomFichier);
        log.info("   URL: {}", downloadUrl);

        try {
            String token = getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] fileContent = response.getBody();
                log.info("Fichier téléchargé avec succès: {} ({} octets)",
                        nomFichier, fileContent.length);

                String contentType = determineContentType(nomFichier);

                return new InMemoryMultipartFile(
                        "file",
                        nomFichier,
                        contentType,
                        fileContent
                );
            } else {
                log.error("Échec du téléchargement: {}", response.getStatusCode());
                throw new RuntimeException("Échec du téléchargement: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement de {}: {}", nomFichier, e.getMessage(), e);
            throw new RuntimeException("Erreur de téléchargement: " + e.getMessage(), e);
        }
    }


     //Détermine le type MIME du fichier à partir de son extension

    private String determineContentType(String nomFichier) {
        if (nomFichier == null) return "application/octet-stream";

        String lowerName = nomFichier.toLowerCase();
        if (lowerName.endsWith(".csv")) {
            return "text/csv";
        } else if (lowerName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }


     //Helper class pour transformer byte[] en MultipartFile

    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}