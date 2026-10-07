package com.elpandor.hlh.modules.impressionzino.security;

import com.elpandor.hlh.modules.impressionzino.application.PrintAgentAdminService;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentEntity;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Authentifie un agent d'impression par sa clé d'API (en-tête {@code X-Agent-Key}).
 * <p>
 * Volontairement PAS un bean Spring : un {@code Filter} déclaré comme bean serait aussi enregistré par Spring Boot
 * dans la chaîne servlet globale. Il est instancié par {@link PrintAgentSecurityConfig} et ne s'applique
 * qu'à {@code /api/v1/print-agent/**}.
 * <p>
 * Le code établissement de l'agent est lu en base : il n'est jamais fourni par le poste, un agent ne peut donc pas
 * se faire passer pour un autre point de vente.
 */
public class PrintAgentAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Agent-Key";
    public static final String ATTR_AGENT = PrintAgentAuthFilter.class.getName() + ".AGENT";

    private final PrintAgentJpaRepository agents;

    public PrintAgentAuthFilter(PrintAgentJpaRepository agents) {
        this.agents = agents;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cle = request.getHeader(HEADER);
        Optional<PrintAgentEntity> agent = (cle == null || cle.isBlank())
                ? Optional.empty()
                : agents.findByApiKeyHashAndActifTrue(PrintAgentAdminService.hash(cle.trim()));

        if (agent.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Clé d'agent absente, invalide ou révoquée.\"}");
            return;
        }

        request.setAttribute(ATTR_AGENT, agent.get());
        chain.doFilter(request, response);
    }
}
