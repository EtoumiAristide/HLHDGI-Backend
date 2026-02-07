package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFactureDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeloittePDFExtractor3 {

    @Autowired
    private IOCRService ocrService;

    public DeloitteFactureDTO extraireDonneesFacture(byte[] pdfBytes) {
        try {
            // Étape 1: Extraire le texte
            String texte = extraireTexte(pdfBytes);
            System.out.println("texte " + texte);

            if (texte.trim().isEmpty()) {
                return null;
            }

            // Debug
//            System.out.println("=== TEXTE EXTRAIT ===");
//            System.out.println(texte.substring(0, Math.min(1000, texte.length())));

            // Étape 2: Analyser spécifiquement pour les factures Deloitte

            // Données brutes
            return analyserFactureDeloitte(texte);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extraireTexte(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String texte = stripper.getText(document);

            // Si peu de texte, essayer OCR
            if (texte.trim().length() < 100) {
                System.out.println("Utilisation de l'OCR...");
                return ocrService.extraireTexteDepuisPDF(pdfBytes);
            }

            return texte;
        }
    }

    private DeloitteFactureDTO analyserFactureDeloitte(String texte) {
        DeloitteFactureDTO facture = new DeloitteFactureDTO();

        // 1. Nom du client (à droite de la date)
        facture.setNomClient(extraireNomClient(texte));
        System.out.println("Nom client: " + facture.getNomClient());

        // 2. Numéro de facture
        facture.setNumeroFacture(extraireNumeroFacture(texte));
        System.out.println("Numéro facture: " + facture.getNumeroFacture());

        // 3. Date de facture
        facture.setDateFacture(extraireDateFacture(texte));
        System.out.println("Date facture: " + facture.getDateFacture());

        // 4. Référence interne
        facture.setReferenceInterne(extraireReferenceInterne(texte));
        System.out.println("Référence interne: " + facture.getReferenceInterne());

        // 5. Adresse du client
        facture.setAdresseClient(extraireAdresseClient(texte));

        // 6. Contact client
        facture.setContactClient(extraireContactClient(texte));

        // 7. Bon de commande
        //facture.setBonDeCommande(extraireBonDeCommande(texte));

        // 8. Items de la facture (description + montant)
        facture.setItems(extraireItemsFacture(texte));
        System.out.println("Nombre d'items: " + facture.getItems().size());

        // 9. Montants HT, TVA, TTC et devise
        Map<String, BigDecimal> montants = extraireMontantsFactureLigne(texte);
        /*Map<String, Object> montants = extraireMontantsFacture(texte);
        facture.setMontantHT((BigDecimal) montants.get("ht"));
        facture.setMontantTVA((BigDecimal) montants.get("tva"));
        facture.setTauxTVA((String) montants.get("tauxTVA"));
        facture.setMontantTTC((BigDecimal) montants.get("ttc"));
        facture.setDevise((String) montants.get("devise"));

        System.out.println("Montants - HT: " + facture.getMontantHT() +
                ", TVA: " + facture.getMontantTVA() +
                ", TTC: " + facture.getMontantTTC() +
                ", Devise: " + facture.getDevise());*/

        return facture;
    }

    private String extraireNomClient(String texte) {
        // Le nom du client est généralement sur la même ligne que la date
        // Format: "Abidjan, le 16 mai 2025" + "ALSTOM METRO D'ABIDJAN"

        String[] lignes = texte.split("\n");
        boolean dateTrouvee = false;
        //System.out.println("Lignes: " + Arrays.toString(lignes));
        String client = "";
        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();
            //System.out.println("ligneClient " + ligne);

            // Chercher la ligne avec la date
            if (ligne.matches(".*Abidjan, le.*\\d{1,2}.*[a-zA-Z]+.*\\d{4}.*")) {

                client = ligne.replaceAll(".*le\\s+\\d{1,2}\\s+\\p{L}+\\s+\\d{4}", "").trim();
                //System.out.println("ligneClient recup " + ligne);

                dateTrouvee = true;

                // Regarder les lignes suivantes pour le nom du client
                /*for (int j = i + 1; j < Math.min(i + 5, lignes.length); j++) {
                    String ligneClient = lignes[j].trim();

                    // Le nom du client est généralement en majuscules et ne contient pas certains mots
                    if (!ligneClient.isEmpty() &&
                            !ligneClient.contains("FACTURE") &&
                            !ligneClient.contains("Réf") &&
                            !ligneClient.contains("Montant") &&
                            !ligneClient.contains("Total") &&
                            (ligneClient.toUpperCase().equals(ligneClient) ||
                                    ligneClient.matches(".*[A-Z].*"))) {

                        // Nettoyer le nom
                        return ligneClient.replaceAll("\\d{5,}", "") // Enlever codes postaux
                                .replaceAll("BP.*", "")    // Enlever BP
                                .replaceAll("\\s+", " ")  // Espaces multiples
                                .trim();
                    }
                }*/
            }
            if (dateTrouvee) break;

            // Alternative: Chercher directement le nom en majuscules après "Abidjan, le"
            /*if (ligne.contains("Abidjan, le")) {
                for (int j = i + 1; j < Math.min(i + 10, lignes.length); j++) {
                    String ligneSuivante = lignes[j].trim();
                    if (ligneSuivante.length() > 5 &&
                            ligneSuivante.matches(".*[A-Z].*") &&
                            !ligneSuivante.contains("FACTURE") &&
                            !ligneSuivante.contains("Tél") &&
                            !ligneSuivante.contains("Fax")) {
                        return ligneSuivante;
                    }
                }
            }*/
        }

        return client;
    }

    private String extraireNumeroFacture(String texte) {
        Pattern pattern = Pattern.compile("FACTURE\\s*N[°\\s]*(\\d+\\s*/\\s*\\d+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ");
        }
        return null;
    }

    private String extraireDateFacture(String texte) {
        Pattern pattern = Pattern.compile("Abidjan, le\\s*(\\d{1,2}\\s+[a-zA-Zéèêëàâäôöùûüç]+\\s+\\d{4})");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extraireReferenceInterne(String texte) {
        Pattern pattern = Pattern.compile("Réf\\s*lnt[\\s:.]*([\\w\\s/._-]+?)(?=\\s+Montant)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("\\s+", " "); // normalisation des espaces;
        }
        return null;
    }

    private String extraireAdresseClient(String texte) {
        String[] lignes = texte.split("\n");
        boolean nomClientTrouve = false;
        StringBuilder adresse = new StringBuilder();

        for (String ligne : lignes) {
            ligne = ligne.trim();

            if (nomClientTrouve && !ligne.isEmpty()) {
                // L'adresse suit le nom du client
                if (ligne.contains("BP") || ligne.contains("Avenue") ||
                        ligne.contains("Rue") || ligne.contains("Zone") ||
                        ligne.matches(".*\\d{5}.*")) {
                    adresse.append(ligne).append(" ");
                } else if (adresse.length() > 0) {
                    break;
                }
            }

            // Détecter le nom du client
            if (!nomClientTrouve && ligne.length() > 5 &&
                    ligne.matches(".*[A-Z].*") &&
                    !ligne.contains("FACTURE") &&
                    !ligne.contains("Deloitte") &&
                    !ligne.contains("Abidjan, le")) {
                nomClientTrouve = true;
            }
        }

        return adresse.toString().trim();
    }

    private String extraireContactClient(String texte) {
        Pattern pattern = Pattern.compile(
                "(?:A l'attention de|To the attention of|A l'attention|Contact)[\\s:]*([^\\n]+)"
        );
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extraireBonDeCommande(String texte) {
        Pattern pattern = Pattern.compile("Bon de commande[\\s:]*([\\d]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private List<DeloitteFactureDTO.ItemFacture> extraireItemsFacture(String texte) {
        List<DeloitteFactureDTO.ItemFacture> items = new ArrayList<>();
        /*String[] lignes = texte.split("\n");

        // Chercher la section des items (entre la référence et "Total H.T.")
        boolean dansSectionItems = false;

        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();

            // Détecter début de section (après référence ou montant en devise)
            if (ligne.contains("Réf int") ||
                    (ligne.contains("Montant en") && i + 1 < lignes.length)) {
                dansSectionItems = true;
                continue;
            }

            // Détecter fin de section
            if (ligne.contains("Total H.T.") ||
                    ligne.contains("Total TTC") ||
                    ligne.contains("Tva")) {
                dansSectionItems = false;
            }

            // Extraire les items
            if (dansSectionItems && !ligne.isEmpty()) {
                // Chercher les lignes avec description et éventuellement montant
                if (ligne.matches(".*[a-zA-Z].*")) { // Contient du texte
                    String description = ligne;
                    BigDecimal montant = null;

                    // Chercher un montant sur la même ligne
                    Pattern montantPattern = Pattern.compile("([\\d\\s,]+(?:[.,]\\d{2})?)");
                    Matcher montantMatcher = montantPattern.matcher(ligne);
                    if (montantMatcher.find()) {
                        String montantStr = montantMatcher.group(1)
                                .replaceAll("\\s+", "")
                                .replace(",", ".");
                        System.out.println("montantStr " + montantStr);
                        montant = new BigDecimal(montantStr);

                        // Retirer le montant de la description
                        description = ligne.replaceAll("[\\d\\s,.]", "").trim();
                    }

                    // Si pas de montant, chercher sur la ligne suivante
                    else if (i + 1 < lignes.length) {
                        String ligneSuivante = lignes[i + 1].trim();
                        montantMatcher = montantPattern.matcher(ligneSuivante);
                        if (montantMatcher.find()) {
                            String montantStr = montantMatcher.group(1)
                                    .replaceAll("\\s+", "")
                                    .replace(",", ".");
                            montant = new BigDecimal(montantStr);
                        }
                    }

                    if (montant != null && !description.isEmpty()) {
                        DeloitteFactureDTO.ItemFacture item = new DeloitteFactureDTO.ItemFacture();
                        item.setDescription(description);
                        item.setMontant(montant);
                        // La devise sera ajoutée plus tard
                        items.add(item);
                    }
                }
            }
        }*/

        // 1️⃣ Extraire le bloc des items
        Pattern blockPattern = Pattern.compile(
                "Montant[\\s\\S]*?(?=Total\\s+H\\.T\\.)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher blockMatcher = blockPattern.matcher(texte);

        if (!blockMatcher.find()) {
            System.out.println("Bloc Montant -> Total H.T. introuvable");
            return null;
        }

        String itemsBlock = blockMatcher.group();

        // 2️⃣ Regex ligne + montant
        Pattern lineWithAmountPattern = Pattern.compile(
                "(.+?)\\s+(\\d{1,3}(?:[\\s.,]\\d{3})*(?:[.,]\\d{2})?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher lineMatcher = lineWithAmountPattern.matcher(itemsBlock);

        while (lineMatcher.find()) {
            String libelle = lineMatcher.group(1)
                    .trim()
                    .replaceAll("\\s+", " ");

            String montant = lineMatcher.group(2)
                    .replaceAll("\\s+", " ");
//            System.out.println("montant "+montant);
            DeloitteFactureDTO.ItemFacture item = new DeloitteFactureDTO.ItemFacture();
            item.setDescription(libelle);
            item.setMontant(parseAmount(montant));
            // La devise sera ajoutée plus tard
            items.add(item);
        }

        // 3️⃣ Affichage
        items.forEach(System.out::println);

        return items;
    }

    private Map<String, Object> extraireMontantsFacture(String texte) {
        Map<String, Object> result = new HashMap<>();
        result.put("ht", BigDecimal.ZERO);
        result.put("tva", BigDecimal.ZERO);
        result.put("ttc", BigDecimal.ZERO);
        result.put("devise", "XOF");
        result.put("tauxTVA", "");

        // Détecter devise
        if (texte.contains("Montant en EUR") || texte.contains("EUR")) {
            result.put("devise", "EUR");
        }

        String[] lignes = texte.split("\n");

        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();

            // Total HT
            if (ligne.contains("Total H.T.") && i + 1 < lignes.length) {
                String montantHT = extraireMontantLigne(lignes[i + 1]);
                System.out.println("montantHT: " + montantHT);
                if (!montantHT.isEmpty()) {
                    result.put("ht", new BigDecimal(montantHT));
                }
            }

            // TVA
            if (ligne.contains("Tva")) {
                // Extraire le type de TVA
                if (ligne.contains("non facturée")) {
                    result.put("tauxTVA", "non facturée");
                } else if (ligne.contains("suspendue")) {
                    result.put("tauxTVA", "suspendue");
                } else if (ligne.contains("18%")) {
                    result.put("tauxTVA", "18%");
                }

                // Extraire le montant TVA
                String montantTVA = extraireMontantLigne(ligne);
                if (!montantTVA.isEmpty()) {
                    result.put("tva", new BigDecimal(montantTVA));
                }
            }

            // Total TTC
            if (ligne.contains("Total TTC") && i + 1 < lignes.length) {
                String montantTTC = extraireMontantLigne(lignes[i + 1]);
                if (!montantTTC.isEmpty()) {
                    result.put("ttc", new BigDecimal(montantTTC));
                }
            }
        }

        return result;
    }

    private String extraireMontantLigne(String ligne) {
        Pattern pattern = Pattern.compile("([\\d\\s,]+(?:[.,]\\d{2})?)\\s*(?:XOF|EUR|€)?");
        Matcher matcher = pattern.matcher(ligne);
        if (matcher.find()) {
            return matcher.group(1)
                    .replaceAll("\\s+", "")
                    .replace(",", ".");
        }
        return "";
    }

    private Map<String, BigDecimal> extraireMontantsFactureLigne(String texte) {
        Map<String, BigDecimal> result = new HashMap<>();

        // 1️⃣ Extraire le bloc Total H.T. -> Total TTC
        Pattern blockPattern = Pattern.compile(
                "Total\\s+H\\.T\\.[\\s\\S]*?Total\\s+TTC[\\s\\S]*?$",
                Pattern.CASE_INSENSITIVE
        );

        Matcher blockMatcher = blockPattern.matcher(texte);

        if (!blockMatcher.find()) {
            System.out.println("Bloc Totaux introuvable");
            return null;
        }

        String totalsBlock = blockMatcher.group();

        // 2️⃣ Extraire lignes alignées avec montants
        Pattern linePattern = Pattern.compile(
                "(?m)^(.+?)\\s{5,}((?:\\d{1,3}(?:[\\s.,]\\d{3})+(?:[.,]\\d{2})?)|(?:\\d+[.,]\\d{2}))"
        );

        Matcher matcher = linePattern.matcher(totalsBlock);

        while (matcher.find()) {

            String label = matcher.group(1)
                    .trim()
                    .replaceAll("\\s+", " ");

            String rawAmount = matcher.group(2);

            BigDecimal amount = parseAmount(rawAmount);

            result.put(label, amount);
        }

        // 3️⃣ Résultat
        result.forEach((k, v) ->
                System.out.println(k + " = " + v)
        );

        return result;
    }

    public DeloitteFactureDTO extraireDonneesFacture(String cheminFichier) {
        try {
            File file = new File(cheminFichier);
            byte[] pdfBytes = java.nio.file.Files.readAllBytes(file.toPath());
            return extraireDonneesFacture(pdfBytes);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean testerOCR() {
        return ocrService != null;
    }

    private BigDecimal parseAmount(String rawAmount) {

        if (rawAmount == null || rawAmount.isBlank()) {
            return BigDecimal.ZERO;
        }

        // 1️⃣ Supprimer devise et caractères non numériques utiles
        String cleaned = rawAmount
                .replaceAll("[^0-9,\\.]", "");

        // 2️⃣ Cas virgule décimale (français)
        if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "");
            cleaned = cleaned.replace(",", ".");
        }

        // 3️⃣ Supprimer séparateurs de milliers
        cleaned = cleaned.replaceAll("(?<=\\d)[\\s\\.](?=\\d{3})", "");

        return new BigDecimal(cleaned);
    }
}
