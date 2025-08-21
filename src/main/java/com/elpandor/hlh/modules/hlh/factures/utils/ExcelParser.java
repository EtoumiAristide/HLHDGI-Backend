package com.elpandor.hlh.modules.hlh.factures.utils;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ExcelParser {
    // add logger as per the need
    private static final Logger logger = LoggerFactory.getLogger(ExcelParser.class);

    public static void parseExcelFile(InputStream is) throws IOException {
        Workbook workbook = WorkbookFactory.create(is);

        Sheet sheet = workbook.getSheetAt(0);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue; // Skip header row
            }

            System.out.println(getCellValueAsString(row.getCell(0)));
            System.out.println(getCellValueAsString(row.getCell(1)));
            System.out.println(getCellValueAsString(row.getCell(2)));
            System.out.println(getCellValueAsString(row.getCell(3)));
            System.out.println(getCellValueAsString(row.getCell(4)));
            // Add more fields as needed

        }

        workbook.close();
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getRichStringCellValue().getString().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Adjust date format if needed
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getRichStringCellValue().getString().trim());
                } catch (NumberFormatException e) {
                    return null; // Handle if the string cannot be parsed as double
                }
            default:
                return null;
        }
    }
}
