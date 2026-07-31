package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.domain.exception.EnvoiFNEException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.service.impl.ZinoApimServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    public ResultatEnvoiFNE executer(List<TicketVenteZino> tickets) throws EnvoiFNEException {

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
            List<PointVenteDto> pointVenteDtoList = pointVenteService.getAllByOrganisationName(entrepriseZino);

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
                                .build();

                        factureService.saveOrUpdate(factureDto);

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

            log.info("Résultat final: {}", resultat.getMessage());
            return resultat;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi à la FNE: {}", e.getMessage(), e);
            throw new EnvoiFNEException("Erreur d'envoi à la FNE: " + e.getMessage(), e);
        }
    }
}