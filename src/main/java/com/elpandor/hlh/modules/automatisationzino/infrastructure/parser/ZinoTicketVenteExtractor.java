package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZinoTicketVenteExtractor {

    private static final String SEPARATEUR = ";";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public List<TicketVenteZino> extraire(InputStream is) throws IOException {
        List<TicketVenteZino> resultats = new ArrayList<>();
        List<String[]> lignesTicketCourant = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String ligne;
            boolean premiereLigneFichier = true;

            while ((ligne = reader.readLine()) != null) {
                if (premiereLigneFichier) {
                    if (!ligne.isEmpty() && ligne.charAt(0) == '\uFEFF') {
                        ligne = ligne.substring(1);
                    }
                    premiereLigneFichier = false;
                }

                if (ligne.isBlank()) {
                    continue;
                }

                String[] colonnes = ligne.split(SEPARATEUR, -1);
                if (colonnes.length < 10) {
                    continue;
                }

                String typeLigne = colonnes[0].trim();

                if ("CLIENT".equalsIgnoreCase(typeLigne)) {
                    if (!lignesTicketCourant.isEmpty()) {
                        resultats.add(construireTicket(lignesTicketCourant));
                        lignesTicketCourant.clear();
                    }
                }
                lignesTicketCourant.add(colonnes);
            }

            if (!lignesTicketCourant.isEmpty()) {
                resultats.add(construireTicket(lignesTicketCourant));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier de vente Zino", e);
        }

        return resultats;
    }

    private TicketVenteZino construireTicket(List<String[]> lignesTicket) {
        TicketVenteZino data = new TicketVenteZino();

        double totalMontantHT = 0.0;
        double totalTva = 0.0;
        double totalMontantTTC = 0.0;
        String modePaiement = null;
        boolean premiereLigneDetails = true;

        for (String[] colonnes : lignesTicket) {
            String typeLigne = colonnes[0].trim();

            switch (typeLigne.toUpperCase()) {

                case "CLIENT" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String nom = colonnes[2] != null ? colonnes[2].trim() : "";
                    String prenom = colonnes[3] != null ? colonnes[3].trim() : "";
                    data.setNomClient((nom + " " + prenom).trim());
                    try {
                        data.setNumTicket(Integer.parseInt(colonnes[8].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                    data.setNumCompteClient(colonnes[9] != null ? colonnes[9].trim() : null);
                }

                case "DETAILS" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String designation = colonnes[3] != null ? colonnes[3].trim() : "";
                    double montantHtLigne = parseDouble(colonnes[7]);
                    double tauxTva = parseDouble(colonnes[6]);
                    double tvaLigne = montantHtLigne * (tauxTva / 100.0);

                    totalMontantHT += montantHtLigne;
                    totalTva += tvaLigne;

                    if (premiereLigneDetails) {
                        String codeProduit = colonnes[2] != null ? colonnes[2].trim() : "";
                        data.setCodeProduitPrincipal(codeProduit);
                        data.setDesignationPrincipale(designation);
                        premiereLigneDetails = false;
                    }
                }

                case "PAIMENT" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String mode = colonnes[4] != null ? colonnes[4].trim() : "";
                    double montantTtcLigne = parseDouble(colonnes[5]);

                    totalMontantTTC += montantTtcLigne;
                    modePaiement = mode;
                }

                default -> {
                    // Ignorer les lignes inconnues
                }
            }
        }

        data.setModePaiement(modePaiement);
        data.setMontantHT(totalMontantHT);
        data.setTva(totalTva);
        data.setMontantTTC(totalMontantTTC);

        return data;
    }

    public List<RepartitionParModePaiement> calculerRepartitionParModePaiement(List<TicketVenteZino> tickets) {
        Map<String, RepartitionParModePaiement> repartitionMap = new HashMap<>();

        for (TicketVenteZino ticket : tickets) {
            if (ticket.getModePaiement() != null && ticket.getModePaiement().equalsIgnoreCase("glovo")) {
                ticket.setModePaiement("Espèces Franc CFA");
            }
            String modePaiement = ticket.getModePaiement();

            RepartitionParModePaiement dto = repartitionMap.getOrDefault(modePaiement, new RepartitionParModePaiement());
            dto.setDate(ticket.getDate());
            dto.setModePaiement(modePaiement);
            dto.setTotalMontantTTC(dto.getTotalMontantTTC() != null ? dto.getTotalMontantTTC() + ticket.getMontantTTC() : ticket.getMontantTTC());
            dto.setTotalMontantHT(dto.getTotalMontantHT() != null ? dto.getTotalMontantHT() + ticket.getMontantHT() : ticket.getMontantHT());
            dto.setTotalTVA(dto.getTotalTVA() != null ? dto.getTotalTVA() + ticket.getTva() : ticket.getTva());
            dto.setNombreTransactions(dto.getNombreTransactions() + 1);
            dto.setDesignationPrincipale(ticket.getDesignationPrincipale());

            repartitionMap.put(modePaiement, dto);
        }

        return new ArrayList<>(repartitionMap.values());
    }

    private Date parseDate(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(valeur.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private double parseDouble(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valeur.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
/*
package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZinoTicketVenteExtractor {

    private static final String SEPARATEUR = ";";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public List<TicketVenteZino> extraire(InputStream is) throws IOException {
        List<TicketVenteZino> resultats = new ArrayList<>();
        List<String[]> lignesTicketCourant = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String ligne;
            boolean premiereLigneFichier = true;

            while ((ligne = reader.readLine()) != null) {
                if (premiereLigneFichier) {
                    if (!ligne.isEmpty() && ligne.charAt(0) == '\uFEFF') {
                        ligne = ligne.substring(1);
                    }
                    premiereLigneFichier = false;
                }

                if (ligne.isBlank()) {
                    continue;
                }

                String[] colonnes = ligne.split(SEPARATEUR, -1);
                if (colonnes.length < 10) {
                    continue;
                }

                String typeLigne = colonnes[0].trim();

                if ("CLIENT".equalsIgnoreCase(typeLigne)) {
                    if (!lignesTicketCourant.isEmpty()) {
                        resultats.add(construireTicket(lignesTicketCourant));
                        lignesTicketCourant.clear();
                    }
                }
                lignesTicketCourant.add(colonnes);
            }

            if (!lignesTicketCourant.isEmpty()) {
                resultats.add(construireTicket(lignesTicketCourant));
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier de vente Zino", e);
        }

        return resultats;
    }

    private TicketVenteZino construireTicket(List<String[]> lignesTicket) {
        TicketVenteZino data = new TicketVenteZino();

        double totalMontantHT = 0.0;
        double totalTva = 0.0;
        double totalMontantTTC = 0.0;
        String modePaiement = null;
        boolean premiereLigneDetails = true;

        for (String[] colonnes : lignesTicket) {
            String typeLigne = colonnes[0].trim();

            switch (typeLigne.toUpperCase()) {

                case "CLIENT" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String nom = colonnes[2] != null ? colonnes[2].trim() : "";
                    String prenom = colonnes[3] != null ? colonnes[3].trim() : "";
                    data.setNomClient((nom + " " + prenom).trim());
                    try {
                        data.setNumTicket(Integer.parseInt(colonnes[8].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                    data.setNumCompteClient(colonnes[9] != null ? colonnes[9].trim() : null);
                }

                case "DETAILS" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String designation = colonnes[3] != null ? colonnes[3].trim() : "";
                    double montantHtLigne = parseDouble(colonnes[7]);
                    double tauxTva = parseDouble(colonnes[6]);
                    double tvaLigne = montantHtLigne * (tauxTva / 100.0);

                    totalMontantHT += montantHtLigne;
                    totalTva += tvaLigne;

                    if (premiereLigneDetails) {
                        String codeProduit = colonnes[2] != null ? colonnes[2].trim() : "";
                        data.setCodeProduitPrincipal(codeProduit);
                        data.setDesignationPrincipale(designation);
                        premiereLigneDetails = false;
                    }
                }

                case "PAIMENT" -> {
                    if (data.getDate() == null) {
                        data.setDate(parseDate(colonnes[1]));
                    }
                    String mode = colonnes[4] != null ? colonnes[4].trim() : "";
                    double montantTtcLigne = parseDouble(colonnes[5]);

                    totalMontantTTC += montantTtcLigne;
                    modePaiement = mode;
                }

                default -> {
                }
            }
        }

        data.setModePaiement(modePaiement);
        data.setMontantHT(totalMontantHT);
        data.setTva(totalTva);
        data.setMontantTTC(totalMontantTTC);

        return data;
    }

    public List<RepartitionParModePaiement> calculerRepartitionParModePaiement(List<TicketVenteZino> tickets) {
        Map<String, RepartitionParModePaiement> repartitionMap = new HashMap<>();

        for (TicketVenteZino ticket : tickets) {
            if (ticket.getModePaiement() != null && ticket.getModePaiement().equalsIgnoreCase("glovo")) {
                ticket.setModePaiement("Espèces Franc CFA");
            }
            String modePaiement = ticket.getModePaiement();

            RepartitionParModePaiement dto = repartitionMap.getOrDefault(modePaiement, new RepartitionParModePaiement());
            dto.setDate(ticket.getDate());
            dto.setModePaiement(modePaiement);
            dto.setTotalMontantTTC(dto.getTotalMontantTTC() != null ? dto.getTotalMontantTTC() + ticket.getMontantTTC() : ticket.getMontantTTC());
            dto.setTotalMontantHT(dto.getTotalMontantHT() != null ? dto.getTotalMontantHT() + ticket.getMontantHT() : ticket.getMontantHT());
            dto.setTotalTVA(dto.getTotalTVA() != null ? dto.getTotalTVA() + ticket.getTva() : ticket.getTva());
            dto.setNombreTransactions(dto.getNombreTransactions() + 1);
            dto.setDesignationPrincipale(ticket.getDesignationPrincipale());

            repartitionMap.put(modePaiement, dto);
        }

        return new ArrayList<>(repartitionMap.values());
    }

    private Date parseDate(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(valeur.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private double parseDouble(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valeur.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}*/
