package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.BKExtractedData;
import com.elpandor.hlh.modules.hlh.model.dto.payload.Comp;
import com.elpandor.hlh.modules.hlh.model.dto.payload.Payment;
import com.elpandor.hlh.modules.hlh.model.dto.payload.Promo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExcelDataExtraction {
    public BKExtractedData extractDataFromExcel(MultipartFile file) throws IOException {

        BKExtractedData extractedData = new BKExtractedData();

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // Première feuille

        extractedData.setPayments(extractPayments(sheet));
        extractedData.setComps(extractComps(sheet));
        extractedData.setPromos(extractPromos(sheet));

        workbook.close();

        return extractedData;
    }

    private List<Payment> extractPayments(Sheet sheet) {
        List<Payment> payments = new ArrayList<>();
        boolean inCashSection = false;
        boolean inBackupCCSection = false;
        boolean inHDGlovoSection = false;
        boolean inCashWaveSection = false;

        for (Row row : sheet) {
            String firstCellValue = getCellStringValue(row.getCell(1));

            // Détection des sections
            if (firstCellValue != null) {
                if (firstCellValue.contains("*********  Cash   *********")) {
                    inCashSection = true;
                    inBackupCCSection = false;
                    inHDGlovoSection = false;
                    inCashWaveSection = false;
                    continue;
                } else if (firstCellValue.contains("*********  Backup CC   *********")) {
                    inCashSection = false;
                    inBackupCCSection = true;
                    inHDGlovoSection = false;
                    inCashWaveSection = false;
                    continue;
                } else if (firstCellValue.contains("*********  HD Glovo   *********")) {
                    inCashSection = false;
                    inBackupCCSection = false;
                    inHDGlovoSection = true;
                    inCashWaveSection = false;
                    continue;
                } else if (firstCellValue.contains("*********  Cash Wave   *********")) {
                    inCashSection = false;
                    inBackupCCSection = false;
                    inHDGlovoSection = false;
                    inCashWaveSection = true;
                    continue;
                }
            }

            // Extraction des données de paiement
            if ((inCashSection || inBackupCCSection || inHDGlovoSection || inCashWaveSection)
                    && isPaymentDataRow(row)) {

                Payment payment = new Payment();
                payment.setCheckNumber(getCellStringValue(row.getCell(1)));
                payment.setCardNumber(getCellStringValue(row.getCell(2)));
                payment.setExp(getCellStringValue(row.getCell(3)));
                payment.setQty(getCellIntegerValue(row.getCell(4)));
                payment.setAmount(getCellBigDecimalValue(row.getCell(5)));
                payment.setTip(getCellBigDecimalValue(row.getCell(7)));
                payment.setTotal(getCellBigDecimalValue(row.getCell(8)));
                payment.setEmp(getCellStringValue(row.getCell(9)));

                // Détermination du type de paiement
                if (inCashSection) {
                    payment.setPaymentType(Payment.PaymentType.CASH);
                } else if (inBackupCCSection) {
                    payment.setPaymentType(Payment.PaymentType.BACKUP_CC);
                } else if (inHDGlovoSection) {
                    payment.setPaymentType(Payment.PaymentType.HD_GLOVO);
                } else if (inCashWaveSection) {
                    payment.setPaymentType(Payment.PaymentType.CASH_WAVE);
                }

                payments.add(payment);
            }
        }

        //paymentRepository.saveAll(payments);
        log.info("Extracted {} payments", payments.size());
        return payments;
    }

    private List<Comp> extractComps(Sheet sheet) {
        List<Comp> comps = new ArrayList<>();
        boolean inStaffMealsSection = false;
        boolean inManagerMealsSection = false;
        boolean inGuestTrackSection = false;

        for (Row row : sheet) {
            String firstCellValue = getCellStringValue(row.getCell(1));

            // Détection des sections Comps
            if (firstCellValue != null) {
                if (firstCellValue.contains("*********  ABJ Staff Meals   *********")) {
                    inStaffMealsSection = true;
                    inManagerMealsSection = false;
                    inGuestTrackSection = false;
                    continue;
                } else if (firstCellValue.contains("*********  ABJ MNGR Meals   *********")) {
                    inStaffMealsSection = false;
                    inManagerMealsSection = true;
                    inGuestTrackSection = false;
                    continue;
                } else if (firstCellValue.contains("*********  Guest Track   *********")) {
                    inStaffMealsSection = false;
                    inManagerMealsSection = false;
                    inGuestTrackSection = true;
                    continue;
                }
            }

            // Extraction des données Comps
            if ((inStaffMealsSection || inManagerMealsSection || inGuestTrackSection)
                    && isCompDataRow(row)) {

                Comp comp = new Comp();
                comp.setChkNumber(getCellStringValue(row.getCell(1)));
                comp.setTime(getCellStringValue(row.getCell(2)));
                comp.setNameItem(getCellStringValue(row.getCell(3)));
                comp.setUnit(getCellStringValue(row.getCell(4)));
                comp.setQty(getCellIntegerValue(row.getCell(5)));
                comp.setAmount(getCellBigDecimalValue(row.getCell(6)));
                comp.setPercentTot(getCellBigDecimalValue(row.getCell(7)));
                comp.setEmp(getCellStringValue(row.getCell(8)));
                comp.setMgr(getCellStringValue(row.getCell(9)));

                // Détermination du type de comp
                if (inStaffMealsSection) {
                    comp.setCompType(Comp.CompType.STAFF_MEALS);
                } else if (inManagerMealsSection) {
                    comp.setCompType(Comp.CompType.MANAGER_MEALS);
                } else if (inGuestTrackSection) {
                    comp.setCompType(Comp.CompType.GUEST_TRACK);
                }

                comps.add(comp);
            }
        }

        //compRepository.saveAll(comps);
        log.info("Extracted {} comps", comps.size());

        return comps;
    }

    private List<Promo> extractPromos(Sheet sheet) {
        List<Promo> promos = new ArrayList<>();
        // Implémentation similaire pour les promotions...
        // (Le code serait structuré de la même manière que pour les paiements et comps)

//        promoRepository.saveAll(promos);
        log.info("Extracted {} promos", promos.size());

        return promos;
    }

    // Méthodes utilitaires
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getCellIntegerValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String value = cell.getStringCellValue().replace(",", "").trim();
                    return new BigDecimal(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private boolean isPaymentDataRow(Row row) {
        // Vérifie si la ligne contient des données de paiement valides
        String checkNumber = getCellStringValue(row.getCell(1));
        BigDecimal amount = getCellBigDecimalValue(row.getCell(5));
        return checkNumber != null && !checkNumber.isEmpty() &&
                !checkNumber.contains("---") && amount != null;
    }

    private boolean isCompDataRow(Row row) {
        // Vérifie si la ligne contient des données comps valides
        String chkNumber = getCellStringValue(row.getCell(1));
        BigDecimal amount = getCellBigDecimalValue(row.getCell(6));
        return chkNumber != null && !chkNumber.isEmpty() &&
                !chkNumber.contains("---") && amount != null;
    }
}
