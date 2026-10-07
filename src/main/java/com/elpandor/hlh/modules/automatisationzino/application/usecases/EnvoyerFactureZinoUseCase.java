package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.domain.exception.EnvoiFNEException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.service.impl.ZinoApimServiceImpl;
import com.elpandor.hlh.modules.impressionzino.application.dto.TicketAImprimer;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.v3.core.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvoyerFactureZinoUseCase {

    private final ZinoApimServiceImpl zinoApimService;
    private final FacturePayloadConverter facturePayloadConverter;

    private final FactureService factureService;

    private final ObjectMapper objectMapper;

    private final PointVenteService pointVenteService;

    @Value("${zino.api.entreprise}")
    private String entrepriseZino;


    //Envoie les tickets Zino à la FNE via ZinoApimService

    public ResultatEnvoiFNE executer(List<TicketVenteZino> tickets, String nomFichierSource, String nomPointDeVente) throws EnvoiFNEException {

        log.info("Envoi des factures Zino à la FNE. {} tickets.", tickets.size());

        try {
            // 1. Authentification
            log.info("Authentification auprès de l'API Manager ZINO...");
            TokenResponse tokenResponse = zinoApimService.auth();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("Échec de l'authentification ZINO");
                return ResultatEnvoiFNE.builder()
                        .succes(false)
                        .codeErreur("AUTH_FAILED")
                        .message("Impossible de s'authentifier auprès de l'API ZINO")
                        .build();
            }

            log.info("Authentification ZINO réussie");

            // 2. Conversion en FacturePayload (regroupé par mode de paiement)
            List<FacturePayload> facturePayloads = facturePayloadConverter.convertir(tickets);
            log.info("factures générées {}", facturePayloads.size());

            // 3. Pour chaque facture, envoi via ZinoApimService
            int successCount = 0;
            int errorCount = 0;
            List<String> erreurs = new ArrayList<>();

            //Recherche de point de vente avec le nom de l'entreprise zino
            List<PointVenteDto> pointVenteDtoList = new ArrayList<>();
            if (nomPointDeVente == null) {
                pointVenteDtoList = pointVenteService.getAllByOrganisationName(entrepriseZino);
            } else {
                PointVenteDto pointVente = pointVenteService.findByNom(nomPointDeVente);
                if (pointVente != null) pointVenteDtoList.add(pointVente);
            }
            List<Map<String, String>> liensFacture = new ArrayList<>();
            // Tickets à imprimer : une entrée par facture ACCEPTÉE par la FNE (voir PrintJobService)
            List<TicketAImprimer> facturesImprimables = new ArrayList<>();

            for (FacturePayload facture : facturePayloads) {
                try {
                    log.info("Envoi de la facture: {}", facture.getNumeroFacture());

//                    log.info("Point.s de vente trouvé.s: {}", pointVenteDtoList.size());
//                    log.info("Détails Point.s de vente: {}", pointVenteDtoList);
                    facture.setEntreprise(!pointVenteDtoList.isEmpty() ? pointVenteDtoList.get(0).getEtablissement().getNom() : null);
                    facture.setPointVente(!pointVenteDtoList.isEmpty() ? pointVenteDtoList.get(0).getNom() : null);

                    // Appel à l'API ZINO via ApimService
                    ResponseEntity<String> response = zinoApimService.sendData(
                            tokenResponse.getAccessToken(),
                            facture
                    );

                    // Analyse de la réponse
                    if (response != null && response.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                        log.info("Facture envoyée avec succès: {}", facture.getNumeroFacture());
                        log.debug("   Réponse: {}", response.getBody());


                        //Gestion de la date de facture
                        LocalDate dateFacture = null;

                        String request = Json.pretty(facture);
                        try {
                            dateFacture = facture.getDateFacture() != null ? LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now();
                        } catch (Exception ex) {
                            dateFacture = LocalDate.now();
                            ex.printStackTrace();
                        }

                        FactureDto factureDto = FactureDto.builder()
                                .numFacture(facture.getNumeroFacture())
                                .dateFacture(dateFacture)
                                .nomClient(facture.getClientPayload().getNom())
//                                .lienFichier(tickets)
                                .typeFacture(facture.getTypeFacture())
                                .typeClient(facture.getTypeClient())
                                .modePaiement(facture.getModePaiement())
                                .dataSend(request)
                                .reponseFNE(response.getBody())
                                .bkExtractedData(null)
                                .pointVente(pointVenteDtoList.get(0))
                                .isAutomatisation(true)
                                .automatisationFileName(nomFichierSource)
                                .build();

                        factureService.saveOrUpdate(factureDto);

                        if (nomFichierSource != null) {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(response.getBody(), JsonObject.class);

                            Map<String, String> dataFacture = new HashMap<>();
                            dataFacture.put("referenceFNE", jsonObject.get("reference").getAsString());
                            dataFacture.put("lienFNE", jsonObject.get("token").getAsString());
                            liensFacture.add(dataFacture);

                            ajouterTicketAImprimer(facturesImprimables, facture, dataFacture);
                        }

                    } else {
                        errorCount++;
                        String message = response != null ? response.getBody() : "Réponse null";
                        String erreur = "Échec pour " + facture.getNumeroFacture() + ": " + message;
                        erreurs.add(erreur);
                        log.error(" Erreur {}", erreur);
                    }

                } catch (Exception e) {
                    errorCount++;
                    String erreur = "Exception pour " + facture.getNumeroFacture() + ": " + e.getMessage();
                    erreurs.add(erreur);
                    log.error("erreur {}", erreur, e);
                }
            }

            // 4. Construction du résultat
            boolean globalSuccess = errorCount == 0;

            ResultatEnvoiFNE resultat = ResultatEnvoiFNE.builder()
                    .succes(globalSuccess)
                    .idTransaction("ZINO-BATCH-" + System.currentTimeMillis())
                    .message(String.format("Envoi terminé: %d succès, %d échecs sur %d factures",
                            successCount, errorCount, facturePayloads.size()))
                    .details(String.format("Total tickets: %d, Factures générées: %d",
                            tickets.size(), facturePayloads.size()))
                    .build();

            if (!globalSuccess) {
                resultat.setCodeErreur("PARTIAL_FAILURE");
                resultat.setMessage(resultat.getMessage() + " - Erreurs: " + String.join("; ", erreurs));
            }

            if (nomPointDeVente != null) {
                resultat.setLiensFactureFNE(liensFacture);
            }
            resultat.setFacturesImprimables(facturesImprimables);

            log.info("Résultat final: {}", resultat.getMessage());
            return resultat;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi à la FNE: {}", e.getMessage(), e);
            throw new EnvoiFNEException("Erreur d'envoi à la FNE: " + e.getMessage(), e);
        }
    }

    /**
     * Construit le ticket à imprimer : la facture envoyée à la FNE (lignes, totaux, point de vente, entreprise)
     * complétée de la référence et du lien de vérification FNE (QR code). Le JSON a le format du {@code FactureDto}
     * de l'application desktop ; les champs inutiles à l'impression y sont ignorés.
     * <p>
     * Ne doit JAMAIS faire échouer l'envoi : la facture est déjà acceptée et enregistrée, une erreur ici est
     * seulement journalisée (le ticket pourra être régénéré depuis la facture enregistrée).
     */
    private void ajouterTicketAImprimer(List<TicketAImprimer> cible, FacturePayload facture, Map<String, String> lienFne) {
        try {
            ObjectNode ticket = objectMapper.valueToTree(facture);
            ticket.put("referenceFNE", lienFne.get("referenceFNE"));
            ticket.put("lienFNE", lienFne.get("lienFNE"));
            cible.add(new TicketAImprimer(
                    facture.getNumeroFacture(),
                    TypeFacture.FACTURE_VENTE.name(),
                    objectMapper.writeValueAsString(ticket)));
        } catch (Exception e) {
            log.error("Ticket non préparé pour l'impression de la facture {} (déjà acceptée par la FNE): {}",
                    facture.getNumeroFacture(), e.getMessage(), e);
        }
    }
}