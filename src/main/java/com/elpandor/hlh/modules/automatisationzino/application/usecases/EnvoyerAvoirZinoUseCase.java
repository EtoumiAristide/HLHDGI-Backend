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
import com.elpandor.hlh.modules.impressionzino.application.dto.TicketAImprimer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.time.format.DateTimeFormatter;
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
    private final ObjectMapper objectMapper;

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
        List<TicketAImprimer> facturesImprimables = new ArrayList<>();

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
                        AvoirEmis emis = emettreAvoir(token.getAccessToken(), ticket, avoir, pointVente, nomFichierSource);
                        succes++;
                        if (emis.lien() != null) liensFacture.add(emis.lien());
                        if (emis.ticketImprimable() != null && nomFichierSource != null) {
                            facturesImprimables.add(emis.ticketImprimable());
                        }
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
                .facturesImprimables(facturesImprimables)
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
    private AvoirEmis emettreAvoir(String accessToken, TicketVenteZino ticket, AvoirAEmettre avoir,
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

        // Ticket à imprimer : construit AVANT la persistance, qui réécrit la facture originelle en facture d'avoir
        TicketAImprimer ticketImprimable = construireTicketAvoir(ticket, avoir, pointVente, response.getBody());

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
            return new AvoirEmis(lien, ticketImprimable);
        } catch (Exception e) {
            log.warn("Lien FNE de l'avoir non extractible: {}", e.getMessage());
            return new AvoirEmis(null, ticketImprimable);
        }
    }

    /** Résultat de l'émission d'un avoir : lien FNE et ticket à imprimer (peut être absent, sans bloquer l'avoir). */
    private record AvoirEmis(Map<String, String> lien, TicketAImprimer ticketImprimable) {}

    /**
     * Construit le ticket de l'avoir au format attendu par l'agent d'impression (JSON du FactureDto desktop) :
     * lignes créditées (quantités et prix de la facture originelle), totaux, référence et lien FNE de l'avoir.
     * Les quantités sont positives : l'application desktop affiche tous les montants en négatif pour un avoir.
     * <p>
     * Ne doit JAMAIS faire échouer l'émission : l'avoir est déjà accepté par la FNE, une erreur est seulement journalisée.
     */
    private TicketAImprimer construireTicketAvoir(TicketVenteZino ticket, AvoirAEmettre avoir, PointVenteDto pointVente,
                                                  String reponseFneAvoir) {
        try {
            FactureDto originale = avoir.originale;
            JsonNode reponse = objectMapper.readTree(reponseFneAvoir);
            String reference = reponse.path("reference").asText("");
            String lien = reponse.path("token").asText("");

            JsonNode origine = lireJson(originale.getDataSend());
            JsonNode itemsFne = lireJson(originale.getReponseFNE()).path("invoice").path("items");

            ObjectNode json = objectMapper.createObjectNode();
            String numTicket = String.valueOf(ticket.getNumTicket());
            String numeroFacture = "AVOIR_" + numTicket + "_" + (reference.isBlank() ? System.currentTimeMillis() : reference);

            double taux = origine.path("totauxPayload").path("tva").has("taux")
                    ? origine.path("totauxPayload").path("tva").path("taux").asDouble() : 18.0;
            ArrayNode lignes = json.putArray("lignes");
            double sommeNetHt = 0;
            long sommeTtc = 0;
            for (FactureAvoirPayload l : avoir.lignes) {
                JsonNode ligneOrigine = null;
                for (JsonNode lo : origine.path("lignes")) {
                    if (l.getDesignation() != null && l.getDesignation().trim().equalsIgnoreCase(lo.path("produit").asText("").trim())) {
                        ligneOrigine = lo;
                        break;
                    }
                }
                double prixUnitaire;
                double remise;
                String date = "";
                if (ligneOrigine != null) {
                    prixUnitaire = ligneOrigine.path("prixUnitaireHT").asDouble();
                    remise = ligneOrigine.path("remise").asDouble(0);
                    date = ligneOrigine.path("date").asText("");
                } else {
                    JsonNode item = null;
                    for (JsonNode it : itemsFne) {
                        if (l.getId() != null && l.getId().equals(it.path("id").asText(null))) {
                            item = it;
                            break;
                        }
                    }
                    prixUnitaire = item != null ? item.path("amount").asDouble() : 0;
                    remise = item != null ? item.path("discount").asDouble(0) : 0;
                }
                int quantite = l.getQuantite();
                double brut = prixUnitaire * quantite;
                sommeNetHt += brut * (1 - remise / 100.0);
                // TTC calculé par unité (prix HT + TVA arrondi), comme la FNE et la caisse : 5 x 2 000 = 10 000
                long ttcUnitaire = Math.round(prixUnitaire * (1 + taux / 100.0));
                sommeTtc += Math.round(ttcUnitaire * quantite * (1 - remise / 100.0));

                ObjectNode n = lignes.addObject();
                n.put("date", date);
                n.put("produit", l.getDesignation());
                n.put("quantite", quantite);
                n.put("prixUnitaireHT", prixUnitaire);
                n.put("montantHT", brut);   // brut avant remise, comme dans les factures de vente envoyées à la FNE
                n.put("remise", remise);
            }

            long ht = Math.round(sommeNetHt);
            long tva = sommeTtc - ht;
            ObjectNode totaux = json.putObject("totauxPayload");
            totaux.put("ht", ht);
            ObjectNode tvaNode = totaux.putObject("tva");
            tvaNode.put("base", ht);
            tvaNode.put("montant", tva);
            tvaNode.put("taux", taux);
            totaux.put("ttc", sommeTtc);

            json.put("numeroFacture", numeroFacture);
            json.put("numeroTicket", numTicket);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^ZINO_(.+)_\\d+$")
                    .matcher(originale.getNumFacture() != null ? originale.getNumFacture() : "");
            if (m.matches()) {
                json.put("numeroTicketOrigine", m.group(1));
            }
            LocalDate date = ticket.getDate() != null
                    ? ticket.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();
            json.put("dateFacture", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            json.put("typeFacture", TypeFacture.FACTURE_AVOIR.name());
            json.put("typeClient", origine.path("typeClient").asText(""));
            json.put("modePaiement", origine.path("modePaiement").asText(""));
            json.put("pointVente", origine.path("pointVente").asText(pointVente.getNom()));
            json.put("entreprise", origine.path("entreprise").asText(entrepriseZino));
            JsonNode client = origine.path("clientPayload");
            if (client.isObject()) {
                json.set("clientPayload", client);
            } else {
                json.putObject("clientPayload").put("nom", originale.getNomClient() != null ? originale.getNomClient() : "");
            }
            json.put("referenceFNE", reference);
            json.put("lienFNE", lien);
            return new TicketAImprimer(numeroFacture, TypeFacture.FACTURE_AVOIR.name(), objectMapper.writeValueAsString(json));
        } catch (Exception e) {
            log.error("Ticket de l'avoir du ticket {} non préparé pour l'impression (avoir déjà accepté par la FNE): {}",
                    ticket.getNumTicket(), e.getMessage(), e);
            return null;
        }
    }

    private JsonNode lireJson(String contenu) {
        try {
            return contenu == null || contenu.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(contenu);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
