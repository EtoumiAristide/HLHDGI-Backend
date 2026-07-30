package com.elpandor.hlh.modules.automatisation.service;

import com.elpandor.hlh.common.utils.InputStreamMultipartFile;
import com.elpandor.hlh.modules.automatisation.model.zino.dto.TicketVente;
import com.elpandor.hlh.modules.automatisation.model.zino.dto.payload.ListeFilesDto;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.service.impl.ZinoApimServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import io.swagger.v3.core.util.Json;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketToFactureTransformer transformer;
    private final PointVenteService pointVenteService;

    private final ZinoApimServiceImpl zinoApimService;
    private final FactureService factureService;
    private final ZinoCsvParserService zinoCsvParserService;

    @Value("${zino.api.entreprise}")
    public String entreprise;

    /**
     * Transforme un ticket en facture
     */
    public FacturePayload transformTicketToFacture(TicketVente ticket, PointVenteDto pointVente) {
        log.info("Transformation du ticket {} en facture", ticket.getNumeroTicket());

//        List<PointVenteDto> pointVentes = pointVenteService.getAllByOrganisationName(entreprise);
        return transformer.transform(ticket, pointVente);
    }

    /**
     * Transforme tous les tickets en factures
     */
    public List<FacturePayload> transformTicketsToFactures(List<TicketVente> tickets, PointVenteDto pointVente) {
        log.info("Transformation de {} tickets en factures", tickets.size());

//        List<PointVenteDto> pointVentes = pointVenteService.getAllByOrganisationName(entreprise);
        return transformer.transformAll(tickets, pointVente);
    }

    /**
     * Transforme et affiche les résultats
     */
    public void processAndDisplayTickets(List<TicketVente> tickets, PointVenteDto pointVente) {
        List<FacturePayload> factures = transformTicketsToFactures(tickets,  pointVente);

        factures.forEach(facture -> {
            log.info("=== FACTURE {} ===", facture.getNumeroFacture());
            log.info("Client: {}", facture.getClientPayload().getNom());
            log.info("Date: {}", facture.getDateFacture());
            log.info("Type client: {}", facture.getTypeClient());
            log.info("Mode paiement: {}", facture.getModePaiement());
            log.info("Nombre de produits: {}", facture.getLignes().size());
            log.info("HT: {}", facture.getTotauxPayload().getHt());
            log.info("TVA: {}", facture.getTotauxPayload().getTva().getMontant());
            log.info("TDT: {}", facture.getTotauxPayload().getTdt().getMontant());
            log.info("TCN: {}", facture.getTotauxPayload().getTcn().getMontant());
            log.info("TTC: {}", facture.getTotauxPayload().getTtc());
            log.info("-----------------------------------");
        });

        log.info("✅ Total factures transformées: {}", factures.size());
    }

    @Transactional
    public List<FacturePayload> runAutomatisation() throws Exception {

        List<FacturePayload> output = new ArrayList<>();

        List<PointVenteDto> pointVentes = pointVenteService.getAllByOrganisationName(entreprise);

        //Appel API Recupération liste des fichiers
        ResponseEntity<List<ListeFilesDto>> responseListeFileAPI = zinoApimService.listeFiles();

        if (responseListeFileAPI.getStatusCode().is2xxSuccessful() && responseListeFileAPI.getBody() != null) {

            List<ListeFilesDto> listeFiles = responseListeFileAPI.getBody();

            for (ListeFilesDto listeFile : listeFiles) {
                //Verification si le fichier est déjà traité
                List<FactureDto> facturexist = factureService.findByAutomatisationFileName(listeFile.getFilename());

                if (facturexist != null && !facturexist.isEmpty()) continue; //On s'assure d'eviter les doublons

                //Appel API téléchargement du fichier
                ResponseEntity<Resource> downloadFileAPI = zinoApimService.downloadFile(listeFile.getFilename());

                if (downloadFileAPI.getStatusCode().is2xxSuccessful() && downloadFileAPI.getBody() != null) {

                    Resource resource = downloadFileAPI.getBody();
                    InputStream is = resource.getInputStream();
                    MultipartFile fichier = new InputStreamMultipartFile(is, listeFile.getFilename(), listeFile.getFilename(), listeFile.getFiletype());

                    List<TicketVente> tickets = zinoCsvParserService.parseCsvFile(fichier);
                    List<FacturePayload> factures = this.transformTicketsToFactures(tickets, !pointVentes.isEmpty() ? pointVentes.get(0) : null);

                    //Envoi à la FNE
                    TokenResponse tokenResponse = tokenResponse = zinoApimService.auth();
                    ResponseEntity<String> response = null;
                    int indexFacture = 0;

                    if (tokenResponse != null) {
                        for (FacturePayload facture : factures) {
                            String request = Json.pretty(facture);
                            String ticketReq = Json.pretty(tickets.get(indexFacture));

                            response = zinoApimService.sendData(tokenResponse.getAccessToken(), facture);

                            //Gestion de la date de facture
                            LocalDate dateFacture = null;
                            dateFacture = facture.getDateFacture() != null ? LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now();

                            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                                FactureDto factureDto = FactureDto.builder()
                                        .numFacture(facture.getNumeroFacture())
                                        .dateFacture(dateFacture)
                                        .nomClient(facture.getClientPayload().getNom())
                                        //.lienFichier(storeName)
                                        .typeFacture(facture.getTypeFacture())
                                        .typeClient(facture.getTypeClient())
                                        .modePaiement(facture.getModePaiement())
                                        .dataSend(request)
                                        .reponseFNE(response.getBody())
                                        .pointVente(!pointVentes.isEmpty() ? pointVentes.get(0) : null)
                                        .isAutomatisation(true)
                                        .automatisationFileName(listeFile.getFilename())
                                        .automatisationFileSize(listeFile.getFileSize())
                                        .automatisationFileType(listeFile.getFiletype())
                                        .ticketExtractedData(ticketReq)
                                        .build();

                                factureService.saveOrUpdate(factureDto);
                                output.add(facture);

                            } else {
                                log.error("L'authentification de la facture à échoué");
                            }

                            indexFacture++;
                        }
                    } else {
                        log.error("Récupération du token pour l'authentification de la facture à échoué");
                    }

                    log.info("Fichier traité: {}", fichier.getOriginalFilename());
                }
                break;//Supprimer en production
            }
        }
        log.info("Total de factures traitée: {}", output.size());
        return output;
    }
}
