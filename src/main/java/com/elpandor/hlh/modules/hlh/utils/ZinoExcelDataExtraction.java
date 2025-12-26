package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.zino.ZinoExtractedData;
import com.elpandor.hlh.modules.hlh.model.dto.payload.zino.ZinoExtractedDataOrdered;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ZinoExcelDataExtraction {

    public List<ZinoExtractedData> extractFacture(InputStream is, Integer indexLectureFichier) throws IOException {
        List<ZinoExtractedData> importedRecords = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(indexLectureFichier);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    if (row.getCell(2).getCellType() != CellType.FORMULA && getCellStringValue(row.getCell(2)) != null && !getCellStringValue(row.getCell(2)).isEmpty()) {
                        ZinoExtractedData zinoExtractedData = new ZinoExtractedData();

                        // Lecture des cellules
                        zinoExtractedData.setDate(row.getCell(0).getDateCellValue());
                        zinoExtractedData.setCaisse(getCellStringValue(row.getCell(1)));
                        zinoExtractedData.setModePaiement(getCellStringValue(row.getCell(2)));
                        zinoExtractedData.setMontantHT(getCellNumericValue(row.getCell(3)));
                        zinoExtractedData.setTva(getCellNumericValue(row.getCell(4)));
                        zinoExtractedData.setMontantTTC(getCellNumericValue(row.getCell(5)));
                        zinoExtractedData.setSheetName(sheet.getSheetName());

                        importedRecords.add(zinoExtractedData);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier Excel", e);
        }
        //System.out.println(importedRecords);
        //System.out.println(calculerRepartitionParModePaiement(importedRecords));
//        Map<String, List<Object>> response = new HashMap<>();
//        response.put("ZinoExtractedData", Collections.singletonList(importedRecords));
//        response.put("ZinoExtractedDataOrdered", Collections.singletonList(calculerRepartitionParModePaiement(importedRecords)));

        return importedRecords;

    }

    public List<ZinoExtractedDataOrdered> calculerRepartitionParModePaiement(List<ZinoExtractedData> transactions) {
        Map<String, ZinoExtractedDataOrdered> repartitionMap = new HashMap<>();

        for (ZinoExtractedData transaction : transactions) {
            if (transaction.getModePaiement() != null && transaction.getModePaiement().equalsIgnoreCase("glovo")) {
                transaction.setModePaiement("Espèces Franc CFA");
            }
            String modePaiement = transaction.getModePaiement();

            ZinoExtractedDataOrdered dto = repartitionMap.getOrDefault(modePaiement,
                    new ZinoExtractedDataOrdered());
            dto.setDate(transaction.getDate());
            dto.setModePaiement(modePaiement);
            dto.setTotalMontantTTC(dto.getTotalMontantTTC() != null ? dto.getTotalMontantTTC() + transaction.getMontantTTC() : transaction.getMontantTTC());
            dto.setTotalMontantHT(dto.getTotalMontantHT() != null ? dto.getTotalMontantHT() + transaction.getMontantHT() : transaction.getMontantHT());
            dto.setTotalTVA(dto.getTotalTVA() != null ? dto.getTotalTVA() + transaction.getTva() : transaction.getTva());
            dto.setNombreTransactions(dto.getNombreTransactions() + 1);
            dto.setSheetName(transaction.getSheetName());

            repartitionMap.put(modePaiement, dto);
        }

        return new ArrayList<>(repartitionMap.values());
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return cell.getStringCellValue();
    }

    private Double getCellNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        return cell.getNumericCellValue();
    }
}
