package com.elpandor.hlh.modules.bk.service.impl;

import com.elpandor.hlh.modules.bk.model.BkTimbreDetail;
import com.elpandor.hlh.modules.bk.model.BkTimbreMonthlyReport;
import com.elpandor.hlh.modules.bk.model.BkTimbreRequest;
import com.elpandor.hlh.modules.bk.service.BurgerKingTimbreService;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.BKExtractedData;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment.PaymentType;
import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class BurgerKingTimbreServiceImpl implements BurgerKingTimbreService {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(5000);
    private static final BigDecimal STAMP_DUTY_AMOUNT = BigDecimal.valueOf(100);

    private final FactureRepository factureRepository;
    private final ObjectMapper objectMapper;

    public BurgerKingTimbreServiceImpl(FactureRepository factureRepository) {
        this.factureRepository = factureRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<BkTimbreDetail> calculateDetails(BkTimbreRequest request) {

        // Charger les données depuis la BD (champ dataSend)
        List<Payment> payments = loadPaymentsFromDatabase(request);

        if (payments == null || payments.isEmpty()) {
            System.out.println("🔍 BK Debug - Aucun paiement trouvé pour la période " + request.getPeriod());
            return Collections.emptyList();
        }

        System.out.println("🔍 BK Debug - Traitement de " + payments.size() + " paiements");

        List<BkTimbreDetail> details = new ArrayList<>();
        int eligibleCount = 0;
        int ineligibleCount = 0;

        for (Payment payment : payments) {
            if (payment == null) {
                System.out.println("🔍 BK Debug - Paiement null ignoré");
                continue;
            }

            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            boolean hasTotalRow = payment.getCheckNumber() != null && payment.getCheckNumber().toLowerCase().contains("total");
            boolean eligible = isEligible(payment, amount, hasTotalRow);
            BigDecimal stampDuty = eligible ? STAMP_DUTY_AMOUNT : BigDecimal.ZERO;
            String reason = buildEligibilityReason(payment, amount, hasTotalRow, eligible);

            if (eligible) {
                eligibleCount++;
            } else {
                ineligibleCount++;
            }

            System.out.println("🔍 BK Debug - Paiement: " + payment.getCheckNumber() + " | Montant: " + amount + " | Type: " + payment.getPaymentType() + " | Éligible: " + eligible + " | Raison: " + reason);

            details.add(BkTimbreDetail.builder()
                    .transactionId(payment.getId() != null ? payment.getId().toString() : null)
                    .checkNumber(payment.getCheckNumber())
                    .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : null)
                    .amount(amount)
                    .stampDuty(stampDuty)
                    .eligible(eligible)
                    .eligibilityReason(reason)
                    .period(request.getPeriod())
                    .build());
        }

        System.out.println("🔍 BK Debug - Resultant: " + eligibleCount + " eligible, " + ineligibleCount + " ineligibles, total: " + details.size());

        return details;
    }
    public BkTimbreMonthlyReport buildReport(BkTimbreRequest request) {
        // Construire un rapport agrégé correspondant au template client
        java.util.Map<String, AggregatedRow> map = getAggregatedData(request != null ? request.getPeriod() : null);

        List<BkTimbreDetail> details = new ArrayList<>();
        BigDecimal totalStampDuty = BigDecimal.ZERO;
        int sumCount = 0;

        for (AggregatedRow r : map.values()) {
            BigDecimal totalRow = BigDecimal.valueOf(r.count).multiply(STAMP_DUTY_AMOUNT);
            totalStampDuty = totalStampDuty.add(totalRow);
            sumCount += r.count;

            // Mapper l'agrégation dans BkTimbreDetail pour l'affichage front
            BkTimbreDetail detail = BkTimbreDetail.builder()
                    .transactionId(r.bkName) // BK NAME
                    .checkNumber(r.invoice) // N°FACTURE
                    .paymentType(r.paymentType) // MOYEN DE PAIEMENT
                    .amount(BigDecimal.valueOf(r.count)) // NBRE TICKET > 5000 stocké dans amount
                    .stampDuty(totalRow) // TOTAL stocké dans stampDuty
                    .eligible(false)
                    .eligibilityReason("")
                    .period(request != null && request.getPeriod() != null ? request.getPeriod() : null)
                    .build();
            // utiliser period field pour stockage de JOUR CA si besoin dans l'UI
            detail.setPeriod(r.jour);
            details.add(detail);
        }

        return BkTimbreMonthlyReport.builder()
                .period(request != null && request.getPeriod() != null ? request.getPeriod() : "unknown")
                .totalStampDuty(totalStampDuty)
                .threshold(THRESHOLD)
                .fixedStampDuty(STAMP_DUTY_AMOUNT)
                .totalTransactions(sumCount)
                .eligibleTransactions(sumCount)
                .details(details)
                .build();
    }

    private boolean isBurgerKing(String value) {
        if (value == null) return false;
        String v = value.toLowerCase();
        return v.contains("burger") || v.contains("burger king") || v.contains("bk");
    }

    private boolean isCashOrGlovo(String mode) {
        if (mode == null) return false;
        String m = mode.toLowerCase();
        return m.contains("cash") || m.contains("glovo") || m.contains("espèce") || m.contains("espece");
    }

    private java.util.Map<String, AggregatedRow> getAggregatedData(String periodStr) {
        java.util.Map<String, AggregatedRow> map = new java.util.LinkedHashMap<>();
        YearMonth period = parsePeriod(periodStr);
        if (period == null) return map;

        java.time.LocalDate startDate = period.atDay(1);
        java.time.LocalDate endDate = period.atEndOfMonth();
        List<Facture> factures = factureRepository.findByDateFactureBetween(startDate, endDate);

        for (Facture facture : factures) {
            String json = facture.getDataSend();
            if (json == null || json.isBlank()) continue;
            try {
                BKExtractedData extracted = objectMapper.readValue(json, BKExtractedData.class);
                if (!isBurgerKing(extracted.getEntreprise()) && !isBurgerKing(extracted.getPointVente())) continue;

                String bkName = safe(extracted.getPointVente() != null ? extracted.getPointVente() : extracted.getEntreprise());
                String invoice = safe(extracted.getNumeroFacture());
                String jour = facture.getDateFacture() != null ? facture.getDateFacture().toString() : safe(extracted.getDateFacture());
                String mode = extracted.getTotauxPayload() != null && extracted.getTotauxPayload().getModePaiement() != null
                        ? extracted.getTotauxPayload().getModePaiement() : safe(extracted.getModePaiement());

                // RG1: Filtrage par mode de paiement
                if (!isCashOrGlovo(mode)) continue;

                // RG1: Filtrage par montant TTC de la facture (Ticket)
                int count = 0;
                if (extracted.getTotauxPayload() != null && extracted.getTotauxPayload().getTtc() != null) {
                    if (BigDecimal.valueOf(extracted.getTotauxPayload().getTtc()).compareTo(THRESHOLD) >= 0) {
                        count = 1;
                    }
                }

                if (count <= 0) continue;

                String key = bkName + "|" + invoice + "|" + jour + "|" + mode;
                AggregatedRow row = map.computeIfAbsent(key, k -> new AggregatedRow(bkName, invoice, jour, mode));
                row.count += count;
            } catch (IOException ignored) {}
        }
        return map;
    }

    @Override
    public String exportCsv(List<BkTimbreDetail> details) {
        if (details == null || details.isEmpty()) {
            return "Transaction;Type paiement;Montant;Timbre;Éligible;Raison\n";
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Transaction;Type paiement;Montant;Timbre;Éligible;Raison\n");
        for (BkTimbreDetail detail : details) {
            csv.append(safe(detail.getTransactionId())).append(";")
                    .append(safe(detail.getPaymentType())).append(";")
                    .append(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO).append(";")
                    .append(detail.getStampDuty() != null ? detail.getStampDuty() : BigDecimal.ZERO).append(";")
                    .append(detail.isEligible() ? "Oui" : "Non").append(";")
                    .append(safe(detail.getEligibilityReason())).append("\n");
        }
        return csv.toString();
    }

    @Override
    public byte[] exportExcel(List<BkTimbreDetail> details) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BK Timbre");

            // --- Définition des Styles ---
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numericStyle = workbook.createCellStyle();
            numericStyle.cloneStyleFrom(dataStyle);
            numericStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            numericStyle.setAlignment(HorizontalAlignment.RIGHT);

            // --- Entête ---
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20);
            String[] headers = {"Transaction", "Type paiement", "Montant", "Timbre", "Éligible", "Raison"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Données ---
            int rowIndex = 1;
            for (BkTimbreDetail detail : details) {
                Row row = sheet.createRow(rowIndex++);
                Cell c0 = row.createCell(0); c0.setCellValue(safe(detail.getTransactionId())); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(safe(detail.getPaymentType())); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(detail.getAmount() != null ? detail.getAmount().doubleValue() : 0); c2.setCellStyle(numericStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(detail.getStampDuty() != null ? detail.getStampDuty().doubleValue() : 0); c3.setCellStyle(numericStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(detail.isEligible() ? "Oui" : "Non"); c4.setCellStyle(dataStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(safe(detail.getEligibilityReason())); c5.setCellStyle(dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération de l'export Excel BK", e);
        }
    }

    /**
     * Génère un CSV agrégé conforme au template client pour la période fournie dans la requête.
     * L'agrégation est faite par point de vente / facture / date / moyen de paiement.
     */
    public String exportAggregatedCsv(BkTimbreRequest request) {
        StringBuilder csv = new StringBuilder();
        String header = "BK NAME;N°FACTURE;JOUR CA;MOYEN DE PAIEMENT;NBRE TICKET > 5000;MONTANT TIMBRE;TOTAL\n";
        csv.append(header);
        
        java.util.Map<String, AggregatedRow> map = getAggregatedData(request != null ? request.getPeriod() : null);

        BigDecimal grandTotal = BigDecimal.ZERO;
        int sumCount = 0;
        for (AggregatedRow r : map.values()) {
            // montant per-ticket (constant) and total per row = count * montantPerTicket
            BigDecimal montantPerTicket = STAMP_DUTY_AMOUNT;
            BigDecimal totalRow = BigDecimal.valueOf(r.count).multiply(montantPerTicket);
            csv.append(r.bkName).append(";")
                    .append(r.invoice).append(";")
                    .append(r.jour).append(";")
                    .append(r.paymentType).append(";")
                    .append(r.count).append(";")
                    .append(montantPerTicket).append(";")
                    .append(totalRow).append("\n");
            grandTotal = grandTotal.add(totalRow);
            sumCount += r.count;
        }

        // Ligne TOTAL: 'TOTAL' in BK NAME, empty invoice/jour/mode, then sumCount, unit stamp, grandTotal
        csv.append("TOTAL;;; ;");
        csv.append(sumCount).append(";").append(STAMP_DUTY_AMOUNT).append(";").append(grandTotal).append("\n");
        return csv.toString();
    }

    /**
     * Génère un fichier Excel agrégé (bytes) conforme au template client pour la période fournie.
     */
    public byte[] exportAggregatedExcel(BkTimbreRequest request) {
        String[] headers = {"BK NAME", "N°FACTURE", "JOUR CA", "MOYEN DE PAIEMENT", "NBRE TICKET > 5000", "MONTANT TIMBRE", "TOTAL"};
        java.util.Map<String, AggregatedRow> map = getAggregatedData(request != null ? request.getPeriod() : null);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BK Timbre Agrégé");

            // --- Styles ---
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numericStyle = workbook.createCellStyle();
            numericStyle.cloneStyleFrom(dataStyle);
            numericStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            numericStyle.setAlignment(HorizontalAlignment.RIGHT);

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(dataStyle);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle totalNumericStyle = workbook.createCellStyle();
            totalNumericStyle.cloneStyleFrom(numericStyle);
            totalNumericStyle.setFont(totalFont);
            totalNumericStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            totalNumericStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // --- Entête ---
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Données ---
            int rowIndex = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            int sumCount = 0;
            for (AggregatedRow r : map.values()) {
                BigDecimal montantPerTicket = STAMP_DUTY_AMOUNT;
                BigDecimal totalRow = BigDecimal.valueOf(r.count).multiply(montantPerTicket);
                Row row = sheet.createRow(rowIndex++);
                Cell c0 = row.createCell(0); c0.setCellValue(r.bkName); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(r.invoice); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(r.jour); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(r.paymentType); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(r.count); c4.setCellStyle(numericStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(montantPerTicket.doubleValue()); c5.setCellStyle(numericStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(totalRow.doubleValue()); c6.setCellStyle(numericStyle);

                grandTotal = grandTotal.add(totalRow);
                sumCount += r.count;
            }

            // --- Ligne Total Final ---
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.setHeightInPoints(20);
            Cell t0 = totalRow.createCell(0); t0.setCellValue("TOTAL GÉNÉRAL"); t0.setCellStyle(totalStyle);
            for (int i = 1; i <= 3; i++) {
                totalRow.createCell(i).setCellStyle(totalStyle);
            }
            Cell t4 = totalRow.createCell(4); t4.setCellValue(sumCount); t4.setCellStyle(totalNumericStyle);
            Cell t5 = totalRow.createCell(5); t5.setCellValue(STAMP_DUTY_AMOUNT.doubleValue()); t5.setCellStyle(totalNumericStyle);
            Cell t6 = totalRow.createCell(6); t6.setCellValue(grandTotal.doubleValue()); t6.setCellStyle(totalNumericStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération de l'export Excel BK agrégé", e);
        }
    }

    private List<Payment> loadPaymentsFromDatabase(BkTimbreRequest request) {
        if (request == null || request.getPeriod() == null) {
            return Collections.emptyList();
        }

        java.util.Map<String, AggregatedRow> aggregatedData = getAggregatedData(request.getPeriod());
        if (aggregatedData.isEmpty()) {
            return Collections.emptyList();
        }

        YearMonth period = parsePeriod(request.getPeriod());
        java.time.LocalDate startDate = period.atDay(1);
        java.time.LocalDate endDate = period.atEndOfMonth();

        System.out.println("🔍 BK Debug - Période: " + period + " (du " + startDate + " au " + endDate + ")");

        List<Facture> factures = factureRepository.findByDateFactureBetween(startDate, endDate);
        System.out.println("🔍 BK Debug - Nombre de factures trouvées: " + factures.size());

        List<Payment> payments = new ArrayList<>();
        int facturesWithDataSend = 0;
        int totalPaymentsExtracted = 0;

        for (Facture facture : factures) {
            // Charger depuis le champ dataSend (données stockées en base)
            String json = facture.getDataSend();
            if (json == null || json.isBlank()) {
                System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : dataSend null ou vide");
                continue;
            }

            facturesWithDataSend++;
            System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : dataSend présent (" + json.length() + " caractères)");

            // Afficher un exemple du JSON (premiers 300 caractères)
            if (facturesWithDataSend == 1) {
                String preview = json.length() > 300 ? json.substring(0, 300) + "..." : json;
                System.out.println("🔍 BK Debug - Exemple de JSON: " + preview);
            }

            try {
                BKExtractedData extractedData = objectMapper.readValue(json, BKExtractedData.class);
                if (extractedData != null) {
                    // Extraire les paiements depuis les lignes (lignes contient les détails des ventes)
                    List<Payment> facturePayments = extractPaymentsFromLignes(extractedData, facture.getId() != null ? facture.getId().longValue() : 0L);
                    totalPaymentsExtracted += facturePayments.size();
                    payments.addAll(facturePayments);
                    System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : " + facturePayments.size() + " paiements extraits depuis lignes");
                    System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : modePaiement=" + (extractedData.getTotauxPayload() != null ? extractedData.getTotauxPayload().getModePaiement() : "N/A") + 
                            ", ttc=" + (extractedData.getTotauxPayload() != null ? extractedData.getTotauxPayload().getTtc() : "N/A"));
                } else {
                    System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : extractedData est null");
                }
            } catch (IOException e) {
                System.out.println("🔍 BK Debug - Facture ID " + facture.getId() + " : erreur de parsing JSON: " + e.getMessage());
                throw new RuntimeException("Impossible de lire les données BK en base", e);
            }
        }

        System.out.println("🔍 BK Debug - Résumé: " + factures.size() + " factures, " + facturesWithDataSend + " avec dataSend, " + totalPaymentsExtracted + " paiements extraits");

        return payments;
    }

    private List<Payment> extractPaymentsFromLignes(BKExtractedData extractedData, Long factureId) {
        List<Payment> payments = new ArrayList<>();
        
        if (extractedData == null || extractedData.getLignes() == null || extractedData.getLignes().isEmpty()) {
            return payments;
        }

        // Créer un Payment pour le total de la facture basé sur totauxPayload
        if (extractedData.getTotauxPayload() != null) {
            Payment totalPayment = new Payment();
            totalPayment.setId(factureId);
            totalPayment.setCheckNumber(extractedData.getSheetName() != null ? extractedData.getSheetName() : "TOTAL");
            totalPayment.setAmount(extractedData.getTotauxPayload().getTtc() != null ? 
                    BigDecimal.valueOf(extractedData.getTotauxPayload().getTtc()) : BigDecimal.ZERO);
            
            // Déterminer le type de paiement
            String modePaiement = extractedData.getTotauxPayload().getModePaiement();
            if (modePaiement != null) {
                if ("cash".equalsIgnoreCase(modePaiement)) {
                    totalPayment.setPaymentType(PaymentType.CASH);
                } else if ("glovo".equalsIgnoreCase(modePaiement) || "hd_glovo".equalsIgnoreCase(modePaiement)) {
                    totalPayment.setPaymentType(PaymentType.HD_GLOVO);
                } else {
                    // Pour les autres modes de paiement, utiliser CASH_WAVE comme fallback
                    totalPayment.setPaymentType(PaymentType.CASH_WAVE);
                }
            }
            
            payments.add(totalPayment);
        }

        // Optionnel: créer un Payment pour chaque ligne si nécessaire
        // for (LineItem ligne : extractedData.getLignes()) { ... }

        return payments;
    }

    private YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeException ignored) {
            if (period != null && period.matches("\\d{4}")) {
                return YearMonth.of(Integer.parseInt(period), 1);
            }
            return null;
        }
    }

    private boolean isEligible(Payment payment, BigDecimal amount, boolean hasTotalRow) {
        if (hasTotalRow) {
            return false;
        }
        if (payment.getPaymentType() == null) {
            return false;
        }
        if (!isCashOrGlovo(payment.getPaymentType())) {
            return false;
        }
        return amount.compareTo(THRESHOLD) >= 0;
    }

    private boolean isCashOrGlovo(PaymentType paymentType) {
        return paymentType == PaymentType.CASH || paymentType == PaymentType.HD_GLOVO;
    }

    private String buildEligibilityReason(Payment payment, BigDecimal amount, boolean hasTotalRow, boolean eligible) {
        if (hasTotalRow) {
            return "Ligne de total ignorée";
        }
        if (payment.getPaymentType() == null) {
            return "Type de paiement inconnu";
        }
        if (!isCashOrGlovo(payment.getPaymentType())) {
            return "Paiement non éligible (pas Cash / Glovo)";
        }
        if (amount.compareTo(THRESHOLD) < 0) {
            return "Montant inférieur au seuil de 5000 FCFA";
        }
        return eligible ? "Éligible au droit de timbre" : "Non éligible";
    }

    private String safe(String value) {
        return value != null ? value.replaceAll("[\r\n;]", " ") : "";
    }

    // Classe interne utilitaire pour stocker les lignes agrégées avant export
    private static class AggregatedRow {
        String bkName;
        String invoice;
        String jour;
        String paymentType;
        int count = 0;
        BigDecimal stampTotal = BigDecimal.ZERO;

        AggregatedRow(String bkName, String invoice, String jour, String paymentType) {
            this.bkName = bkName;
            this.invoice = invoice;
            this.jour = jour;
            this.paymentType = paymentType;
        }
    }
}
