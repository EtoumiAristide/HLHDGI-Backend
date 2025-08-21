package com.elpandor.hlh.common.core.security;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;

@Configuration
public class KeycloakApiConfig {

    @Value("${keycloak.admin.host}")
    private String KEYCLOAK_HOST;
    @Value("${keycloak.admin.realm}")
    private String KEYCLOAK_REALM;
    @Value("${keycloak.admin.clientId}")
    private String KEYCLOAK_CLIENT_ID;
    @Value("${keycloak.admin.clientSecret}")
    private String KEYCLOAK_CLIENT_SECRET;

    @Bean
    public Keycloak getInstance() {
        return KeycloakBuilder
                .builder()
                .grantType(CLIENT_CREDENTIALS)
                .serverUrl(KEYCLOAK_HOST)
                .realm(KEYCLOAK_REALM)
                .clientId(KEYCLOAK_CLIENT_ID)
                .clientSecret(KEYCLOAK_CLIENT_SECRET)
                .build();
    }
}
