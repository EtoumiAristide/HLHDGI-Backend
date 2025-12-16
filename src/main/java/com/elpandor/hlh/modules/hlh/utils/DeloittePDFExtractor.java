package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFacturePayload;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeloittePDFExtractor {

    public DeloitteFacturePayload extraireDonneesFacture(byte[] pdfBytes) throws IOException {
        PDDocument document = PDDocument.load(pdfBytes);

        return extraireDonnees(document);
    }

    public DeloitteFacturePayload extraireDonneesFacture(String cheminFichier) throws IOException {
        PDDocument document = PDDocument.load(new File(cheminFichier));
        return extraireDonnees(document);
    }

    private DeloitteFacturePayload extraireDonnees(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        System.out.println("text: " + text);
        // Extraire les données brutes pour debug
        Map<String, String> donneesBrutes = extraireDonneesBrutes(text);
        System.out.println("donneesBrutes: " + donneesBrutes);

        // Créer l'objet FactureDTO structuré
        DeloitteFacturePayload facture = new DeloitteFacturePayload();

        // 1. Type de document
        facture.setTypeDocument(determinerTypeDocument(text));

        // 2. Numéro du document
        facture.setNumeroDocument(extrairePattern(text, "(FACTURE|AVOIR)\\s*N°\\s*(\\d+\\s*/\\s*\\d+)", 2));

        // 3. Date
        facture.setDate(extrairePattern(text, "(\\d{1,2}\\s+\\p{L}+\\s+\\d{4})"));

        // 4. Référence interne
        facture.setReferenceInterne(extrairePattern(text, "(?:Réf|Réf int\\.?)[\\s:]*([\\w/_-]+)"));

        // 5. Devise et montants
        String[] montants = extraireMontants(text, facture.getTypeDocument());
        facture.setDevise(montants[0]);
        facture.setMontantHT(convertirEnBigDecimal(montants[1]));
        facture.setMontantDebours(convertirEnBigDecimal(montants[2]));
        facture.setMontantTVA(convertirEnBigDecimal(montants[3]));
        facture.setMontantTTC(convertirEnBigDecimal(montants[4]));

        // 6. Pour les avoirs : numéro de facture à annuler
        if ("AVOIR".equalsIgnoreCase(facture.getTypeDocument())) {
            facture.setNumeroFactureAnnulee(extrairePattern(text, "N°\\s*facture\\s*à\\s*annuler\\s*:\\s*(\\d+)"));
        }

        // 7. Description
        facture.setDescription(extraireDescription(text));

        // 8. Client destinataire
        facture.setClientDestinataire(extrairePattern(text, "à l'attention de\\s*[:\\(]?\\s*([^\\)\\n]+)"));

        // 9. Modalité de paiement
        facture.setModalitePaiement(extrairePattern(text, "Modalité de paiement[\\s:]*(.+?)(?=\\n|$)"));

        // 10. Informations bancaires
        extraireInfosBancaires(text, facture);

        // 11. Adresse
        facture.setAdresse(extrairePattern(text, "Cas de réception[\\s:]*([^\\n]+)"));

        document.close();
        return facture;
    }

    private String determinerTypeDocument(String text) {
        if (text.contains("AVOIR N°")) {
            return "AVOIR";
        } else if (text.contains("FACTURE N°")) {
            return "FACTURE";
        }
        return "INCONNU";
    }

    private String[] extraireMontants(String text, String typeDocument) {
        String devise = "XOF";
        String montantHT = "0";
        String montantDebours = "0";
        String montantTVA = "0";
        String montantTTC = "0";

        // Chercher la devise (EUR ou XOF)
        Pattern devisePattern = Pattern.compile("Montant\\s+en\\s+(EUR|XOF|\\w{3})", Pattern.CASE_INSENSITIVE);
        Matcher deviseMatcher = devisePattern.matcher(text);
        if (deviseMatcher.find()) {
            devise = deviseMatcher.group(1);
        }

        // Extraire les montants selon le format
        String[] lignes = text.split("\n");
        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();

            // Pour les avoirs (format simple)
            if (typeDocument.equals("AVOIR")) {
                if (ligne.contains("Total H.T.")) {
                    montantHT = extraireValeurMontant(lignes[i + 1]);
                }
                if (ligne.contains("Total TTC")) {
                    montantTTC = extraireValeurMontant(lignes[i + 1]);
                }
                if (ligne.contains("Tva non facturée")) {
                    montantTVA = extraireValeurMontant(ligne);
                }
            }
            // Pour les factures (format avec débours)
            else {
                if (ligne.contains("Total H.T.")) {
                    montantHT = extraireValeurMontant(lignes[i + 1]);
                }
                if (ligne.contains("Debours") && !ligne.contains("Total")) {
                    montantDebours = extraireValeurMontant(ligne);
                }
                if (ligne.contains("Tva non facturée")) {
                    montantTVA = extraireValeurMontant(ligne);
                }
                if (ligne.contains("Total TTC")) {
                    montantTTC = extraireValeurMontant(lignes[i + 1]);
                }
            }
        }

        return new String[]{devise, montantHT, montantDebours, montantTVA, montantTTC};
    }

    private String extraireValeurMontant(String ligne) {
        Pattern pattern = Pattern.compile("([\\d\\s,]+(?:\\.\\d{2})?)\\s*(?:EUR|XOF|\\w{3})?");
        Matcher matcher = pattern.matcher(ligne);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "").replace(",", ".");
        }
        return "0";
    }

    private void extraireInfosBancaires(String text, DeloitteFacturePayload facture) {
        Pattern banquePattern = Pattern.compile("Banque[\\s:]*([^\\n]+)");
        Pattern ribPattern = Pattern.compile("Numéro de Compte RIB\\s*([\\d\\s]+)");
        Pattern swiftPattern = Pattern.compile("Code SWIFT\\s*([A-Z]{8,11})");

        Matcher banqueMatcher = banquePattern.matcher(text);
        if (banqueMatcher.find()) {
            facture.setBanque(banqueMatcher.group(1).trim());
        }

        Matcher ribMatcher = ribPattern.matcher(text);
        if (ribMatcher.find()) {
            facture.setRib(ribMatcher.group(1).replaceAll("\\s+", ""));
        }

        Matcher swiftMatcher = swiftPattern.matcher(text);
        if (swiftMatcher.find()) {
            facture.setSwift(swiftMatcher.group(1));
        }
    }

    private String extrairePattern(String text, String regex) {
        return extrairePattern(text, regex, 1);
    }

    private String extrairePattern(String text, String regex, int group) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group).trim();
        }
        return null;
    }

    private String extraireDescription(String text) {
        // Chercher la ligne après le numéro de référence ou avant les montants
        Pattern pattern = Pattern.compile("(?:(?:Réf|Réf int\\.?)[\\s:]*[^\\n]+\\n)([^\\n]+(?=\\n\\s*\\d))");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private BigDecimal convertirEnBigDecimal(String valeur) {
        if (valeur == null || valeur.isEmpty() || "0".equals(valeur)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valeur);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, String> extraireDonneesBrutes(String text) {
        Map<String, String> donnees = new HashMap<>();
        String[] lignes = text.split("\n");

        for (String ligne : lignes) {
            if (ligne.contains(":")) {
                String[] parties = ligne.split(":", 2);
                if (parties.length == 2) {
                    donnees.put(parties[0].trim(), parties[1].trim());
                }
            }
        }
        return donnees;
    }
}
