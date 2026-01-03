package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFactureDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class DeloittePDFExtractor4 {

    @Autowired
    private IOCRService ocrService;

    @Autowired
    private GenericInvoiceExtractor genericExtractor;

    public DeloitteFactureDTO extraireDonneesFacture(byte[] pdfBytes) {
        try {
            // 1. Détecter le type de PDF
            boolean isScanned = isPdfScanned(pdfBytes);

            // 2. Extraire le texte
            String extractedText;
            if (isScanned) {
                System.out.println("PDF scanné détecté, utilisation OCR...");
                extractedText = ocrService.extraireTexteDepuisPDF(pdfBytes);
            } else {
                System.out.println("PDF texte détecté, extraction directe...");
                extractedText = extractTextFromPdf(pdfBytes);
            }

            System.out.println("Texte extrait (" + extractedText.length() + " caractères)");

            // 3. Utiliser l'extracteur générique
            return genericExtractor.extractFromText(extractedText, isScanned);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
//            return createErrorResponse("Erreur lors de l'extraction: " + e.getMessage());
        }
    }

    private boolean isPdfScanned(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Heuristique simple: si peu de texte mais des pages, c'est probablement scanné
            return text.trim().length() < 100 && document.getNumberOfPages() > 0;
        }
    }

    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    public DeloitteFactureDTO extraireDonneesFacture(String filePath) {
        try {
            File file = new File(filePath);
            byte[] pdfBytes = Files.readAllBytes(file.toPath());
            return extraireDonneesFacture(pdfBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean testerOCR() {
        return ocrService != null;
    }

}
