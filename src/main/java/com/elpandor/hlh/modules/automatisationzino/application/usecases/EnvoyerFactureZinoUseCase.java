package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.domain.exception.EnvoiFNEException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.rest.FactureApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvoyerFactureZinoUseCase {

    private final FactureApi factureApi;
    private final FacturePayloadConverter facturePayloadConverter;
    private final ObjectMapper objectMapper;


     //Envoie les tickets Zino à la FNE via FactureApi.save()

    public ResultatEnvoiFNE executer(List<TicketVenteZino> tickets, Jwt jwt) throws EnvoiFNEException {
        log.info("Envoi des factures Zino à la FNE. {} tickets.", tickets.size());

        try {
            // 1. Conversion en FacturePayload (regroupé par mode de paiement)
            List<FacturePayload> facturePayloads = facturePayloadConverter.convertir(tickets);

            // 2. Pour chaque facture, appel à FactureApi.save()
            int successCount = 0;
            int errorCount = 0;
            List<String> erreurs = new ArrayList<>();

            for (FacturePayload facture : facturePayloads) {
                try {
                    log.info("Envoi de la facture: {}", facture.getNumeroFacture());

                    // Sérialisation de la facture en JSON
                    String dataFacture = objectMapper.writeValueAsString(facture);

                    // Appel à FactureApi.save() - retourne ResponseEntity<Map<String, Object>>
                    ResponseEntity<Map<String, Object>> responseEntity = factureApi.save(null, "FACTURE_VENTE", "B2C",
                            facture.getModePaiement().toString(), "pv", "", "Envoi automatique - Batch Zino",
                            dataFacture, null, jwt);

                    // Analyse de la réponse
                    if (responseEntity != null) {
                        // Récupération du body
                        Map<String, Object> responseBody = responseEntity.getBody();

                        // Vérification du statut HTTP
                        boolean isSuccess = responseEntity.getStatusCode().is2xxSuccessful();

                        if (isSuccess) {
                            successCount++;
                            log.info("Facture envoyée avec succès: {}", facture.getNumeroFacture());
                        } else {
                            errorCount++;
                            String message = "Statut HTTP: " + responseEntity.getStatusCode();
                            if (responseBody != null && responseBody.get("message") != null) {
                                message = responseBody.get("message").toString();
                            }
                            String erreur = "Échec pour " + facture.getNumeroFacture() + ": " + message;
                            erreurs.add(erreur);
                            log.error("{}", erreur);
                        }
                    } else {
                        errorCount++;
                        String erreur = "Réponse nulle pour " + facture.getNumeroFacture();
                        erreurs.add(erreur);
                        log.error("{}", erreur);
                    }

                } catch (Exception e) {
                    errorCount++;
                    String erreur = "Exception pour " + facture.getNumeroFacture() + ": " + e.getMessage();
                    erreurs.add(erreur);
                    log.error("{}", erreur, e);
                }
            }

            // 3. Construction du résultat
            boolean globalSuccess = errorCount == 0;

            ResultatEnvoiFNE resultat = ResultatEnvoiFNE.builder()
                    .succes(globalSuccess)
                    .idTransaction("BATCH-" + System.currentTimeMillis())
                    .message(String.format("Envoi terminé: %d succès, %d échecs sur %d factures",
                            successCount, errorCount, facturePayloads.size()))
                    .details(String.format("Total tickets: %d, Factures générées: %d",
                            tickets.size(), facturePayloads.size()))
                    .build();

            if (!globalSuccess) {
                resultat.setCodeErreur("PARTIAL_FAILURE");
                resultat.setMessage(resultat.getMessage() + " - Erreurs: " + String.join("; ", erreurs));
            }

            log.info("Résultat final: {}", resultat.getMessage());
            return resultat;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi à la FNE: {}", e.getMessage(), e);
            throw new EnvoiFNEException("Erreur d'envoi à la FNE: " + e.getMessage(), e);
        }
    }
}