package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.*;
import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.TicketVenteRepository;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Construit le rapport d'état des extractions automatiques Zino (synthèse par fichier +
 * détail des factures envoyées à la FNE) et génère ses exports (Excel, PDF, Word).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapportExtractionUseCase {

    private final FichierSourceRepository fichierSourceRepository;
    private final TicketVenteRepository ticketVenteRepository;
    private final FactureRepository factureRepository;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ===================== Construction des données =====================

    public RapportExtractionResponse genererRapport(RapportExtractionRequest request) {
        LocalDateTime debut = request.getDateDebut().atStartOfDay();
        LocalDateTime fin = request.getDateFin().atTime(23, 59, 59);

        List<FichierSource> fichiersSource = fichierSourceRepository.findByPeriode(debut, fin);
        if (request.getStatut() != null && !request.getStatut().isBlank()) {
            fichiersSource = fichiersSource.stream()
                    .filter(f -> request.getStatut().equalsIgnoreCase(f.getStatut()))
                    .collect(Collectors.toList());
        }

        // NB: filtre sur dateCreation (Instant, hérité d'AuditModel) plutôt que dateFacture,
        // conformément à l'ajustement fait sur le backfill (voir échange précédent).
        List<Facture> facturesAutomatisation = recupererFacturesAutomatisation(request);

        Map<String, Long> nombreFacturesParFichier = facturesAutomatisation.stream()
                .filter(f -> f.getAutomatisationFileName() != null)
                .collect(Collectors.groupingBy(Facture::getAutomatisationFileName, Collectors.counting()));

        List<RapportFichierDto> fichiersDto = new ArrayList<>();
        for (FichierSource fs : fichiersSource) {
            int nbTickets = ticketVenteRepository.findByNomFichierSource(fs.getNomFichier()).size();
            long nbFactures = nombreFacturesParFichier.getOrDefault(fs.getNomFichier(), 0L);

            fichiersDto.add(RapportFichierDto.builder()
                    .id(fs.getId())
                    .nomFichier(fs.getNomFichier())
                    .statut(fs.getStatut())
                    .dateCreation(fs.getDateCreation())
                    .dateDerniereModification(fs.getDateDerniereModification())
                    .tentativeEnvoi(fs.getTentativeEnvoi())
                    .dernierMessageErreur(fs.getDernierMessageErreur())
                    .nombreTickets(nbTickets)
                    .nombreFacturesEnvoyees((int) nbFactures)
                    .build());
        }

        RapportExtractionTotaux totaux = RapportExtractionTotaux.builder()
                .nombreFichiers(fichiersDto.size())
                .nombreFichiersSucces((int) fichiersDto.stream().filter(f -> "SENT".equalsIgnoreCase(f.getStatut())).count())
                .nombreFichiersErreur((int) fichiersDto.stream().filter(f -> f.getStatut() != null && f.getStatut().toUpperCase().contains("ERROR")).count())
                .nombreFichiersEnAttente((int) fichiersDto.stream().filter(f -> "PENDING".equalsIgnoreCase(f.getStatut())).count())
                .nombreTicketsExtraits(fichiersDto.stream().mapToInt(RapportFichierDto::getNombreTickets).sum())
                .nombreFacturesEnvoyees(facturesAutomatisation.size())
                .build();

        return RapportExtractionResponse.builder()
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .fichiers(fichiersDto)
                .totaux(totaux)
                .build();
    }

    /**
     * Page du détail des factures automatisées, pour l'affichage à l'écran (liste potentiellement
     * nombreuse). Le tri se fait sur les propriétés de l'entité Facture (ex: "dateFacture",
     * "dateCreation", "numFacture").
     */
    public Page<RapportFactureDto> getFacturesPaginees(
            RapportExtractionRequest request, int page, int size, String sortBy, String direction) {

        Instant debut = request.getDateDebut().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant fin = request.getDateFin().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Sort sort = "DESC".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return factureRepository.findByIsAutomatisationTrueAndDateCreationBetween(debut, fin, pageable)
                .map(this::versRapportFactureDto);
    }

    /**
     * Liste complète (non paginée) des factures automatisées sur la période, utilisée en interne
     * pour construire les exports (Excel/PDF/Word) et calculer les totaux — jamais renvoyée
     * telle quelle à l'écran, pour ne pas charger d'un coup un volume potentiellement important.
     */
    private List<Facture> recupererFacturesAutomatisation(RapportExtractionRequest request) {
        java.time.Instant debut = request.getDateDebut().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant fin = request.getDateFin().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        return factureRepository.findByIsAutomatisationTrueAndDateCreationBetweenOrderByDateCreationAsc(debut, fin);
    }

    /**
     * Liste complète des factures, mappées en DTO, pour les exports (Excel/PDF/Word) uniquement.
     */
    private List<RapportFactureDto> recupererFacturesDto(RapportExtractionRequest request) {
        return recupererFacturesAutomatisation(request).stream()
                .map(this::versRapportFactureDto)
                .collect(Collectors.toList());
    }

    private RapportFactureDto versRapportFactureDto(Facture facture) {
        Double montant = null;
        String statutFNE = null;
        String referenceFNE = null;
        String lienFacture = null;

        if (facture.getReponseFNE() != null && !facture.getReponseFNE().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(facture.getReponseFNE());
                JsonNode invoice = root.path("invoice");
                if (invoice.has("totalDue")) {
                    montant = invoice.path("totalDue").asDouble();
                } else if (invoice.has("amount")) {
                    montant = invoice.path("amount").asDouble();
                }
                if (invoice.has("status")) {
                    statutFNE = invoice.path("status").asText();
                }
                if (root.has("reference")) {
                    referenceFNE = root.path("reference").asText();
                }
                if (root.has("token")) {
                    lienFacture = root.path("token").asText();
                }
            } catch (Exception e) {
                log.debug("Impossible de parser reponseFNE pour la facture {}: {}", facture.getNumFacture(), e.getMessage());
            }
        }

        return RapportFactureDto.builder()
                .numFacture(facture.getNumFacture())
                .referenceFNE(facture.getReference())
                .dateFacture(facture.getDateFacture())
                .nomClient(facture.getNomClient())
                .modePaiement(facture.getModePaiement() != null ? facture.getModePaiement().toString() : null)
                .montant(Double.valueOf(String.format("%.0f",montant)))
                .statutFNE(statutFNE)
                .referenceFNE(referenceFNE)
                .lienFacture(lienFacture)
                .nomFichierSource(facture.getAutomatisationFileName())
                .build();
    }

    // ===================== Export Excel =====================

    public byte[] exporterExcel(RapportExtractionRequest request) throws Exception {
        RapportExtractionResponse rapport = genererRapport(request);
        List<RapportFactureDto> factures = recupererFacturesDto(request);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle styleEntete = creerStyleEntete(workbook);
            CellStyle styleDate = workbook.createCellStyle();
            styleDate.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));

            // ----- Feuille Synthèse -----
            Sheet feuilleSynthese = workbook.createSheet("Synthèse fichiers");
            String[] entetesSynthese = {"Fichier", "Statut", "Date création", "Dernière modification",
                    "Tentatives", "Tickets extraits", "Factures envoyées", "Dernier message d'erreur"};
            Row enteteRow = feuilleSynthese.createRow(0);
            for (int i = 0; i < entetesSynthese.length; i++) {
                Cell cell = enteteRow.createCell(i);
                cell.setCellValue(entetesSynthese[i]);
                cell.setCellStyle(styleEntete);
            }
            int rowIdx = 1;
            for (RapportFichierDto f : rapport.getFichiers()) {
                Row row = feuilleSynthese.createRow(rowIdx++);
                row.createCell(0).setCellValue(f.getNomFichier());
                row.createCell(1).setCellValue(f.getStatut());
                row.createCell(2).setCellValue(f.getDateCreation() != null ? f.getDateCreation().format(FMT_DATETIME) : "");
                row.createCell(3).setCellValue(f.getDateDerniereModification() != null ? f.getDateDerniereModification().format(FMT_DATETIME) : "");
                row.createCell(4).setCellValue(f.getTentativeEnvoi() != null ? f.getTentativeEnvoi() : 0);
                row.createCell(5).setCellValue(f.getNombreTickets() != null ? f.getNombreTickets() : 0);
                row.createCell(6).setCellValue(f.getNombreFacturesEnvoyees() != null ? f.getNombreFacturesEnvoyees() : 0);
                row.createCell(7).setCellValue(f.getDernierMessageErreur() != null ? f.getDernierMessageErreur() : "");
            }
            for (int i = 0; i < entetesSynthese.length; i++) {
                feuilleSynthese.autoSizeColumn(i);
            }

            // ----- Feuille Détail factures -----
            Sheet feuilleFactures = workbook.createSheet("Détail factures");
            String[] entetesFactures = {"N° Facture", "Référence FNE", "Date", "Client", "Mode de paiement",
                    "Montant", "Statut FNE", "Fichier source"};
            Row enteteFactRow = feuilleFactures.createRow(0);
            for (int i = 0; i < entetesFactures.length; i++) {
                Cell cell = enteteFactRow.createCell(i);
                cell.setCellValue(entetesFactures[i]);
                cell.setCellStyle(styleEntete);
            }
            int rowIdxF = 1;
            for (RapportFactureDto f : factures) {
                Row row = feuilleFactures.createRow(rowIdxF++);
                row.createCell(0).setCellValue(f.getNumFacture());
                row.createCell(1).setCellValue(f.getReferenceFNE());
                row.createCell(2).setCellValue(f.getDateFacture() != null ? f.getDateFacture().format(FMT_DATE) : "");
                row.createCell(3).setCellValue(f.getNomClient());
                row.createCell(4).setCellValue(f.getModePaiement());
                row.createCell(5).setCellValue(f.getMontant() != null ? f.getMontant() : 0.0);
                row.createCell(6).setCellValue(f.getStatutFNE());
                row.createCell(7).setCellValue(f.getNomFichierSource());
            }
            for (int i = 0; i < entetesFactures.length; i++) {
                feuilleFactures.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle creerStyleEntete(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // ===================== Export PDF =====================

    public byte[] exporterPdf(RapportExtractionRequest request) throws Exception {
        RapportExtractionResponse rapport = genererRapport(request);
        List<RapportFactureDto> factures = recupererFacturesDto(request);

        Context ctx = new Context();
        ctx.setVariable("dateDebut", rapport.getDateDebut().format(FMT_DATE));
        ctx.setVariable("dateFin", rapport.getDateFin().format(FMT_DATE));
        ctx.setVariable("fichiers", rapport.getFichiers());
        ctx.setVariable("factures", factures);
        ctx.setVariable("totaux", rapport.getTotaux());
        ctx.setVariable("fmtDate", FMT_DATE);
        ctx.setVariable("fmtDateTime", FMT_DATETIME);

        String html = templateEngine.process("rapport-extraction-zino", ctx);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, new File("src/main/resources/static/").toURI().toString());
        builder.toStream(os);
        builder.run();
        return os.toByteArray();
    }

    // ===================== Export Word (docx) =====================

    public byte[] exporterWord(RapportExtractionRequest request) throws Exception {
        RapportExtractionResponse rapport = genererRapport(request);
        List<RapportFactureDto> factures = recupererFacturesDto(request);

        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph titre = document.createParagraph();
            titre.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titreRun = titre.createRun();
            titreRun.setText("Rapport d'état des extractions automatiques - Zino");
            titreRun.setBold(true);
            titreRun.setFontSize(16);

            XWPFParagraph periode = document.createParagraph();
            XWPFRun periodeRun = periode.createRun();
            periodeRun.setText("Période du " + rapport.getDateDebut().format(FMT_DATE) + " au " + rapport.getDateFin().format(FMT_DATE));
            periodeRun.setItalic(true);

            XWPFParagraph resume = document.createParagraph();
            XWPFRun resumeRun = resume.createRun();
            RapportExtractionTotaux t = rapport.getTotaux();
            resumeRun.setText(String.format(
                    "%d fichier(s) traité(s) - %d en succès, %d en erreur, %d en attente - %d ticket(s) extrait(s) - %d facture(s) envoyée(s)",
                    t.getNombreFichiers(), t.getNombreFichiersSucces(), t.getNombreFichiersErreur(),
                    t.getNombreFichiersEnAttente(), t.getNombreTicketsExtraits(), t.getNombreFacturesEnvoyees()));

            document.createParagraph();

            XWPFParagraph titreSynthese = document.createParagraph();
            XWPFRun titreSyntheseRun = titreSynthese.createRun();
            titreSyntheseRun.setText("Synthèse par fichier");
            titreSyntheseRun.setBold(true);
            titreSyntheseRun.setFontSize(13);

            String[] entetesSynthese = {"Fichier", "Statut", "Date création", "Tentatives", "Tickets", "Factures envoyées"};
            XWPFTable tableSynthese = document.createTable(rapport.getFichiers().size() + 1, entetesSynthese.length);
            for (int i = 0; i < entetesSynthese.length; i++) {
                remplirCelluleEntete(tableSynthese.getRow(0).getCell(i), entetesSynthese[i]);
            }
            int r = 1;
            for (RapportFichierDto f : rapport.getFichiers()) {
                XWPFTableRow row = tableSynthese.getRow(r++);
                row.getCell(0).setText(f.getNomFichier());
                row.getCell(1).setText(f.getStatut());
                row.getCell(2).setText(f.getDateCreation() != null ? f.getDateCreation().format(FMT_DATETIME) : "");
                row.getCell(3).setText(String.valueOf(f.getTentativeEnvoi() != null ? f.getTentativeEnvoi() : 0));
                row.getCell(4).setText(String.valueOf(f.getNombreTickets() != null ? f.getNombreTickets() : 0));
                row.getCell(5).setText(String.valueOf(f.getNombreFacturesEnvoyees() != null ? f.getNombreFacturesEnvoyees() : 0));
            }

            document.createParagraph();

            XWPFParagraph titreDetail = document.createParagraph();
            XWPFRun titreDetailRun = titreDetail.createRun();
            titreDetailRun.setText("Détail des factures");
            titreDetailRun.setBold(true);
            titreDetailRun.setFontSize(13);

            String[] entetesFactures = {"N° Facture", "Référence FNE", "Date", "Client", "Mode paiement", "Montant", "Statut FNE"};
            XWPFTable tableFactures = document.createTable(factures.size() + 1, entetesFactures.length);
            for (int i = 0; i < entetesFactures.length; i++) {
                remplirCelluleEntete(tableFactures.getRow(0).getCell(i), entetesFactures[i]);
            }
            int rf = 1;
            for (RapportFactureDto f : factures) {
                XWPFTableRow row = tableFactures.getRow(rf++);
                row.getCell(0).setText(f.getNumFacture());
                row.getCell(1).setText(f.getReferenceFNE());
                row.getCell(2).setText(f.getDateFacture() != null ? f.getDateFacture().format(FMT_DATE) : "");
                row.getCell(3).setText(f.getNomClient());
                row.getCell(4).setText(f.getModePaiement());
                row.getCell(5).setText(f.getMontant() != null ? String.valueOf(f.getMontant()) : "");
                row.getCell(6).setText(f.getStatutFNE());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    private void remplirCelluleEntete(XWPFTableCell cell, String texte) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun run = p.createRun();
        run.setText(texte);
        run.setBold(true);
    }

    /**
     * Génère les octets du rapport dans le format demandé ("excel", "pdf" ou "word").
     */
    public byte[] exporter(RapportExtractionRequest request, String format) throws Exception {
        return switch (format.toLowerCase()) {
            case "excel", "xlsx" -> exporterExcel(request);
            case "pdf" -> exporterPdf(request);
            case "word", "docx" -> exporterWord(request);
            default -> throw new IllegalArgumentException("Format d'export inconnu: " + format);
        };
    }
}
