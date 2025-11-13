package com.elpandor.hlh.modules.hlh.utils;

import com.elpandor.hlh.modules.hlh.model.dto.payload.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ExcelFactureExtractor {

    public List<FacturePayload> extractFacture(InputStream is, Integer indexLectureFichier) throws IOException {

        List<FacturePayload> factures = new ArrayList<>();

        Workbook workbook = new XSSFWorkbook(is);

        /*int indexLecture = switch (entreprise) {
            case "HOTEL AND LUXURY HOUSING" -> 0;
            case "SIA RESTAURATION RAPIDE COTE D'IVOIRE", "BK AEROPORT" -> 2;
            default -> 0;
        };*/

        for (int i = indexLectureFichier; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i); // Première feuille
            FacturePayload facture = new FacturePayload();

            facture.setSheetName(sheet.getSheetName());
            // Extraction des informations de base
            extractHeaderInfo(sheet, facture);

            // Extraction des informations client
            extractClientInfo(sheet, facture);

            // Extraction des lignes de produits
            extractLignesProduits(sheet, facture);

            // Extraction des totaux
            extractTotaux(sheet, facture);

            // Extraction de la mention de réception
            extractReception(sheet, facture);

            factures.add(facture);
        }

        return factures;
    }

    private void extractHeaderInfo(Sheet sheet, FacturePayload facture) {
        // Recherche dynamique du numéro de facture
        boolean numFactureTrouve = false;
        boolean dateFactureTrouve = false;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && cellValue.startsWith("N° FACTURE :")) {
                        facture.setNumeroFacture(cellValue.replace("N° FACTURE :", "").trim());
                        //return;
                        numFactureTrouve = true;
                    }
                    if (cellValue != null && cellValue.startsWith("DATE :")) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        facture.setDateFacture(cellValue.replace("DATE :", "").trim());
                        //return;
                        dateFactureTrouve = true;
                    }
                }
            }
            if (numFactureTrouve && dateFactureTrouve) return;
        }
    }

    private void extractClientInfo(Sheet sheet, FacturePayload facture) {
        ClientPayload client = new ClientPayload();
        boolean foundClientSection = false;

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    //System.out.println("cellValue "+cellValue);
                    if ("Information Client".equalsIgnoreCase(cellValue.trim())) {
                        foundClientSection = true;
                        continue;
                    }

                    if (foundClientSection) {
                        if ("SOCIETE".equalsIgnoreCase(cellValue.trim()) || "CLIENT".equalsIgnoreCase(cellValue.trim()) || "SOCIETE / CLIENT".equalsIgnoreCase(cellValue.trim())) {
                            // Le nom du client est dans la cellule suivante
                            Cell clientCell = row.getCell(cell.getColumnIndex() + 2);
                            if (clientCell != null && !clientCell.getStringCellValue().trim().isEmpty()) {
//                                System.out.println("clientCell.getStringCellValue() "+clientCell.getStringCellValue());
                                client.setNom(clientCell.getStringCellValue().trim());
                            }
                        } else if ("N°CC".equalsIgnoreCase(cellValue.trim())) {
                            // Le numéro CC est dans la cellule suivante
                            Cell ccCell = row.getCell(cell.getColumnIndex() + 2);
                            if (ccCell != null && !ccCell.getStringCellValue().trim().isEmpty()) {
                                client.setNumeroCC(ccCell.getStringCellValue().trim());
                            }
                            // On sort après avoir trouvé le numéro CC
                            facture.setClientPayload(client);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void extractLignesProduits(Sheet sheet, FacturePayload facture) {
        List<LigneProduitPayload> lignes = new ArrayList<>();
        boolean foundProductHeader = false;
        int productStartRow = -1;

        // Recherche de l'en-tête "Produit / Service"
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING &&
                        "Produit / Service".equalsIgnoreCase(cell.getStringCellValue().trim())) {
                    foundProductHeader = true;
                    productStartRow = row.getRowNum() + 1; // Ligne suivante est le début des produits
                    break;
                }
            }
            if (foundProductHeader) break;
        }

//        System.out.println("foundProductHeader "+foundProductHeader);

        if (!foundProductHeader) {
//            System.out.println();
            facture.setLignes(lignes);
            return;
        }
        // Extraction des produits jusqu'à trouver une ligne vide ou la section des totaux
        for (int i = productStartRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Vérifie si on a atteint la section des totaux
            boolean isTotalSection = false;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && ("H.T".equalsIgnoreCase(cell.getStringCellValue().trim()) || "Montant TTC".equalsIgnoreCase(cell.getStringCellValue().trim()))) {
                    isTotalSection = true;
                    break;
                }
            }
            if (isTotalSection) break;

            // Vérifie si la ligne contient un produit
            Cell productCell = row.getCell(1); // Colonne B (Produit / Service)
