package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFacturePayload;
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
public class DeloittePDFExtractor2 {
    @Autowired
    private IOCRService ocrService;

    public DeloitteFacturePayload extraireDonneesFacture(byte[] pdfBytes) {
        try {
            // Étape 1: Essayer d'extraire comme PDF texte
            String texte = extraireTexteDepuisPDFTexte(pdfBytes);
            boolean estScanne = texte.trim().length() < 100; // Si peu de texte, probablement scanné

            if (estScanne) {
                System.out.println("PDF détecté comme scanné, utilisation de l'OCR...");
                // Étape 2: Utiliser OCR
                texte = ocrService.extraireTexteDepuisPDF(pdfBytes);
                System.out.println("Texte OCR extrait (" + texte.length() + " caractères)");
            } else {
                System.out.println("PDF détecté comme texte, extraction directe");
                System.out.println("Texte extrait (" + texte.length() + " caractères)");
            }

            // Debug: afficher les premières lignes
            String[] lignes = texte.split("\n");
            System.out.println("\n=== PREMIÈRES LIGNES ===");
            for (int i = 0; i < Math.min(20, lignes.length); i++) {
                System.out.println("L" + i + ": " + lignes[i]);
            }

            // Étape 3: Analyser le texte
            DeloitteFacturePayload facture = analyserTexteFacture(texte);
            facture.setEstPDFScanne(estScanne);

            return facture;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction: " + e.getMessage());
            e.printStackTrace();

        }
        return null;
    }

