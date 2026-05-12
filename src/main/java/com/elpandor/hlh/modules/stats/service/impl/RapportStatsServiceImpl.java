package com.elpandor.hlh.modules.stats.service.impl;

import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.elpandor.hlh.modules.stats.model.FactureTimbre;
import com.elpandor.hlh.modules.stats.model.FactureTimbreRequest;
import com.elpandor.hlh.modules.stats.service.RapportStatsService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RapportStatsServiceImpl implements RapportStatsService {

    private final FactureRepository factureRepository;

    public RapportStatsServiceImpl(FactureRepository factureRepository) {
        this.factureRepository = factureRepository;
    }

    @Override
    public Page<FactureTimbre> getFactureTimbre(int page, int size, String sortBy, String direction, FactureTimbreRequest request) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return factureRepository.getFactureTimbrePaginate(pageable, request.getNumcc(), request.getDateDebut(), request.getDateFin());
    }

    @Override
    public byte[] exportTimbreToExcel(FactureTimbreRequest request) throws IOException {
        // Récupérer toutes les données (sans pagination pour l'export)
        List<FactureTimbre> toutesLesFactures = factureRepository.getFactureTimbre(request.getNumcc(), request.getDateDebut(), request.getDateFin());

        // Grouper par mois
        Map<String, List<FactureTimbre>> facturesParMois = toutesLesFactures.stream()
                .collect(Collectors.groupingBy(FactureTimbre::getMois));

        if (toutesLesFactures.isEmpty()) {
            // Retourner un Excel vide ou une erreur
            throw new RuntimeException("Aucune donnée trouvée pour la période spécifiée");
        }

        // Trier les mois (ordre chronologique)
        List<String> moisTries = facturesParMois.keySet().stream()
                .sorted(Comparator.comparing((String m) -> {
                    String[] parts = m.split(" ");
                    return Integer.parseInt(parts[1]); // année
                }).thenComparing(m -> {
                    String[] parts = m.split(" ");
                    return Integer.parseInt(parts[0]); // mois
                }))
                .collect(Collectors.toList());

        // Créer le workbook Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            // Créer les styles
            Map<String, CellStyle> styles = createStyles(workbook);

            // Pour chaque mois, créer une feuille
            for (String mois : moisTries) {
                List<FactureTimbre> factures = facturesParMois.get(mois);

                // Nom de la feuille (ex: "FEVRIER 26", "MARS 26")
                String sheetName = formatSheetName(mois);
                Sheet sheet = workbook.createSheet(sheetName);

                // Créer la feuille
                createSheetForMonth(sheet, factures, styles);
            }
            /*for (Map.Entry<String, List<FactureTimbre>> entry : facturesParMois.entrySet()) {
                String mois = entry.getKey();
                List<FactureTimbre> factures = entry.getValue();

                // Nom de la feuille (ex: "FEVRIER 26", "MARS 26")
                String sheetName = formatSheetName(mois);
                Sheet sheet = workbook.createSheet(sheetName);

                // Créer la feuille
                createSheetForMonth(sheet, factures, styles);
            }*/

            // Écrire dans le tableau de bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // Style pour l'en-tête
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        styles.put("header", headerStyle);

        // Style pour les cellules normales
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        styles.put("cell", cellStyle);

        // Style pour le total
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        styles.put("total", totalStyle);

        return styles;
    }

    private void createSheetForMonth(Sheet sheet, List<FactureTimbre> factures, Map<String, CellStyle> styles) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // En-têtes
        String[] headers = {"BK NAME", "N°FACTURE", "JOUR CA", "MOYEN DE PAIEMENT",
                "NBRE TICKET > 5000", "MONTANT TIMBRE", "TOTAL"};

        Row headerRow = sheet.createRow(3); // Ligne 4 (index 3)
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
            sheet.setColumnWidth(i, 5000);
        }

        // Lignes de données
        int rowNum = 4;
        for (FactureTimbre facture : factures) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(facture.getBkName());
            row.createCell(1).setCellValue(facture.getNFacture());
            row.createCell(2).setCellValue(facture.getJourCa().format(formatter));
            row.createCell(3).setCellValue(facture.getMoyenDePaiement());
            row.createCell(4).setCellValue(facture.getNbreTicket5000());
            row.createCell(5).setCellValue(facture.getMontantTimbre());
            row.createCell(6).setCellValue(facture.getTotal().doubleValue());

            // Appliquer le style de cellule
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(styles.get("cell"));
            }
        }

        // Ligne de total par établissement et mode de paiement
        Map<String, Map<String, Integer>> totalsByEtablishmentAndPayment = calculateTotals(factures);

        for (Map.Entry<String, Map<String, Integer>> etabEntry : totalsByEtablishmentAndPayment.entrySet()) {
            String bkName = etabEntry.getKey();
            for (Map.Entry<String, Integer> paymentEntry : etabEntry.getValue().entrySet()) {
                String paymentMode = paymentEntry.getKey();
                int totalTickets = paymentEntry.getValue();

                Row totalRow = sheet.createRow(rowNum++);
                totalRow.createCell(0).setCellValue(bkName);
                totalRow.createCell(1).setCellValue("");
                totalRow.createCell(2).setCellValue("");
                totalRow.createCell(3).setCellValue(paymentMode + " (Total)");
                totalRow.createCell(4).setCellValue(totalTickets);
                totalRow.createCell(5).setCellValue(100);
                totalRow.createCell(6).setCellValue(totalTickets * 100);

                for (int i = 0; i < 7; i++) {
                    totalRow.getCell(i).setCellStyle(styles.get("total"));
                }
            }
        }

        // Ligne de total général
        int totalGeneral = factures.stream()
                .mapToInt(FactureTimbre::getNbreTicket5000)
                .sum();

        Row grandTotalRow = sheet.createRow(rowNum + 1);
        grandTotalRow.createCell(3).setCellValue("TOTAL");
        grandTotalRow.createCell(4).setCellValue(totalGeneral);
        grandTotalRow.createCell(5).setCellValue(100);
        grandTotalRow.createCell(6).setCellValue(totalGeneral * 100);

        for (int i = 0; i < 7; i++) {
            if (i >= 0) {
                Cell cell = grandTotalRow.getCell(i);
                if (cell == null) {
                    cell = grandTotalRow.createCell(i);
                }
                cell.setCellStyle(styles.get("total"));
            }
        }
    }

    private Map<String, Map<String, Integer>> calculateTotals(List<FactureTimbre> factures) {
        Map<String, Map<String, Integer>> totals = new HashMap<>();

        for (FactureTimbre facture : factures) {
            String bkName = facture.getBkName();
            String paymentMode = facture.getMoyenDePaiement();
            int tickets = facture.getNbreTicket5000();

            totals.computeIfAbsent(bkName, k -> new HashMap<>())
                    .merge(paymentMode, tickets, Integer::sum);
        }

        return totals;
    }

    private String formatSheetName(String mois) {
        // Exemple: "02 2026" -> "FEVRIER 26"
        String[] parts = mois.split(" ");
        String monthNumber = parts[0];
        String year = parts[1].substring(2); // Prendre les 2 derniers chiffres

        String monthName = switch (monthNumber) {
            case "01" -> "JANVIER";
            case "02" -> "FEVRIER";
            case "03" -> "MARS";
            case "04" -> "AVRIL";
            case "05" -> "MAI";
            case "06" -> "JUIN";
            case "07" -> "JUILLET";
            case "08" -> "AOUT";
            case "09" -> "SEPTEMBRE";
            case "10" -> "OCTOBRE";
            case "11" -> "NOVEMBRE";
            case "12" -> "DECEMBRE";
            default -> monthNumber;
        };

        return monthName + " " + year;
    }
}