//            System.out.println("productCell "+productCell);
            if (productCell.getCellType() == CellType.BLANK) {
                continue; // Ligne vide
            }

            LigneProduitPayload ligne = new LigneProduitPayload();

            // Date (colonne A)
            Cell dateCell = row.getCell(0);
            if (dateCell != null) {
//                System.out.println("dateCell " + dateCell.toString());
                if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    ligne.setDate(dateCell.getDateCellValue().toString());
                } else if (dateCell.getCellType() == CellType.STRING) {
                    ligne.setDate(dateCell.getStringCellValue().trim());
                }
            }

            // Produit (colonne B)
            if (productCell.getCellType() == CellType.STRING) {
                ligne.setProduit(productCell.getStringCellValue().trim());
            }

            // Quantité (colonne C)
            Cell qteCell = row.getCell(2);
//            System.out.println("qteCell "+qteCell);
            if (qteCell != null) {
//            System.out.println("qteCell.getCellType() "+qteCell.getCellType());
                if (qteCell.getCellType() == CellType.NUMERIC || qteCell.getCellType() == CellType.FORMULA) {
                    ligne.setQuantite((int) qteCell.getNumericCellValue());
                } else if (qteCell.getCellType() == CellType.STRING || qteCell.getCellType() == CellType.FORMULA) {
                    try {
                        ligne.setQuantite(Integer.parseInt(qteCell.getStringCellValue()));
                    } catch (NumberFormatException e) {
                        ligne.setQuantite(0);
                    }
                }
            }

            // Prix unitaire HT (colonne D)
            Cell prixCell = row.getCell(3);
