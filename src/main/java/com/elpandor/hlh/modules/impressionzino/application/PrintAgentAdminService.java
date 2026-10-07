package com.elpandor.hlh.modules.impressionzino.application;

import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentEntity;
import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintAgentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Gestion des agents d'impression : enregistrement d'un poste, rotation et révocation de sa clé.
 * La clé d'API n'est renvoyée qu'une fois, à la création ou à la rotation ; seule son empreinte est conservée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintAgentAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PrintAgentJpaRepository repository;

    /** Résultat d'une création ou d'une rotation : la clé en clair n'est disponible qu'ici. */
    public record AgentAvecCle(String agentId, String codeEtablissement, String apiKey) {
    }

    @Transactional
    public AgentAvecCle creer(String agentId, String codeEtablissement) {
        String id = agentId == null ? "" : agentId.trim();
        String code = CodeEtablissement.normaliser(codeEtablissement);
        if (id.isEmpty() || code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agentId et codeEtablissement sont obligatoires");
        }
        if (repository.findByAgentId(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un agent « " + id + " » existe déjà");
        }
        String cle = genererCle();
        repository.save(PrintAgentEntity.builder()
                .agentId(id)
                .codeEtablissement(code)
                .apiKeyHash(hash(cle))
                .actif(true)
                .build());
        log.info("Agent d'impression {} créé pour l'établissement {}", id, code);
        return new AgentAvecCle(id, code, cle);
    }

    @Transactional
    public AgentAvecCle regenererCle(String agentId) {
        PrintAgentEntity agent = trouver(agentId);
        String cle = genererCle();
        agent.setApiKeyHash(hash(cle));
        agent.setActif(true);
        log.info("Clé de l'agent d'impression {} régénérée", agent.getAgentId());
        return new AgentAvecCle(agent.getAgentId(), agent.getCodeEtablissement(), cle);
    }

    @Transactional
    public void revoquer(String agentId) {
        PrintAgentEntity agent = trouver(agentId);
        agent.setActif(false);
        log.info("Agent d'impression {} révoqué", agent.getAgentId());
    }

    @Transactional(readOnly = true)
    public List<PrintAgentEntity> lister() {
        return repository.findAll();
    }

    private PrintAgentEntity trouver(String agentId) {
        return repository.findByAgentId(agentId == null ? "" : agentId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent d'impression inconnu"));
    }

    private static String genererCle() {
        byte[] octets = new byte[32];
        RANDOM.nextBytes(octets);
        return "zpa_" + Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    /** Empreinte SHA-256 (hexadécimale) d'une clé d'API. */
    public static String hash(String cle) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(cle.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
