package com.elpandor.hlh.modules.automatisationzino.application.usecases;

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
     * Convertit une liste de tickets Zino en liste de FacturePayload
     * Regroupement par mode de paiement (comme dans le controller Zino)
     *
     * @param tickets Liste des tickets extraits du fichier CSV
     * @return Liste des FacturePayload prêtes à être envoyées à la FNE
     */
    public List<FacturePayload> convertir(List<TicketVenteZino> tickets) {
        log.info("Conversion de {} tickets Zino en FacturePayload", tickets.size());

        if (tickets == null || tickets.isEmpty()) {
            log.warn("Aucun ticket à convertir");
            return new ArrayList<>();
        }

        List<FacturePayload> factures = new ArrayList<>();

        // 1. Regrouper par mode de paiement (comme dans traitementFactureZino())
        Map<String, List<TicketVenteZino>> ticketsParMode = new LinkedHashMap<>();

        for (TicketVenteZino ticket : tickets) {
            String mode = ticket.getModePaiement() != null ? ticket.getModePaiement() : "INCONNU";

            // Normalisation : "glovo" → "Espèces Franc CFA" (comme dans le controller)
            if ("glovo".equalsIgnoreCase(mode)) {
                mode = "Espèces Franc CFA";
            }

            ticketsParMode.computeIfAbsent(mode, k -> new ArrayList<>()).add(ticket);
        }

        log.debug("Répartition par mode de paiement: {}", ticketsParMode.keySet());

        // 2. Pour chaque mode de paiement, créer une FacturePayload
        for (Map.Entry<String, List<TicketVenteZino>> entry : ticketsParMode.entrySet()) {
            String modePaiement = entry.getKey();
            List<TicketVenteZino> ticketsDuMode = entry.getValue();

            FacturePayload facture = creerFacturePourMode(modePaiement, ticketsDuMode);
            factures.add(facture);

            log.debug("Facture générée pour {}: {} tickets, HT={}, TTC={}",
                    modePaiement,
                    ticketsDuMode.size(),
                    facture.getTotauxPayload().getHt(),
                    facture.getTotauxPayload().getTtc());
        }

        log.info("Conversion terminée: {} factures générées", factures.size());
        return factures;
    }

    /**
     * Crée une FacturePayload pour un mode de paiement donné
     */
    private FacturePayload creerFacturePourMode(String modePaiement, List<TicketVenteZino> tickets) {
        FacturePayload facture = new FacturePayload();

        // --- Date ---
        TicketVenteZino premierTicket = tickets.get(0);
        if (premierTicket.getDate() != null) {
            facture.setDateFacture(new SimpleDateFormat("dd/MM/yyyy").format(premierTicket.getDate()));
        }

        // --- Types ---
        facture.setTypeFacture(TypeFacture.FACTURE_VENTE);
        facture.setTypeClient(TypeClient.B2C);

        // --- SheetName (pour l'identification) ---
        facture.setSheetName(modePaiement + " - ZINO");

        // --- Mode de paiement (mapping vers ModePaiement enum) ---
        facture.setModePaiement(mapperModePaiement(modePaiement));

        // --- Client ---
        ClientPayload client = new ClientPayload();
        client.setNom(modePaiement + " - ZINO");
        client.setNumeroCC("");
        facture.setClientPayload(client);

        // --- Lignes de produits ---
        List<LigneProduitPayload> lignes = new ArrayList<>();
        double totalHT = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;

        for (TicketVenteZino ticket : tickets) {
            LigneProduitPayload ligne = new LigneProduitPayload();
            ligne.setProduit(ticket.getDesignationPrincipale() != null ?
                    ticket.getDesignationPrincipale() : "Vente Zino");
            ligne.setMontantHT(ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0);
            ligne.setQuantite(1);
            ligne.setPrixUnitaireHT(ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0);
            ligne.setDate(facture.getDateFacture());
            lignes.add(ligne);

            totalHT += ticket.getMontantHT() != null ? ticket.getMontantHT() : 0.0;
            totalTVA += ticket.getTva() != null ? ticket.getTva() : 0.0;
            totalTTC += ticket.getMontantTTC() != null ? ticket.getMontantTTC() : 0.0;
        }
        facture.setLignes(lignes);

        // --- Totaux (comme dans traitementFactureZino) ---
        TotauxPayload totaux = new TotauxPayload();
        totaux.setHt(totalHT);

        // TVA
        TaxePayload tva = new TaxePayload();
        tva.setTaux(18.0);
        tva.setBase(totalHT);
        tva.setMontant(totalTVA);
        totaux.setTva(tva);

        // TTC
        totaux.setTtc(totalTTC);
        totaux.setModePaiement(modePaiement);
        facture.setTotauxPayload(totaux);

        // --- Numéro de facture (comme dans le controller) ---
        facture.setNumeroFacture("ZINO_" + System.currentTimeMillis() + "_" + modePaiement.replace(" ", "_"));

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