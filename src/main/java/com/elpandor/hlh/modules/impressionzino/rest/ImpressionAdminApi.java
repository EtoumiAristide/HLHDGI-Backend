package com.elpandor.hlh.modules.impressionzino.rest;

import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.impressionzino.application.PrintAgentAdminService;
import com.elpandor.hlh.modules.impressionzino.application.PrintJobService;
import com.elpandor.hlh.modules.impressionzino.application.dto.PrintJobResume;
import com.elpandor.hlh.modules.impressionzino.domain.PrintJobStatut;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration et suivi de l'impression (authentification JWT Keycloak habituelle).
 * <p>
 * Les {@code @PreAuthorize} du projet sont inactifs (méthode security non activée) : le contrôle de rôle est donc
 * fait explicitement ici. Les rôles autorisés se règlent par {@code impression.admin-roles}.
 */
@RestController
@RequestMapping("/api/v1/impression")
@RequiredArgsConstructor
public class ImpressionAdminApi {

    private final PrintJobService printJobService;
    private final PrintAgentAdminService agentAdminService;

    @Value("${impression.admin-roles:ROLE_Admin,ROLE_Super-Admin,ROLE_frontend_Admin,ROLE_frontend_Super-Admin,ROLE_realm_Admin,ROLE_realm_Super-Admin}")
    private String[] rolesAdmin;

    public record CreerAgentRequest(String agentId, String codeEtablissement) {
    }

    public record AgentResume(String agentId, String codeEtablissement, boolean actif,
                              String derniereConnexion, String dateCreation) {
        static AgentResume from(PrintAgentEntity a) {
            return new AgentResume(a.getAgentId(), a.getCodeEtablissement(), a.isActif(),
                    a.getLastSeenAt() != null ? a.getLastSeenAt().toString() : null,
                    a.getDateCreation() != null ? a.getDateCreation().toString() : null);
        }
    }

    // ------------------------------------------------------------------ jobs

    /** Suivi des impressions : filtres optionnels par établissement et par statut. */
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> jobs(
            Authentication authentication,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "statut", required = false) String statutParam,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        exigerAdmin(authentication);
        PrintJobStatut statut = null;
        if (statutParam != null && !statutParam.isBlank() && !"undefined".equalsIgnoreCase(statutParam.trim())) {
            try {
                statut = PrintJobStatut.valueOf(statutParam.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Statut invalide : " + statutParam + " (PENDING, DISPATCHED, PRINTED, FAILED)");
            }
        }
        String codeFiltre = (code == null || code.isBlank()
                || "undefined".equalsIgnoreCase(code.trim()) || "null".equalsIgnoreCase(code.trim())) ? null : code;
        Page<PrintJobResume> resultat = printJobService.lister(codeFiltre, statut, page, size).map(PrintJobResume::from);
        return Utilities.createSuccessResponse(HttpStatus.OK, resultat, "Jobs d'impression");
    }

    /** Relance un job en échec définitif. */
    @PostMapping("/jobs/{id}/retry")
    public ResponseEntity<Map<String, Object>> relancer(Authentication authentication, @PathVariable("id") UUID id) {
        exigerAdmin(authentication);
        PrintJobResume job = PrintJobResume.from(printJobService.relancer(id));
        return Utilities.createSuccessResponse(HttpStatus.OK, job, "Job relancé");
    }

    // ------------------------------------------------------------------ agents

    @GetMapping("/agents")
    public ResponseEntity<Map<String, Object>> agents(Authentication authentication) {
        exigerAdmin(authentication);
        List<AgentResume> agents = agentAdminService.lister().stream().map(AgentResume::from).toList();
        return Utilities.createSuccessResponse(HttpStatus.OK, agents, "Agents d'impression");
    }

    /** Enregistre un poste. La clé d'API n'est affichée qu'ici, à noter dans la configuration du poste. */
    @PostMapping("/agents")
    public ResponseEntity<Map<String, Object>> creerAgent(Authentication authentication,
                                                          @RequestBody CreerAgentRequest request) {
        exigerAdmin(authentication);
        return reponseAvecCle(agentAdminService.creer(request.agentId(), request.codeEtablissement()),
                HttpStatus.CREATED, "Agent créé");
    }

    @PostMapping("/agents/{agentId}/rotate-key")
    public ResponseEntity<Map<String, Object>> regenererCle(Authentication authentication,
                                                            @PathVariable("agentId") String agentId) {
        exigerAdmin(authentication);
        return reponseAvecCle(agentAdminService.regenererCle(agentId), HttpStatus.OK, "Clé régénérée");
    }

    @PostMapping("/agents/{agentId}/revoke")
    public ResponseEntity<Map<String, Object>> revoquer(Authentication authentication,
                                                        @PathVariable("agentId") String agentId) {
        exigerAdmin(authentication);
        agentAdminService.revoquer(agentId);
        return Utilities.createSuccessResponse(HttpStatus.OK, (Object) null, "Agent révoqué");
    }

    // ------------------------------------------------------------------ utilitaires

    private ResponseEntity<Map<String, Object>> reponseAvecCle(PrintAgentAdminService.AgentAvecCle agent,
                                                               HttpStatus status, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("agentId", agent.agentId());
        data.put("codeEtablissement", agent.codeEtablissement());
        data.put("apiKey", agent.apiKey());
        data.put("avertissement", "Cette clé n'est affichée qu'une seule fois : à reporter dans la configuration du poste.");
        return Utilities.createSuccessResponse(status, data, message);
    }

    private void exigerAdmin(Authentication authentication) {
        boolean autorise = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> Arrays.asList(rolesAdmin).contains(role));
        if (!autorise) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle administrateur requis");
        }
    }
}