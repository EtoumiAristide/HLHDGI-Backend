package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFactureDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GenericInvoiceExtractor {
    private static final List<String> MOTS_IGNORES = Arrays.asList(
            "Deloitte", "FACTURE", "Réf", "Montant", "Total", "Tva", "TVA",
            "Abidjan", "Attention", "l'attention", "Modalité", "Banque",
            "RIB", "SWIFT", "Code", "Guichet", "Compte", "La présente"
    );

    public DeloitteFactureDTO extractInvoiceData(String text, boolean isFromOCR) {
        DeloitteFactureDTO invoice = new DeloitteFactureDTO();

        // 1. Nettoyer le texte selon la source
        String cleanedText = cleanText(text, isFromOCR);
        String[] lines = cleanedText.split("\n");

        // 2. Extraire les données structurées
        extractStructuredData(lines, invoice);

        // 3. Compléter avec l'analyse de contexte
        extractContextualData(lines, invoice);

        // 4. Valider et corriger les données
        validateAndCorrectData(invoice);

        return invoice;
    }

    private String cleanText(String text, boolean isFromOCR) {
        String cleaned = text;

        if (isFromOCR) {
            // Corrections spécifiques OCR
            cleaned = cleaned.replaceAll("lvolre", "d'Ivoire")
                    .replaceAll("DeIOItte", "Deloitte")
                    .replaceAll("°lo", "%")
                    .replaceAll("'", "'")
                    .replaceAll("‘", "'")
                    .replaceAll("’", "'");

            // Corriger les O/0 dans les nombres
            cleaned = cleaned.replaceAll("(?<=\\d) O(?=\\d)", "0")
                    .replaceAll("(?<=\\s)O(?=\\d)", "0");
        }

        // Normalisation générale
        cleaned = cleaned.replaceAll("\\s+", " ")
                .replaceAll("\\.\\.+", ".")
                .replaceAll(",,+", ",")
                .trim();

        return cleaned;
    }

    private void extractStructuredData(String[] lines, DeloitteFactureDTO invoice) {
        // Patterns pour les données structurées
        Map<Pattern, DataExtractor> extractors = new LinkedHashMap<>();

        extractors.put(
                Pattern.compile("FACTURE\\s*N[°\\s]*(\\d+\\s*/\\s*\\d+)"),
                (matcher, inv) -> inv.setNumeroFacture(matcher.group(1).replaceAll("\\s+", " "))
        );

        extractors.put(
                Pattern.compile("Abidjan,\\s*le\\s*(\\d{1,2}\\s+\\p{L}+\\s+\\d{4})"),
                (matcher, inv) -> inv.setDateFacture(matcher.group(1))
        );

        extractors.put(
                Pattern.compile("Réf\\s*int[\\s:]*([\\w\\s/._-]+)"),
                (matcher, inv) -> inv.setReferenceInterne(matcher.group(1).trim())
        );

        extractors.put(
                Pattern.compile("Montant\\s+en\\s+(XOF|EUR|\\w{3})"),
                (matcher, inv) -> inv.setDevise(matcher.group(1))
        );

        extractors.put(
                Pattern.compile("A\\s*l'attention\\s*de[\\s:]*([^\\n]+)"),
                (matcher, inv) -> inv.setContactClient(matcher.group(1).trim())
        );

        extractors.put(
                Pattern.compile("To\\s*the\\s*attention\\s*of[\\s:]*([^\\n]+)"),
                (matcher, inv) -> inv.setContactClient(matcher.group(1).trim())
        );

        // Appliquer tous les extracteurs
        String fullText = String.join("\n", lines);
        for (Map.Entry<Pattern, DataExtractor> entry : extractors.entrySet()) {
            Matcher matcher = entry.getKey().matcher(fullText);
            if (matcher.find()) {
                entry.getValue().extract(matcher, invoice);
            }
        }

        // Extraire les montants avec une logique spécifique
        extractAmounts(lines, invoice);
    }

    private void extractAmounts(String[] lines, DeloitteFactureDTO invoice) {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal tva = BigDecimal.ZERO;
        BigDecimal ttc = BigDecimal.ZERO;
        String tauxTVA = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Total HT
            if (line.matches(".*Total\\s+H\\.?T\\.?.*")) {
                ht = extractAmountFromLine(line);
                if (ht.equals(BigDecimal.ZERO) && i + 1 < lines.length) {
                    ht = extractAmountFromLine(lines[i + 1]);
                }
            }

            // TVA
            if (line.matches(".*Tva.*|.*TVA.*")) {
                // Extraire le taux
                Pattern tauxPattern = Pattern.compile("(\\d+)\\s*%");
                Matcher tauxMatcher = tauxPattern.matcher(line);
                if (tauxMatcher.find()) {
                    tauxTVA = tauxMatcher.group(1) + "%";
                } else if (line.contains("non facturée")) {
                    tauxTVA = "non facturée";
                } else if (line.contains("suspendue")) {
                    tauxTVA = "suspendue";
                }

                // Extraire le montant
                tva = extractAmountFromLine(line);
            }

            // Total TTC
            if (line.matches(".*Total\\s+TTC.*")) {
                ttc = extractAmountFromLine(line);
                if (ttc.equals(BigDecimal.ZERO) && i + 1 < lines.length) {
                    ttc = extractAmountFromLine(lines[i + 1]);
                }
            }
        }

        invoice.setMontantHT(ht);
        invoice.setMontantTVA(tva);
        invoice.setTauxTVA(tauxTVA);
        invoice.setMontantTTC(ttc);

        // Si TTC manquant mais HT et TVA présents
        if (ttc.equals(BigDecimal.ZERO) && !ht.equals(BigDecimal.ZERO)) {
            ttc = ht.add(tva);
            invoice.setMontantTTC(ttc);
        }
    }

    private void extractContextualData(String[] lines, DeloitteFactureDTO invoice) {
        // 1. Extraire le nom du client (logique générique)
        invoice.setNomClient(extractClientName(lines));

        // 2. Extraire les items
        invoice.setItems(extractInvoiceItems(lines));

        // 3. Extraire l'adresse du client
        invoice.setAdresseClient(extractClientAddress(lines, invoice.getNomClient()));

        // 4. Extraire le bon de commande
        invoice.setBonDeCommande(extractPurchaseOrder(lines));
    }

    private String extractClientName(String[] lines) {
        // Stratégie 1: Chercher après la date
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Abidjan, le")) {
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    String candidate = lines[j].trim();
                    if (isValidClientName(candidate)) {
                        return cleanClientName(candidate);
                    }
                }
            }
        }

        // Stratégie 2: Chercher des lignes significatives avant le numéro de facture
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("FACTURE N°")) {
                for (int j = i - 1; j >= Math.max(0, i - 5); j--) {
                    String candidate = lines[j].trim();
                    if (!candidate.isEmpty() && isValidClientName(candidate)) {
                        return cleanClientName(candidate);
                    }
                }
            }
        }

        // Stratégie 3: Chercher des patterns typiques de noms d'entreprise
        Pattern companyPattern = Pattern.compile(
                "^(?!(?:Deloitte|FACTURE|Abidjan)).*" +
                        "(?:SA|SAS|SARL|SOCIETE|LIMITED|INC|GMBH|LTD|GROUP).*$",
                Pattern.CASE_INSENSITIVE
        );

        for (String line : lines) {
            String candidate = line.trim();
            if (!candidate.isEmpty() &&
                    companyPattern.matcher(candidate).matches() &&
                    !containsIgnoredWords(candidate)) {
                return cleanClientName(candidate);
            }
        }

        return null;
    }

    private boolean isValidClientName(String candidate) {
        if (candidate.length() < 3) {
            return false;
        }

        // Ne pas prendre les lignes avec des mots ignorés
        if (containsIgnoredWords(candidate)) {
            return false;
        }

        // Ne pas prendre les lignes qui sont clairement des adresses
        if (candidate.matches(".*\\d{5,}.*") || // Contient un code postal long
                candidate.matches(".*BP\\s*\\d+.*") || // Contient BP
                candidate.matches(".*Avenue.*|.*Rue.*|.*Boulevard.*") || // Contient des types de voie
                candidate.matches(".*Abidjan.*|.*Plateau.*|.*Cocody.*")) { // Contient des noms de quartier
            return false;
        }

        // Le nom doit contenir au moins une lettre
        return candidate.matches(".*[a-zA-Z].*");
    }

    private String cleanClientName(String name) {
        // Supprimer les numéros de référence qui pourraient être collés
        name = name.replaceAll("\\d{6,}", "") // Supprimer les longs numéros
                .replaceAll("NEANT", "")   // Supprimer "NEANT"
                .replaceAll("\\s+", " ")   // Normaliser les espaces
                .trim();

        return name.isEmpty() ? null : name;
    }

    private boolean containsIgnoredWords(String text) {
        String lowerText = text.toLowerCase();
        for (String word : MOTS_IGNORES) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<DeloitteFactureDTO.ItemFacture> extractInvoiceItems(String[] lines) {
        List<DeloitteFactureDTO.ItemFacture> items = new ArrayList<>();
        StringBuilder currentDescription = new StringBuilder();
        boolean inItemsSection = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Début de la section des items
            if (!inItemsSection &&
                    (line.contains("Honoraires") ||
                            line.contains("Fees") ||
                            line.contains("Débours") ||
                            (line.matches(".*[a-zA-Z].*") &&
                                    !line.contains("FACTURE") &&
                                    !line.contains("Total") &&
                                    !line.contains("Tva") &&
                                    line.length() > 10))) {
                inItemsSection = true;
                currentDescription.setLength(0);
            }

            // Dans la section des items
            if (inItemsSection) {
                // Fin de la section
                if (line.contains("Total H.T.") ||
                        line.contains("Total TTC") ||
                        (i > 0 && lines[i - 1].contains("Montant en") && line.matches(".*\\d.*"))) {
                    break;
                }

                // Ligne avec un montant
                BigDecimal amount = extractAmountFromLine(line);
                if (!amount.equals(BigDecimal.ZERO)) {
                    // C'est probablement la fin d'un item
                    if (currentDescription.length() > 0) {
                        DeloitteFactureDTO.ItemFacture item = new DeloitteFactureDTO.ItemFacture();
                        item.setDescription(currentDescription.toString().trim());
                        item.setMontant(amount);
                        items.add(item);
                        currentDescription.setLength(0);
                    }
                } else {
                    // Ajouter à la description
                    if (!line.isEmpty() && !line.matches("^\\d+$")) {
                        currentDescription.append(line).append(" ");
                    }
                }
            }
        }

        return items;
    }

    private String extractClientAddress(String[] lines, String clientName) {
        if (clientName == null) return null;

        StringBuilder address = new StringBuilder();
        boolean foundClient = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (!foundClient && line.contains(clientName)) {
                foundClient = true;
                continue;
            }

            if (foundClient) {
                // L'adresse suit généralement le nom du client
                if (line.matches(".*\\d+.*") || // Contient des chiffres
                        line.matches(".*[A-Z]{2,}.*") || // Contient des majuscules
                        line.contains("BP") ||
                        line.contains("Avenue") ||
                        line.contains("Rue") ||
                        line.contains("Zone") ||
                        line.contains("Abidjan") ||
                        line.contains("COte") ||
                        line.contains("IVOIRE")) {

                    address.append(line).append(" ");

                    // Arrêter quand on trouve une ligne vide ou le début d'autre chose
                    if (i + 1 < lines.length &&
                            (lines[i + 1].trim().isEmpty() ||
                                    lines[i + 1].contains("FACTURE"))) {
                        break;
                    }
                } else if (address.length() > 0) {
                    // On a déjà une adresse et on rencontre une ligne qui n'en fait pas partie
                    break;
                }
            }
        }

        return address.toString().trim();
    }

    private String extractPurchaseOrder(String[] lines) {
        for (String line : lines) {
            if (line.matches(".*Bon de commande.*|.*Purchase order.*|.*Commande.*")) {
                Pattern pattern = Pattern.compile("\\d{5,}");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }
        return null;
    }

    private BigDecimal extractAmountFromLine(String line) {
        // Chercher des montants avec formats variés
        Pattern amountPattern = Pattern.compile(
                "([\\d\\s.,]+(?:[.,]\\d{2})?)(?:\\s*(?:XOF|EUR|€|\\$))?"
        );

        Matcher matcher = amountPattern.matcher(line);
        while (matcher.find()) {
            String amountStr = matcher.group(1)
                    .replaceAll("\\s", "")
                    .replaceAll("\\.", "")
                    .replace(",", ".");

            try {
                BigDecimal amount = new BigDecimal(amountStr);
                // Valider que c'est un montant raisonnable (pas un code, etc.)
                if (amount.compareTo(new BigDecimal("10")) >= 0 &&
                        amount.compareTo(new BigDecimal("1000000000")) <= 0) {
                    return amount;
                }
            } catch (Exception e) {
                continue;
            }
        }

        return BigDecimal.ZERO;
    }

    private void validateAndCorrectData(DeloitteFactureDTO invoice) {
        // S'assurer que la devise est définie
        if (invoice.getDevise() == null) {
            // Déduire de la localisation ou des montants
            if (invoice.getMontantHT().compareTo(new BigDecimal("10000")) > 0) {
                invoice.setDevise("XOF"); // Grands montants typiques en XOF
            } else {
                invoice.setDevise("EUR"); // Petits montants typiques en EUR
            }
        }

        // Si TTC manquant mais HT et TVA présents, calculer
        if (invoice.getMontantTTC().equals(BigDecimal.ZERO) &&
                !invoice.getMontantHT().equals(BigDecimal.ZERO)) {
            BigDecimal ttc = invoice.getMontantHT().add(invoice.getMontantTVA());
            invoice.setMontantTTC(ttc);
        }

        // Nettoyer les descriptions d'items
        if (invoice.getItems() != null) {
            for (DeloitteFactureDTO.ItemFacture item : invoice.getItems()) {
                if (item.getDescription() != null) {
                    item.setDescription(item.getDescription()
                            .replaceAll("\\d+[.,]\\d{2}", "") // Enlever les montants dans la description
                            .replaceAll("\\s+", " ")
                            .trim());
                }
            }
        }
    }

    // Interface pour les extracteurs de données
    @FunctionalInterface
    private interface DataExtractor {
        void extract(Matcher matcher, DeloitteFactureDTO invoice);
    }

    // Méthode principale d'extraction
    public DeloitteFactureDTO extractFromText(String text, boolean isFromOCR) {
        try {
            DeloitteFactureDTO invoice = extractInvoiceData(text, isFromOCR);

            return invoice;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
