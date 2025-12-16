package com.elpandor.hlh.modules.hlh.utils;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class OCRService implements IOCRService {

    @Value("${tesseract.datapath:}")
    private String tesseractDataPath;

    @Value("${tesseract.language:fra+eng}")
    private String tesseractLanguage;

    private Tesseract tesseract;

    @PostConstruct
    public void init() {
        tesseract = new Tesseract();
        System.out.println("tesseractDataPath " + tesseractDataPath);
        // Définir le chemin des données de langue
        if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
            tesseract.setDatapath(tesseractDataPath);
        } else {
            // Chercher dans les emplacements courants
            String[] possiblePaths = {
                    "/usr/share/tesseract-ocr/4.00/tessdata",
                    "/usr/share/tesseract-ocr/tessdata",
                    "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                    System.getProperty("user.dir") + "/tessdata"
            };

            for (String path : possiblePaths) {
                if (Files.exists(Paths.get(path))) {
                    tesseract.setDatapath(path);
                    break;
                }
            }
        }

        tesseract.setLanguage(tesseractLanguage);

        // Configuration OCR
        tesseract.setPageSegMode(1); // Automatic page segmentation
        tesseract.setOcrEngineMode(3); // Default engine
        tesseract.setTessVariable("preserve_interword_spaces", "1");
        tesseract.setTessVariable("user_defined_dpi", "300");
    }

    @Override
    public String extraireTexteDepuisPDF(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder texteComplet = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                System.out.println("Traitement OCR page " + (page + 1) + "/" + document.getNumberOfPages());

                // Rendre l'image avec une bonne résolution
                BufferedImage image = renderer.renderImageWithDPI(page, 300);

                // Prétraitement de l'image pour améliorer l'OCR
                BufferedImage imageAmelioree = preprocessImage(image);

                String textePage = tesseract.doOCR(imageAmelioree);
                texteComplet.append(textePage).append("\n--- Page ").append(page + 1).append(" ---\n");
            }

            return texteComplet.toString();

        } catch (IOException | TesseractException e) {
            System.err.println("Erreur OCR PDF: " + e.getMessage());
            return "";
        }
    }

    @Override
    public String extraireTexteDepuisImage(BufferedImage image) {
        try {
            BufferedImage imageAmelioree = preprocessImage(image);
            return tesseract.doOCR(imageAmelioree);
        } catch (TesseractException e) {
            System.err.println("Erreur OCR image: " + e.getMessage());
            return "";
        }
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        // Prétraitement basique pour améliorer l'OCR
        // 1. Conversion en niveaux de gris
        BufferedImage grayImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        grayImage.getGraphics().drawImage(image, 0, 0, null);

        // 2. Augmenter le contraste (simple threshold)
        BufferedImage highContrast = new BufferedImage(
                grayImage.getWidth(),
                grayImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        highContrast.getGraphics().drawImage(grayImage, 0, 0, null);

        return highContrast;
    }

    public boolean testerConfiguration() {
        try {
            // Créer une image test simple
            BufferedImage imageTest = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            String texte = extraireTexteDepuisImage(imageTest);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur configuration Tesseract: " + e.getMessage());
            //System.err.println("DataPath configuré: " + tesseract.getDatapath());
            return false;
        }
    }
}
