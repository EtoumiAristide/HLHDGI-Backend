package com.elpandor.hlh.modules.automatisation.service;

import com.elpandor.hlh.modules.automatisation.model.zino.Detail;
import com.elpandor.hlh.modules.automatisation.model.zino.TicketVente;
import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketToFactureTransformer {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Transforme un TicketVente en FacturePayload
     */
    public FacturePayload transform(TicketVente ticket) {
        if (ticket == null) {
            return null;
        }

        FacturePayload facture = new FacturePayload();

        // Informations générales
        facture.setNumeroFacture(genererNumeroFacture(ticket));
        facture.setDateFacture(formatDate(ticket.getDate()));
        facture.setTypeFacture(TypeFacture.FACTURE_VENTE);
        facture.setTypeClient(determinerTypeClient(ticket));
        facture.setModePaiement(determinerModePaiement(ticket));
        facture.setPointVente("Y2CI"); // Vous pouvez le rendre configurable
        facture.setEntreprise("HLH");
        facture.setReception("vente");
        facture.setSheetName("TICKETVENTE");

        // Client
        facture.setClientPayload(creerClientPayload(ticket));

        // Lignes de produits
        facture.setLignes(creerLignesProduit(ticket));

        // Totaux
        facture.setTotauxPayload(calculerTotaux(ticket));

        // Taxes
        calculerTaxes(facture);

        return facture;
    }

    /**
     * Transforme une liste de TicketVente en liste de FacturePayload
     */
    public List<FacturePayload> transformAll(List<TicketVente> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return new ArrayList<>();
        }
        return tickets.stream()
                .map(this::transform)
                .collect(Collectors.toList());
    }

    /**
     * Crée le payload client
     */
    private ClientPayload creerClientPayload(TicketVente ticket) {
        ClientPayload client = new ClientPayload();

        // Construction du nom complet
        String nom = ticket.getClient();
        if (ticket.getNomClient() != null && !ticket.getNomClient().isEmpty()) {
            nom = ticket.getNomClient();
            if (ticket.getPrenomClient() != null && !ticket.getPrenomClient().isEmpty()) {
                nom = ticket.getPrenomClient() + " " + nom;
            }
        }
        client.setNom(nom);

        // Numéro de carte/identifiant client
        client.setNumeroCC(ticket.getCodeClient() != null ? ticket.getCodeClient() : "");

        return client;
    }

    /**
     * Crée les lignes de produits à partir des détails
     */
    private List<LigneProduitPayload> creerLignesProduit(TicketVente ticket) {
        List<LigneProduitPayload> lignes = new ArrayList<>();

        for (Detail detail : ticket.getDetails()) {
            LigneProduitPayload ligne = new LigneProduitPayload();

            ligne.setDate(formatDate(ticket.getDate()));
            ligne.setProduit(detail.getDescription() + " (" + detail.getReference() + ")");
            ligne.setQuantite(detail.getQuantite().intValue());
            ligne.setUnite("U"); // Unité standard

            // Prix unitaire HT (hors taxe)
            double prixUnitaireHT = detail.getPrixUnitaire().doubleValue();
            ligne.setPrixUnitaireHT(prixUnitaireHT);

            // Montant HT = prix unitaire * quantité
            double montantHT = detail.getMontantTotal().doubleValue();
            ligne.setMontantHT(montantHT);

            lignes.add(ligne);
        }

        return lignes;
    }

    /**
     * Calcule les totaux de la facture
     */
    private TotauxPayload calculerTotaux(TicketVente ticket) {
        TotauxPayload totaux = new TotauxPayload();

        // Calcul du HT (somme des montants HT de chaque ligne)
        double totalHT = ticket.getDetails().stream()
                .mapToDouble(d -> d.getMontantTotal().doubleValue())
                .sum();
        totaux.setHt(totalHT);

        // Le TTC = montant total payé
        double ttc = ticket.getPaiement() != null ?
                ticket.getPaiement().getMontant().doubleValue() :
                totalHT;
        totaux.setTtc(ttc);

        // Mode de paiement
        if (ticket.getPaiement() != null) {
            totaux.setModePaiement(ticket.getPaiement().getMoyenPaiement());
        }

        return totaux;
    }

    /**
     * Calcule les taxes (TVA, TDT, TCN) avec les bonnes bases
     * TVA = 18% sur le HT
     * TDT = 0.5% sur le TTC
     * TCN = 0.2% sur le TTC
     */
    private void calculerTaxes(FacturePayload facture) {
        TotauxPayload totaux = facture.getTotauxPayload();
        if (totaux == null) {
            return;
        }

        double ht = totaux.getHt();
        double ttc = totaux.getTtc();

        // --- TVA (18%) ---
        TaxePayload tva = new TaxePayload();
        tva.setTaux(18.0);
        tva.setBase(ht); // Base HT
        tva.setMontant(ht * 0.18);
        totaux.setTva(tva);

        // --- TDT (0.5%) ---
        TaxePayload tdt = new TaxePayload();
        tdt.setBase(ttc); // Base TTC
        tdt.setMontant(ttc * 0.005);
        // Pas de taux pour TDT (car vous avez dit que c'est optionnel)
        totaux.setTdt(tdt);

        // --- TCN (0.2%) ---
        TaxePayload tcn = new TaxePayload();
        tcn.setTaux(0.2);
        tcn.setBase(ttc); // Base TTC
        tcn.setMontant(ttc * 0.002);
        totaux.setTcn(tcn);

        // Mise à jour du TTC = HT + TVA + TDT + TCN
        double ttcCalcule = ht +
                (ht * 0.18) +
                (ttc * 0.005) +
                (ttc * 0.002);
        // On garde le TTC original qui vient du paiement
        // mais on peut le recalculer si besoin
        // totaux.setTtc(ttcCalcule); // Optionnel
    }

    /**
     * Détermine le type de client
     */
    private TypeClient determinerTypeClient(TicketVente ticket) {
        // Logique par défaut - à adapter selon vos règles métier
        String codeClient = ticket.getCodeClient();
        if (codeClient != null) {
            if (codeClient.startsWith("305-")) {
                return TypeClient.B2C;
            } else if (codeClient.matches("\\d+")) {
                return TypeClient.B2B;
            }
        }
        return TypeClient.B2C; // Par défaut
    }

    /**
     * Détermine le mode de paiement à partir des données du ticket
     */
    private ModePaiement determinerModePaiement(TicketVente ticket) {
        if (ticket.getPaiement() == null) {
            return ModePaiement.cash;
        }

        String moyenPaiement = ticket.getPaiement().getMoyenPaiement();
        if (moyenPaiement == null) {
            return ModePaiement.cash;
        }

        // Mapping des moyens de paiement du CSV vers les énumérations
        switch (moyenPaiement.toLowerCase()) {
            case "orange money":
            case "wave":
                return ModePaiement.mobilemoney;
            case "carte bancaire franc cfa":
            case "carte bancaire":
                return ModePaiement.card;
            case "espèce":
            case "espece":
            case "cash":
                return ModePaiement.cash;
            case "chèque":
            case "cheque":
            case "check":
                return ModePaiement.check;
            case "virement":
            case "transfer":
                return ModePaiement.transfer;
            default:
                return ModePaiement.cash;
        }
    }

    /**
     * Génère un numéro de facture unique
     */
    private String genererNumeroFacture(TicketVente ticket) {
        // Format: FV-YYYYMMDD-XXXXX
        String dateStr = ticket.getDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ticketNum = ticket.getNumeroTicket() != null ? ticket.getNumeroTicket() : "00000";
        return "FV-" + dateStr + "-" + String.format("%05d", Integer.parseInt(ticketNum));
    }

    /**
     * Formate une date en String
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        return date.format(DATE_FORMATTER);
    }
}
