package com.elpandor.hlh.modules.impressionzino.security;

import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Chaîne de sécurité dédiée aux agents d'impression, prioritaire sur la chaîne JWT/Keycloak existante
 * ({@code WebSecurityConfig}, qui reste inchangée et gère tout le reste).
 * <p>
 * Les postes de caisse n'ont pas de compte Keycloak : ils s'authentifient par clé d'API par poste, révocable.
 * L'authentification est faite par {@link PrintAgentAuthFilter} (401 si la clé est refusée) ; la règle
 * {@code permitAll} ci-dessous signifie seulement « aucune autre exigence Spring Security sur ce chemin ».
 */
@Configuration
public class PrintAgentSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain printAgentSecurityFilterChain(HttpSecurity http,
                                                             PrintAgentJpaRepository agents) throws Exception {
        http
                .securityMatcher("/api/v1/print-agent/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(new PrintAgentAuthFilter(agents), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
