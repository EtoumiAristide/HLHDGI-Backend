package com.elpandor.hlh.modules.automatisation.service;

import com.elpandor.hlh.modules.automatisation.model.zino.Detail;
import com.elpandor.hlh.modules.automatisation.model.zino.Paiement;
import com.elpandor.hlh.modules.automatisation.model.zino.TicketVente;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZinoCsvParserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<TicketVente> parseCsvFile(MultipartFile file) throws Exception {
        List<TicketVente> tickets = new ArrayList<>();
        TicketVente currentTicket = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 🔥 Méthode robuste pour supprimer le BOM et autres caractères invisibles
                String cleanLine = line.replaceFirst("^\\uFEFF", "")
                        .replaceFirst("^\\ufeff", "")
                        .trim();

                String[] columns = cleanLine.split(";");
                if (columns.length < 2) {
                    continue;
                }

                String type = columns[0].trim();

                switch (type) {
                    case "CLIENT":
                        // Nouveau ticket client
                        currentTicket = parseClientLine(columns);
                        tickets.add(currentTicket);
                        break;
                    case "DETAILS":
                        if (currentTicket != null) {
                            Detail detail = parseDetailLine(columns);
                            currentTicket.getDetails().add(detail);
                        }
                        break;
                    case "PAIMENT":
                        if (currentTicket != null) {
                            Paiement paiement = parsePaiementLine(columns);
                            currentTicket.setPaiement(paiement);
                        }
                        break;
                    default:
                        // Ignorer les autres types
                        break;
                }
            }
        }

        return tickets;
    }

    private TicketVente parseClientLine(String[] columns) {
        TicketVente ticket = new TicketVente();
        ticket.setDate(LocalDate.parse(columns[1].trim(), DATE_FORMATTER));
        ticket.setClient(columns[2].trim());

        if (columns.length > 3 && columns[3] != null && !columns[3].isEmpty()) {
            ticket.setNomClient(columns[3].trim());
        }

        if (columns.length > 4 && columns[4] != null && !columns[4].isEmpty()) {
            ticket.setPrenomClient(columns[4].trim());
        }

        // colonne 7 = numéro de ticket (index 7 car 0-based)
        if (columns.length > 7) {
            ticket.setNumeroTicket(columns[7].trim());
        }

        // colonne 8 = code client (index 8)
        if (columns.length > 8) {
            ticket.setCodeClient(columns[8].trim());
        }

        return ticket;
    }

    private Detail parseDetailLine(String[] columns) {
        Detail detail = new Detail();
        detail.setDate(LocalDate.parse(columns[1].trim(), DATE_FORMATTER));
        detail.setReference(columns[2].trim());
        detail.setDescription(columns[3].trim());
        detail.setQuantite(new BigDecimal(columns[4].trim()));
        detail.setPrixUnitaire(new BigDecimal(columns[5].trim()));
        detail.setTauxTVA(Integer.parseInt(columns[6].trim()));
        detail.setMontantTotal(new BigDecimal(columns[7].trim()));

        if (columns.length > 8) {
            detail.setNumTicket(Integer.parseInt(columns[8].trim()));
        }

        if (columns.length > 9) {
            detail.setCodeClient(columns[9].trim());
        }

        return detail;
    }

    private Paiement parsePaiementLine(String[] columns) {
        Paiement paiement = new Paiement();
        paiement.setDate(LocalDate.parse(columns[1].trim(), DATE_FORMATTER));
        paiement.setCodePaiement(columns[2].trim());
        paiement.setQuantite(Integer.parseInt(columns[3].trim()));
        paiement.setMoyenPaiement(columns[4].trim());
        paiement.setMontant(new BigDecimal(columns[5].trim()));

        if (columns.length > 8) {
            paiement.setNumTicket(Integer.parseInt(columns[8].trim()));
        }

        if (columns.length > 9) {
            paiement.setCodeClient(columns[9].trim());
        }

        return paiement;
    }
}
