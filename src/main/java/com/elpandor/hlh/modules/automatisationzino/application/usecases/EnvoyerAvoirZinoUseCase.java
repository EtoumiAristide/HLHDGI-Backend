package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.DetailZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.AvoirRequest;
import com.elpandor.hlh.modules.hlh.model.dto.payload.FactureAvoirPayload;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.service.impl.ZinoApimServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traitement des tickets Zino à montant négatif : création de factures d'avoir.
 * <p>
 * Pour chaque article négatif d'un ticket, on recherche dans {@code factures_hlh} la facture de vente
 * originelle contenant le même article (même description que celle envoyée à la FNE), puis on envoie
 * l'avoir à la FNE avec la même logique que {@code FactureApi.saveAvoir}, et on persiste la facture d'avoir.
 * Un avoir est émis par facture originelle concernée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvoyerAvoirZinoUseCase {

    private static final int MAX_FACTURES_CANDIDATES = 20;

    private final ZinoApimServiceImpl zinoApimService;
    private final FactureService factureService;
    private final PointVenteService pointVenteService;

    @Value("${zino.api.entreprise}")
    private String entrepriseZino;

    /** Un ticket est un avoir si son montant (TTC, à défaut HT) est négatif. */
    public static boolean estTicketNegatif(TicketVenteZino ticket) {
        if (ticket.getMontantTTC() != null) return ticket.getMontantTTC() < 0;
        return ticket.getMontantHT() != null && ticket.getMontantHT() < 0;
    }

    /** Article à créditer, issu d'un ticket négatif. */
    private record ArticleAvoir(String libelle, String designation, int quantite) {}

    /** Avoir à émettre : une facture originelle + les lignes de cette facture à créditer. */
    private static class AvoirAEmettre {
        final FactureDto originale;
        final List<FactureAvoirPayload> lignes = new ArrayList<>();

        AvoirAEmettre(FactureDto originale) {
            this.originale = originale;
        }
    }

    public ResultatEnvoiFNE executer(List<TicketVenteZino> ticketsNegatifs, String nomFichierSource, String nomPointDeVente) {
        log.info("Création des factures d'avoir Zino. {} tickets négatifs.", ticketsNegatifs.size());

        int succes = 0;
        int echecs = 0;
        List<String> erreurs = new ArrayList<>();
        List<Map<String, String>> liensFacture = new ArrayList<>();

        try {
            PointVenteDto pointVente = resoudrePointVente(nomPointDeVente);
            if (pointVente == null) {
                return ResultatEnvoiFNE.builder()
                        .succes(false)
                        .codeErreur("POINT_VENTE_INTROUVABLE")
                        .message("Point de vente introuvable pour l'émission des avoirs: " + nomPointDeVente)
                        .build();
            }

            TokenResponse token = zinoApimService.auth();
            if (token == null || token.getAccessToken() == null) {
                return ResultatEnvoiFNE.builder()
                        .succes(false)
                        .codeErreur("AUTH_FAILED")
                        .message("Impossible de s'authentifier auprès de l'API ZINO (avoirs)")
                        .build();
            }

            for (TicketVenteZino ticket : ticketsNegatifs) {
                // Regroupement des articles du ticket par facture originelle
                Map<Integer, AvoirAEmettre> avoirs = new LinkedHashMap<>();
                boolean ticketEnErreur = false;

                for (ArticleAvoir article : extraireArticles(ticket)) {
                    try {
                        affecterArticle(pointVente, article, avoirs);
                    } catch (Exception e) {
                        ticketEnErreur = true;
                        echecs++;
                        String erreur = "Ticket " + ticket.getNumTicket() + " - article '" + article.designation() + "': " + e.getMessage();
                        erreurs.add(erreur);
                        log.error("Avoir impossible: {}", erreur);
                    }
                }

                for (AvoirAEmettre avoir : avoirs.values()) {
                    try {
                        Map<String, String> lien = emettreAvoir(token.getAccessToken(), ticket, avoir, pointVente, nomFichierSource);
                        succes++;
                        if (lien != null) liensFacture.add(lien);
                    } catch (Exception e) {
                        echecs++;
                        String erreur = "Ticket " + ticket.getNumTicket() + " - avoir sur facture " + avoir.originale.getNumFacture() + ": " + e.getMessage();
                        erreurs.add(erreur);
                        log.error("Échec envoi avoir: {}", erreur, e);
                    }
                }

                if (!ticketEnErreur && avoirs.isEmpty()) {
                    echecs++;
                    erreurs.add("Ticket " + ticket.getNumTicket() + ": aucun article à créditer");
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création des avoirs: {}", e.getMessage(), e);
            echecs++;
            erreurs.add("Exception: " + e.getMessage());
        }

        ResultatEnvoiFNE resultat = ResultatEnvoiFNE.builder()
                .succes(echecs == 0)
                .idTransaction("ZINO-AVOIR-" + System.currentTimeMillis())
                .message(String.format("Avoirs: %d succès, %d échecs sur %d tickets négatifs", succes, echecs, ticketsNegatifs.size()))
                .details(String.format("Total tickets Avoirs: %d, Factures générées: %d",
                        ticketsNegatifs.size(), succes))
                .liensFactureFNE(liensFacture)
                .build();
        if (echecs > 0) {
            resultat.setCodeErreur("PARTIAL_FAILURE");
            resultat.setMessage(resultat.getMessage() + " - Erreurs: " + String.join("; ", erreurs));
        }
        return resultat;
    }

    private PointVenteDto resoudrePointVente(String nomPointDeVente) {
        if (nomPointDeVente != null) {
            return pointVenteService.findByNom(nomPointDeVente);
        }
        List<PointVenteDto> pvs = pointVenteService.getAllByOrganisationName(entrepriseZino);
        return pvs.isEmpty() ? null : pvs.get(0);
    }

    /**
     * Articles à créditer pour un ticket : les détails négatifs (quantité ou montant), ou à défaut tous les détails.
     * Libellé construit comme dans {@code FacturePayloadConverter} pour correspondre à la description enregistrée côté FNE.
     */
    private List<ArticleAvoir> extraireArticles(TicketVenteZino ticket) {
        List<ArticleAvoir> articles = new ArrayList<>();
        List<DetailZino> details = ticket.getDetails();

        if (details == null || details.isEmpty()) {
            String designation = ticket.getDesignationPrincipale() != null ? ticket.getDesignationPrincipale() : "Vente Zino";
            articles.add(new ArticleAvoir(designation, designation, 1));
            return articles;
        }

        List<DetailZino> aCrediter = details.stream()
                .filter(d -> d.getMontantHT() < 0 || d.getQuantite() < 0)
                .toList();
        if (aCrediter.isEmpty()) aCrediter = details;

        for (DetailZino d : aCrediter) {
            String designation = d.getDesignation() != null ? d.getDesignation() : "Produit Zino";
            String libelle = d.getDesignation() != null ? d.getCodeProduit() + " - " + d.getDesignation() : "Produit Zino";
            int quantite = (int) Math.round(Math.abs(d.getQuantite()));
            if (quantite == 0) {
                log.warn("Quantité nulle pour l'article '{}' du ticket {}, 1 retenu", designation, ticket.getNumTicket());
                quantite = 1;
            }
            articles.add(new ArticleAvoir(libelle, designation, quantite));
        }
        return articles;
    }

    /**
     * Retrouve dans factures_hlh la facture originelle (la plus récente dont l'article a une quantité suffisante)
     * et ajoute la ligne correspondante (id de l'item FNE) à l'avoir de cette facture.
     */
    private void affecterArticle(PointVenteDto pointVente, ArticleAvoir article, Map<Integer, AvoirAEmettre> avoirs) {
        List<FactureDto> candidates = factureService.findFacturesVenteByArticle(
                pointVente.getId(), article.libelle(), article.designation(), MAX_FACTURES_CANDIDATES);

        if (candidates.isEmpty()) {
            throw new IllegalStateException("aucune facture originelle trouvée (libellé '" + article.libelle() + "')");
        }

        Gson gson = new Gson();
        for (FactureDto candidate : candidates) {
            JsonObject item = trouverItem(gson, candidate.getReponseFNE(), article);
            if (item == null) continue;
            if (!item.has("id") || item.get("id").isJsonNull()) continue;

            double quantiteOriginale = item.has("quantity") && !item.get("quantity").isJsonNull() ? item.get("quantity").getAsDouble() : Double.MAX_VALUE;
            AvoirAEmettre avoir = avoirs.get(candidate.getId());
            double dejaCredite = avoir == null ? 0 : avoir.lignes.stream()
                    .filter(l -> l.getId().equals(item.get("id").getAsString()))
                    .mapToInt(FactureAvoirPayload::getQuantite).sum();
            if (quantiteOriginale - dejaCredite < article.quantite()) continue;

            if (avoir == null) {
                avoir = new AvoirAEmettre(candidate);
                avoirs.put(candidate.getId(), avoir);
            }
            FactureAvoirPayload ligne = new FactureAvoirPayload();
            ligne.setId(item.get("id").getAsString());
            ligne.setDesignation(item.get("description").getAsString());
            ligne.setQuantite(article.quantite());
            avoir.lignes.add(ligne);
            log.info("Facture originelle {} retenue pour l'article '{}' (qté {})", candidate.getNumFacture(), article.designation(), article.quantite());
            return;
        }
        throw new IllegalStateException("aucune facture originelle avec une quantité suffisante (" + article.quantite() + ")");
    }

    private JsonObject trouverItem(Gson gson, String reponseFne, ArticleAvoir article) {
        try {
            JsonObject root = gson.fromJson(reponseFne, JsonObject.class);
            JsonArray items = root.getAsJsonObject("invoice").getAsJsonArray("items");
            for (JsonElement el : items) {
                JsonObject item = el.getAsJsonObject();
                if (!item.has("description") || item.get("description").isJsonNull()) continue;
                String description = item.get("description").getAsString().trim();
                if (description.equalsIgnoreCase(article.libelle().trim()) || description.equalsIgnoreCase(article.designation().trim())) {
                    return item;
                }
            }
        } catch (Exception e) {
            log.warn("Réponse FNE illisible pour la recherche d'article: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Envoi de l'avoir à la FNE puis persistance, sur le modèle de {@code FactureApi.saveAvoir}.
     */
    private Map<String, String> emettreAvoir(String accessToken, TicketVenteZino ticket, AvoirAEmettre avoir,
                                             PointVenteDto pointVente, String nomFichierSource) {
        FactureDto originale = avoir.originale;
        Gson gson = new Gson();

        JsonObject reponseOriginale = gson.fromJson(originale.getReponseFNE(), JsonObject.class);

        AvoirRequest avoirRequest = new AvoirRequest();
        avoirRequest.setType(TypeFacture.FACTURE_AVOIR.name());
        avoirRequest.setNumeroFacture(reponseOriginale.getAsJsonObject("invoice").get("id").getAsString());
        avoirRequest.setMessageCommercial("Avoir ticket " + ticket.getNumTicket());
        avoirRequest.setSelectedLines(avoir.lignes);

        boolean premiereVersion = pointVente.getEtablissement() != null
                && pointVente.getEtablissement().getOrganisation() != null
                && Boolean.TRUE.equals(pointVente.getEtablissement().getOrganisation().getIsAvoirFirstVersion());

        JsonObject facture = new JsonObject();
        facture.addProperty("typeFacture", avoirRequest.getType());
        if (premiereVersion) {
            facture.add("data", gson.fromJson(originale.getReponseFNE(), JsonObject.class));
        } else {
            facture.addProperty("data", gson.toJson(avoirRequest));
        }

        ResponseEntity<String> response = zinoApimService.sendData(accessToken, facture);
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("réponse FNE en échec: " + (response != null ? response.getBody() : "réponse null"));
        }

        // Persistance (même principe que saveAvoir : copie de la facture originelle passée en FACTURE_AVOIR)
        String requeteOriginale = originale.getReponseFNE();
        LocalDate dateFacture = ticket.getDate() != null
                ? ticket.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        originale.setId(null);
        originale.setTypeFacture(TypeFacture.FACTURE_AVOIR);
        originale.setDataSend(requeteOriginale);
        originale.setReponseFNE(response.getBody());
        originale.setDateFacture(dateFacture);
        originale.setPointVente(pointVente);
        originale.setIsAutomatisation(true);
        originale.setAutomatisationFileName(nomFichierSource);
        factureService.saveOrUpdate(originale);
        log.info("Facture d'avoir enregistrée pour le ticket {}", ticket.getNumTicket());

        try {
            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            Map<String, String> lien = new HashMap<>();
            lien.put("referenceFNE", json.get("reference").getAsString());
            lien.put("lienFNE", json.get("token").getAsString());
            return lien;
        } catch (Exception e) {
            log.warn("Lien FNE de l'avoir non extractible: {}", e.getMessage());
            return null;
        }
    }
}
