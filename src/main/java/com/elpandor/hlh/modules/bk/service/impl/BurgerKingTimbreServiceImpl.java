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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
        List<BkTimbreDetail> details = calculateDetails(request);
        BigDecimal totalStampDuty = details.stream()
                .map(BkTimbreDetail::getStampDuty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long eligibleTransactions = details.stream().filter(BkTimbreDetail::isEligible).count();
        long totalTransactions = details.size();

        return BkTimbreMonthlyReport.builder()
                .period(request != null && request.getPeriod() != null ? request.getPeriod() : "unknown")
                .totalStampDuty(totalStampDuty)
                .threshold(THRESHOLD)
                .fixedStampDuty(STAMP_DUTY_AMOUNT)
                .totalTransactions(totalTransactions)
                .eligibleTransactions(eligibleTransactions)
                .details(details)
                .build();
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

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Transaction", "Type paiement", "Montant", "Timbre", "Éligible", "Raison"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (BkTimbreDetail detail : details) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safe(detail.getTransactionId()));
                row.createCell(1).setCellValue(safe(detail.getPaymentType()));
                row.createCell(2).setCellValue(detail.getAmount() != null ? detail.getAmount().doubleValue() : 0);
                row.createCell(3).setCellValue(detail.getStampDuty() != null ? detail.getStampDuty().doubleValue() : 0);
                row.createCell(4).setCellValue(detail.isEligible() ? "Oui" : "Non");
                row.createCell(5).setCellValue(safe(detail.getEligibilityReason()));
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

    private List<Payment> loadPaymentsFromDatabase(BkTimbreRequest request) {
        if (request == null || request.getPeriod() == null) {
            return Collections.emptyList();
        }

        YearMonth period = parsePeriod(request.getPeriod());
        if (period == null) {
            return Collections.emptyList();
        }

        Instant start = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        System.out.println("🔍 BK Debug - Période: " + period + " (du " + start + " au " + end + ")");

        List<Facture> factures = factureRepository.findBkFacturesByDateCreationBetween(start, end);
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
}
