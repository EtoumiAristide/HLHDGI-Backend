package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.BKExtratedData2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BKExcelDataExtraction2 {
    public Map<String, List<BKExtratedData2>> extractAllSheetsData(InputStream is) {

        Map<String, List<BKExtratedData2>> map = new HashMap<>();
        try {
            Workbook workbook = new XSSFWorkbook(is);

            int numberOfSheets = workbook.getNumberOfSheets();

            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<BKExtratedData2> sheetData = extractSheetData(sheet);

                map.put(sheet.getSheetName(), sheetData);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier Excel", e);
        }
        return map;
    }

    private List<BKExtratedData2> extractSheetData(Sheet sheet) {
        List<BKExtratedData2> transactions = new ArrayList<>();
        // Commencer à la ligne 5 (index 4) pour ignorer les en-têtes et les métadonnées
        for (int rowIndex = 4; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            // Vérifier si la ligne est vide (première cellule vide)
            Cell secondCell = row.getCell(1);
            if (secondCell == null || secondCell.getCellType() == CellType.BLANK) {
                // Si la ligne est vide, on peut continuer ou arrêter selon votre logique
                continue;
            }

            BKExtratedData2 dto = new BKExtratedData2();

            //Date
            dto.setDate(getCellStringValue(sheet.getRow(1).getCell(0)));

            // Colonne A : Référence
            dto.setReference(getCellStringValue(row.getCell(0)));

            // Colonne B : Check #
            dto.setCheckNumber(getCellStringValue(row.getCell(1)));

            // Colonne C : HT (formule ou valeur)
            dto.setHt(getCellNumericValue(row.getCell(2)));

            // Colonne D : TDT (formule ou valeur)
            dto.setTdt(getCellNumericValue(row.getCell(3)));

            // Colonne E : TVA (formule ou valeur)
            dto.setTva(getCellNumericValue(row.getCell(4)));

            // Colonne F : Amount TTC (valeur)
            dto.setAmountTTC(getCellNumericValue(row.getCell(5)));

            // Ajouter uniquement si nous avons au moins une donnée
            if (dto.getReference() != null || dto.getCheckNumber() != null ||
                    dto.getAmountTTC() != null) {
                transactions.add(dto);
            }
        }
        return transactions;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default:
                return null;
        }
    }

    private Double getCellNumericValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC, FORMULA:
                    return cell.getNumericCellValue();
                case STRING:
                    String str = cell.getStringCellValue().trim();
                    if (str.isEmpty()) return null;
                    return Double.parseDouble(str);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