//            System.out.println("prixCell " + prixCell);
            if (prixCell != null) {
                if (prixCell.getCellType() == CellType.NUMERIC) {
                    ligne.setPrixUnitaireHT(prixCell.getNumericCellValue());
                } else if (prixCell.getCellType() == CellType.STRING) {
                    try {
                        ligne.setPrixUnitaireHT(Double.parseDouble(prixCell.getStringCellValue()));
                    } catch (NumberFormatException e) {
                        ligne.setPrixUnitaireHT(0);
                    }
                }
            }

            // Montant HT (colonne E)
            Cell montantCell = row.getCell(4);
            if (montantCell != null) {
                if (montantCell.getCellType() == CellType.NUMERIC) {
                    ligne.setMontantHT(montantCell.getNumericCellValue());
                } else if (montantCell.getCellType() == CellType.FORMULA) {
                    try {
                        ligne.setMontantHT(montantCell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        ligne.setMontantHT(0);
                    }
                }
            }

            lignes.add(ligne);
        }

        facture.setLignes(lignes);
    }

    private void extractTotaux(Sheet sheet, FacturePayload facture) {
        TotauxPayload totaux = new TotauxPayload();
        TaxePayload tdt = new TaxePayload();
        TaxePayload tva = new TaxePayload();
        boolean foundTotalSection = false;

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue().trim();
                    DecimalFormat decimalFormat = new DecimalFormat("#");

                    if ("H.T".equalsIgnoreCase(cellValue)) {
                        foundTotalSection = true;
                        // Montant HT (colonne E)
                        Cell htCell = row.getCell(4);
                        if (htCell != null && (htCell.getCellType() == CellType.NUMERIC || htCell.getCellType() == CellType.FORMULA)) {
                            totaux.setHt(Double.parseDouble(decimalFormat.format(htCell.getNumericCellValue())));
                        }
                    } else if ("TDT".equalsIgnoreCase(cellValue)) {
                        // Base TDT (colonne D)
                        Cell baseCell = row.getCell(3);
                        if (baseCell != null && (baseCell.getCellType() == CellType.NUMERIC || baseCell.getCellType() == CellType.FORMULA)) {
                            tdt.setBase(Double.parseDouble(decimalFormat.format(baseCell.getNumericCellValue())));
                        }
                        // Montant TDT (colonne E)
                        Cell montantCell = row.getCell(4);
                        if (montantCell != null && (montantCell.getCellType() == CellType.NUMERIC || montantCell.getCellType() == CellType.FORMULA)) {
                            tdt.setMontant(Double.parseDouble(decimalFormat.format(montantCell.getNumericCellValue())));
                        }
                    } else if (cellValue.startsWith("TVA")) {
                        // Taux TVA (extrait du libellé)
//                        if (cellValue.contains("18.00%")) {
//                            tva.setTaux(18.0);
//                        }
                        // Base TVA (colonne D)
                        Cell baseCell = row.getCell(3);
                        if (baseCell != null && (baseCell.getCellType() == CellType.NUMERIC || baseCell.getCellType() == CellType.FORMULA)) {
                            tva.setBase(Double.parseDouble(decimalFormat.format(baseCell.getNumericCellValue())));
                        }
                        // Montant TVA (colonne E)
                        Cell montantCell = row.getCell(4);
                        if (montantCell != null && (montantCell.getCellType() == CellType.NUMERIC || montantCell.getCellType() == CellType.FORMULA)) {
                            tva.setMontant(Double.parseDouble(decimalFormat.format(montantCell.getNumericCellValue())));
                        }
                    } else if ("Montant TTC".equalsIgnoreCase(cellValue)) {
                        // Montant TTC (colonne E)
                        Cell ttcCell = row.getCell(4);
                        if (ttcCell != null && (ttcCell.getCellType() == CellType.NUMERIC || ttcCell.getCellType() == CellType.FORMULA)) {
                            totaux.setTtc(Double.parseDouble(decimalFormat.format(ttcCell.getNumericCellValue())));
                        }
                    } else if ("Mode de paiement".equalsIgnoreCase(cellValue)) {
                        // Mode de paiement (colonne D)
                        Cell modeCell = row.getCell(3);
                        if (modeCell != null && modeCell.getCellType() == CellType.STRING) {
                            totaux.setModePaiement(modeCell.getStringCellValue().trim());
                        }
                    }
                }
            }
        }

        //Calcul du pourcentage de TDT
        tdt.setTaux(Double.valueOf(new DecimalFormat("#0.00").format((tdt.getMontant() * 100) / tdt.getBase()).replace(',', '.')));
        totaux.setTdt(tdt);

        //Calcul du pourcentage de TVA
        tva.setTaux(Double.valueOf(new DecimalFormat("#0").format((tva.getMontant() * 100) / tva.getBase()).replace(',', '.')));
        totaux.setTva(tva);

        facture.setTotauxPayload(totaux);
    }

    private void extractReception(Sheet sheet, FacturePayload facture) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING &&
                        "La Reception".equalsIgnoreCase(cell.getStringCellValue().trim())) {
                    facture.setReception(cell.getStringCellValue().trim());
                    return;
                }
            }
        }
    }
}
