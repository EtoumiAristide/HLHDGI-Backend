package com.elpandor.hlh.modules.hlh.utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OCRCleaner {

    private static final Map<String, String> CORRECTIONS_OCR = new HashMap<>();

    static {
        // Corrections courantes pour l'OCR français
        CORRECTIONS_OCR.put("l'", "l'");
        CORRECTIONS_OCR.put("d'", "d'");
        CORRECTIONS_OCR.put("qu'", "qu'");
        CORRECTIONS_OCR.put("s'", "s'");
        CORRECTIONS_OCR.put("n'", "n'");
        CORRECTIONS_OCR.put("c'", "c'");
        CORRECTIONS_OCR.put("j'", "j'");
        CORRECTIONS_OCR.put("m'", "m'");
        CORRECTIONS_OCR.put("t'", "t'");
        CORRECTIONS_OCR.put("lorsqu'", "lorsqu'");
        CORRECTIONS_OCR.put("puisqu'", "puisqu'");
        CORRECTIONS_OCR.put("quoiqu'", "quoiqu'");
        CORRECTIONS_OCR.put("jusqu'", "jusqu'");

        // Corrections de caractères
        CORRECTIONS_OCR.put("Ã©", "é");
        CORRECTIONS_OCR.put("Ã¨", "è");
        CORRECTIONS_OCR.put("Ãª", "ê");
        CORRECTIONS_OCR.put("Ã«", "ë");
        CORRECTIONS_OCR.put("Ã¢", "â");
        CORRECTIONS_OCR.put("Ã€", "À");
        CORRECTIONS_OCR.put("Ã", "à");
        CORRECTIONS_OCR.put("Ã®", "î");
        CORRECTIONS_OCR.put("Ã¯", "ï");
        CORRECTIONS_OCR.put("Ã´", "ô");
        CORRECTIONS_OCR.put("Ã¶", "ö");
        CORRECTIONS_OCR.put("Ã»", "û");
        CORRECTIONS_OCR.put("Ã¼", "ü");
        CORRECTIONS_OCR.put("Ã§", "ç");
        CORRECTIONS_OCR.put("Å“", "œ");

        // Corrections spécifiques aux factures
        CORRECTIONS_OCR.put("Facture", "FACTURE");
        CORRECTIONS_OCR.put("Total H.T.", "Total H.T.");
        CORRECTIONS_OCR.put("Total TTC", "Total TTC");
        CORRECTIONS_OCR.put("Tva", "Tva");
        CORRECTIONS_OCR.put("non facturée", "non facturée");
        CORRECTIONS_OCR.put("suspendue", "suspendue");
    }

    public String nettoyerTexteFacture(String texte) {
        if (texte == null) return "";

        // 1. Appliquer les corrections
        for (Map.Entry<String, String> correction : CORRECTIONS_OCR.entrySet()) {
            texte = texte.replace(correction.getKey(), correction.getValue());
        }

        // 2. Corriger les erreurs d'OCR courantes
        texte = texte.replaceAll("\\|", "I");
        texte = texte.replaceAll("\\[", "I");
        texte = texte.replaceAll("\\]", "I");
        texte = texte.replaceAll("\\{", "I");
        texte = texte.replaceAll("\\}", "I");

        // 3. Normaliser les espaces
        texte = texte.replaceAll("\\s+", " ");
        texte = texte.replaceAll("(?m)^\\s+", "");
        texte = texte.replaceAll("(?m)\\s+$", "");

        // 4. Corriger les montants
        texte = corrigerMontants(texte);

        return texte;
    }

    private String corrigerMontants(String texte) {
        // Remplacer "O" (lettre) par "0" (zéro) dans les montants
        Pattern montantPattern = Pattern.compile("([\\d\\s]+)[Oo]([\\d\\s]+)");
        Matcher matcher = montantPattern.matcher(texte);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = matcher.group(1) + "0" + matcher.group(2);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }
}