    private String extraireTexteDepuisPDFTexte(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private DeloitteFacturePayload analyserTexteFacture(String texte) {
        DeloitteFacturePayload facture = new DeloitteFacturePayload();

        // Nettoyer le texte
        texte = nettoyerTexte(texte);

        // Extraire les informations
        facture.setTypeDocument(extraireTypeDocument(texte));
        facture.setNumeroDocument(extraireNumeroDocument(texte));
        facture.setDate(extraireDate(texte));
        facture.setReferenceInterne(extraireReferenceInterne(texte));
        facture.setDescription(extraireDescription(texte));
        facture.setClientDestinataire(extraireDestinataire(texte));
        facture.setModalitePaiement(extraireModalitePaiement(texte));

        // Extraire montants
        ExtractionMontants montants = extraireMontants(texte);
        facture.setDevise(montants.devise);
        facture.setMontantHT(montants.montantHT);
        facture.setMontantDebours(montants.montantDebours);
        facture.setMontantTVA(montants.montantTVA);
        facture.setMontantTTC(montants.montantTTC);

        // Pour les avoirs
        if ("AVOIR".equals(facture.getTypeDocument())) {
            facture.setNumeroFactureAnnulee(extraireFactureAnnulee(texte));
        }

        // Informations bancaires
        extraireInfosBancaires(texte, facture);

        return facture;
    }

    private String nettoyerTexte(String texte) {
        // Remplacer les caractères OCR problématiques
        texte = texte.replaceAll("\\|", "I")  // | devient I
                .replaceAll("\\[", "I")  // [ devient I
                .replaceAll("\\]", "I")  // ] devient I
                .replaceAll("\\{", "I")  // { devient I
                .replaceAll("\\}", "I")  // } devient I
                .replaceAll("\\(", "I")  // ( devient I
                .replaceAll("\\)", "I")  // ) devient I
                .replaceAll("\\\\", "/") // \ devient /
                .replaceAll("\"", "'")   // " devient '
                .replaceAll("\\s+", " ") // Espaces multiples
                .replaceAll("(?m)^\\s+", "") // Espaces début ligne
                .replaceAll("(?m)\\s+$", ""); // Espaces fin ligne

        return texte;
    }

    private String extraireTypeDocument(String texte) {
        if (texte.matches("(?s).*\\bAVOIR\\b.*")) {
            return "AVOIR";
        } else if (texte.matches("(?s).*\\bFACTURE\\b.*")) {
            return "FACTURE";
        }
        return "INCONNU";
    }

    private String extraireNumeroDocument(String texte) {
        // Chercher "AVOIR N° 26 / 1222" ou "FACTURE N°25 / 262"
        Pattern pattern = Pattern.compile("(?:AVOIR|FACTURE)\\s*N[°o]\\s*(\\d+\\s*/\\s*\\d+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "");
        }
        return null;
    }

    private String extraireDate(String texte) {
        // Formats possibles: "09 décembre 2025", "09/12/2025", "09-12-2025"
        Pattern pattern = Pattern.compile(
                "(\\d{1,2}\\s+[a-zA-Zéèêëàâäôöùûüç]+\\s+\\d{4})|" +  // 09 décembre 2025
                        "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"                   // 09/12/2025
        );
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    private String extraireReferenceInterne(String texte) {
        Pattern pattern = Pattern.compile("(?:Réf|Ref|Réf int|Ref int)[\\s:.]*([\\w/._-]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extraireDescription(String texte) {
        // Chercher après la référence interne
        Pattern pattern = Pattern.compile("(?:Réf|Ref)[\\s:.]*[^\\n]+\\n([^\\n]+(?=\\n\\s*(?:Total|Debours|\\d)))");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extraireDestinataire(String texte) {
        Pattern pattern = Pattern.compile("(?:à l'attention de|A l'attention de|Client|DESTINATAIRE)[\\s:]*([^\\n]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extraireModalitePaiement(String texte) {
        Pattern pattern = Pattern.compile("(?:Modalité de paiement|Paiement|Mode de paiement)[\\s:]*([^\\n]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extraireFactureAnnulee(String texte) {
        Pattern pattern = Pattern.compile("(?:N° facture à annuler|Facture à annuler|Annule facture)[\\s:]*([\\d]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static class ExtractionMontants {
        String devise = "EUR";
        BigDecimal montantHT = BigDecimal.ZERO;
        BigDecimal montantDebours = BigDecimal.ZERO;
        BigDecimal montantTVA = BigDecimal.ZERO;
        BigDecimal montantTTC = BigDecimal.ZERO;
    }

    private ExtractionMontants extraireMontants(String texte) {
        ExtractionMontants result = new ExtractionMontants();

        // Détecter devise
        if (texte.contains("XOF") || texte.contains("CFA")) {
            result.devise = "XOF";
        } else if (texte.contains("EUR") || texte.contains("€")) {
            result.devise = "EUR";
        }

        // Extraire tous les montants possibles
        Pattern montantPattern = Pattern.compile(
                "([\\d\\s.,]+(?:[.,]\\d{2})?)\\s*(?:XOF|EUR|€|CFA)?"
        );
        Matcher matcher = montantPattern.matcher(texte);

        List<BigDecimal> montantsTrouves = new ArrayList<>();
        while (matcher.find()) {
            String valeurStr = matcher.group(1).replaceAll("[\\s,]", "").replace(",", ".");
            if (valeurStr.matches("\\d+\\.?\\d*")) {
                try {
                    BigDecimal valeur = new BigDecimal(valeurStr);
                    montantsTrouves.add(valeur);
                } catch (Exception e) {
                    // Ignorer
                }
            }
        }

        // Trier et assigner (logique simple)
        if (!montantsTrouves.isEmpty()) {
            montantsTrouves.sort(Collections.reverseOrder());

            if (!montantsTrouves.isEmpty()) {
                result.montantTTC = montantsTrouves.get(0); // Le plus grand = TTC
            }
            if (montantsTrouves.size() >= 2) {
                result.montantHT = montantsTrouves.get(1);
            }
            if (montantsTrouves.size() >= 3) {
                result.montantTVA = montantsTrouves.get(2);
            }

            // Calculer TVA si manquant
            if (result.montantTVA.equals(BigDecimal.ZERO) &&
                    !result.montantHT.equals(BigDecimal.ZERO) &&
                    !result.montantTTC.equals(BigDecimal.ZERO)) {
                result.montantTVA = result.montantTTC.subtract(result.montantHT);
            }
        }

        return result;
    }

    private void extraireInfosBancaires(String texte, DeloitteFacturePayload facture) {
        // Banque
        Pattern banquePattern = Pattern.compile("(?:Banque|BANK)[\\s:]*([^\\n]+)");
        Matcher banqueMatcher = banquePattern.matcher(texte);
        if (banqueMatcher.find()) {
            facture.setBanque(banqueMatcher.group(1).trim());
        }

        // RIB
        Pattern ribPattern = Pattern.compile("(?:RIB|IBAN|Compte)[\\s:]*([\\d\\s]+)");
        Matcher ribMatcher = ribPattern.matcher(texte);
        if (ribMatcher.find()) {
            facture.setRib(ribMatcher.group(1).replaceAll("\\s+", ""));
        }

        // SWIFT
        Pattern swiftPattern = Pattern.compile("(?:SWIFT|BIC)[\\s:]*([A-Z]{8,11})");
        Matcher swiftMatcher = swiftPattern.matcher(texte);
        if (swiftMatcher.find()) {
            facture.setSwift(swiftMatcher.group(1));
        }
    }

    private String extraireAdresse(String texte) {
        Pattern pattern = Pattern.compile("(?:Adresse|Address|Livraison|Cas de réception)[\\s:]*([^\\n]+)");
        Matcher matcher = pattern.matcher(texte);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    public DeloitteFacturePayload extraireDonneesFacture(String cheminFichier) {
        try {
            File file = new File(cheminFichier);
            byte[] pdfBytes = java.nio.file.Files.readAllBytes(file.toPath());
            return extraireDonneesFacture(pdfBytes);
        } catch (IOException e) {
            System.err.println("Erreur lecture fichier: " + e.getMessage());
        }
        return null;
    }

    public boolean testerOCR() {
        return ocrService != null;
    }
}
