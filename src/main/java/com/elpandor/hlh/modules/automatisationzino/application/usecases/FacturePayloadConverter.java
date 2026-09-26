package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.DetailZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.RepartitionParModePaiement;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Convertisseur des tickets Zino en FacturePayload
 * Réutilise la logique du contrôleur FactureApi.traitementFactureZino()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacturePayloadConverter {

    /**
     * Convertit une liste de tickets Zino en liste de FacturePayload.
     * Chaque TicketVenteZino donne lieu à exactement une FacturePayload
     * (plus de regroupement par mode de paiement).
     *
     * @param tickets Liste des tickets extraits du fichier CSV
     * @return Liste des FacturePayload prêtes à être envoyées à la FNE, une par ticket
     */
    public List<FacturePayload> convertir(List<TicketVenteZino> tickets) {
        log.info("Conversion de {} tickets Zino en FacturePayload", tickets.size());

        if (tickets.isEmpty()) {
            log.warn("Aucun ticket à convertir");
            return new ArrayList<>();
        }

        List<FacturePayload> factures = new ArrayList<>();

        // Un ticket Zino = une FacturePayload (plus de regroupement par mode de paiement)
        for (TicketVenteZino ticket : tickets) {
            FacturePayload facture = creerFactureDepuisTicket(ticket);
            factures.add(facture);

            log.debug("Facture générée pour le ticket {}: mode={}, HT={}, TTC={}",
                    ticket.getNumTicket(),
                    facture.getModePaiement(),
                    facture.getTotauxPayload().getHt(),
                    facture.getTotauxPayload().getTtc());
        }

        log.info("Conversion terminée: {} factures générées", factures.size());
        return factures;
    }

    /**
     * Crée une FacturePayload à partir d'un unique ticket Zino
     */
    private FacturePayload creerFactureDepuisTicket(TicketVenteZino ticket) {
        FacturePayload facture = new FacturePayload();

        // --- Date ---
        if (ticket.getDate() != null) {
            facture.setDateFacture(new SimpleDateFormat("dd/MM/yyyy").format(ticket.getDate()));
        }

        // --- Types ---
        facture.setTypeFacture(TypeFacture.FACTURE_VENTE);
        facture.setTypeClient(TypeClient.B2C);

        // --- Normalisation du mode de paiement : "glovo" -> "Espèces Franc CFA" ---
        String modePaiementBrut = ticket.getModePaiement() != null ? ticket.getModePaiement() : "INCONNU";
        if ("glovo".equalsIgnoreCase(modePaiementBrut)) {
            modePaiementBrut = "Espèces Franc CFA";
        }

        // --- SheetName (pour l'identification) ---
        facture.setSheetName(modePaiementBrut + " - ZINO - Ticket " + ticket.getNumTicket());

        // --- Mode de paiement (mapping vers ModePaiement enum) ---
        facture.setModePaiement(mapperModePaiement(modePaiementBrut));

        // --- Client ---
        ClientPayload client = new ClientPayload();
        client.setNom(ticket.getNomClient());
        client.setNumeroCC("");
        facture.setClientPayload(client);

        // --- Lignes de produits ---
        List<LigneProduitPayload> lignes = new ArrayList<>();
        double totalHT = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;
        double tauxTvaReference = 18.0;

        if (ticket.getDetails() != null && !ticket.getDetails().isEmpty()) {
            // Parcourir tous les détails du ticket
            for (DetailZino detail : ticket.getDetails()) {
                LigneProduitPayload ligne = new LigneProduitPayload();
                ligne.setProduit(detail.getDesignation() != null ? detail.getCodeProduit() + " - " + detail.getDesignation() : "Produit Zino");
                ligne.setMontantHT(detail.getMontantHT());
                ligne.setQuantite((int) Math.round(detail.getQuantite())); // Convertir double en int
                ligne.setPrixUnitaireHT(detail.getPrixUnitaire());
                ligne.setDate(facture.getDateFacture());
                ligne.setRemise(detail.getRemise());

                lignes.add(ligne);

                // Accumuler les totaux
                totalHT += detail.getMontantHT();
                totalTVA += detail.getTva();
                totalTTC += (detail.getMontantHT() + detail.getTva());

                if (detail.getTauxTVA() > 0) {
                    tauxTvaReference = detail.getTauxTVA();
                }
            }
        } else {
            // Fallback: Si pas de détails, utiliser les données principales du ticket
            log.warn("Ticket {} sans détails, utilisation des données principales", ticket.getNumTicket());
            LigneProduitPayload ligne = new LigneProduitPayload();
            ligne.setProduit(ticket.getDesignationPrincipale() != null ?
                    ticket.getDesignationPrincipale() : "Vente Zino");
            ligne.setMontantHT(ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0);
            ligne.setQuantite(1);
            ligne.setPrixUnitaireHT(ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0);
            ligne.setDate(facture.getDateFacture());
            ligne.setRemise(0.0);
            lignes.add(ligne);

            totalHT += ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0;
            totalTVA += ticket.getTva() != null ? ticket.getTva() : 0.0;
            totalTTC += ticket.getMontantTTC() != null ? ticket.getMontantTTC() : 0.0;
        }
        facture.setLignes(lignes);

        // --- Totaux ---
        TotauxPayload totaux = new TotauxPayload();
        totaux.setHt(totalHT);

        // TVA
        TaxePayload tva = new TaxePayload();
        tva.setTaux(tauxTvaReference);
        tva.setBase(totalHT);
        tva.setMontant(totalTVA);
        totaux.setTva(tva);

        // TTC
        totaux.setTtc(totalTTC);
        totaux.setModePaiement(modePaiementBrut);
        facture.setTotauxPayload(totaux);

        // --- Numéro de facture : basé sur le numéro de ticket Zino pour la traçabilité 1:1 ---
        String refTicket = ticket.getNumTicket() != null ? String.valueOf(ticket.getNumTicket()) : String.valueOf(System.currentTimeMillis());
        facture.setNumeroFacture("ZINO_" + refTicket + "_" + System.currentTimeMillis());

        return facture;
    }

    /**
     * Mapping du mode de paiement Zino vers ModePaiement HLH
     * (Reprend exactement la logique du controller FactureApi)
     *
     * @param modePaiement Mode de paiement extrait du fichier Zino
     * @return ModePaiement HLH correspondant
     */
    private ModePaiement mapperModePaiement(String modePaiement) {
        if (modePaiement == null) {
            return ModePaiement.cash;
        }

        String mode = modePaiement.toLowerCase().trim();

        // 1. Mobile Money (Orange Money, Wave, MTN, MOOV, etc.)
        if (mode.contains("mobile") || mode.contains("orange") ||
                mode.contains("wave") || mode.contains("mtn") || mode.contains("moov") ||
                mode.contains("money") || mode.contains("mobile money")) {
            return ModePaiement.mobilemoney;
        }

        // 2. Carte bancaire (CC, Carte Bancaire, Card)
        if (mode.contains("carte") || mode.contains("cc") || mode.contains("card") ||
                mode.contains("bancaire") || mode.contains("cb")) {
            return ModePaiement.card;
        }

        // 3. Chèque
        if (mode.contains("chèque") || mode.contains("cheque") || mode.contains("check")) {
            return ModePaiement.check;
        }

        // 4. Espèces / Cash / Glovo
        if (mode.contains("glovo") || mode.contains("espece") || mode.contains("espèce") ||
                mode.contains("cash") || mode.contains("espèces") || mode.contains("especes")) {
            return ModePaiement.cash;
        }

        // 5. Par défaut : cash
        log.debug("Mode de paiement non reconnu '{}', mapping vers CASH par défaut", modePaiement);
        return ModePaiement.cash;
    }

    /**
     * Vérifie si une conversion est nécessaire ou si les données sont déjà au bon format
     * Utile pour les tests et les validations
     */
    public boolean isConvertible(List<TicketVenteZino> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }
        return tickets.stream().allMatch(t ->
                t.getMontantHT() != null && t.getMontantTTC() != null
        );
    }

    /**
     * Calcule la répartition par mode de paiement (comme dans le controller)
     * Utile pour les rapports et le suivi
     */
    public List<RepartitionParModePaiement> calculerRepartitionParModePaiement(List<TicketVenteZino> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, RepartitionParModePaiement> repartitionMap = new HashMap<>();

        for (TicketVenteZino ticket : tickets) {
            String mode = ticket.getModePaiement() != null ? ticket.getModePaiement() : "INCONNU";

            // Normalisation (comme dans le controller)
            if ("glovo".equalsIgnoreCase(mode)) {
                mode = "Espèces Franc CFA";
            }

            RepartitionParModePaiement dto = repartitionMap.getOrDefault(mode, new RepartitionParModePaiement());
            dto.setDate(ticket.getDate());
            dto.setModePaiement(mode);
            dto.setTotalMontantTTC(dto.getTotalMontantTTC() != null ?
                    dto.getTotalMontantTTC() + ticket.getMontantTTC() : ticket.getMontantTTC());
            dto.setTotalMontantHT(dto.getTotalMontantHT() != null ?
                    dto.getTotalMontantHT() + ticket.getMontantHT() : ticket.getMontantHT());
            dto.setTotalTVA(dto.getTotalTVA() != null ?
                    dto.getTotalTVA() + ticket.getTva() : ticket.getTva());
            dto.setNombreTransactions(dto.getNombreTransactions() + 1);
            dto.setDesignationPrincipale(ticket.getDesignationPrincipale());

            repartitionMap.put(mode, dto);
        }

        return new ArrayList<>(repartitionMap.values());
    }


    //Valide la structure d'une FacturePayload avant envoi

    public boolean validerFacturePayload(FacturePayload facture) {
        if (facture == null) {
            log.warn("FacturePayload null");
            return false;
        }

        boolean valide = true;

        if (facture.getNumeroFacture() == null || facture.getNumeroFacture().isEmpty()) {
            log.warn("Numéro de facture manquant");
            valide = false;
        }

        if (facture.getModePaiement() == null) {
            log.warn("Mode de paiement manquant");
            valide = false;
        }

        if (facture.getClientPayload() == null) {
            log.warn("Client manquant");
            valide = false;
        }

        if (facture.getLignes() == null || facture.getLignes().isEmpty()) {
            log.warn("Aucune ligne de produit");
            valide = false;
        }

        if (facture.getTotauxPayload() == null) {
            log.warn("Totaux manquants");
            valide = false;
        }

        if (valide) {
            log.debug("FacturePayload valide: {}", facture.getNumeroFacture());
        }

        return valide;
    }
}