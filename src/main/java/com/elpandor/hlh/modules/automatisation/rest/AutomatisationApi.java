package com.elpandor.hlh.modules.automatisation.rest;

import com.elpandor.hlh.modules.automatisation.model.zino.TicketVente;
import com.elpandor.hlh.modules.automatisation.service.TicketService;
import com.elpandor.hlh.modules.automatisation.service.ZinoCsvParserService;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/automatisation")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AutomatisationApi {

    private Logger log = LoggerFactory.getLogger(AutomatisationApi.class);

    private final ZinoCsvParserService zinoCsvParserService;
    private final TicketService ticketService;


    @PostMapping
    public ResponseEntity<List<FacturePayload>> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Parser le fichier CSV
            List<TicketVente> tickets = zinoCsvParserService.parseCsvFile(file);
            log.info("✅ {} tickets extraits du fichier", tickets.size());

            // Afficher les résultats dans la console
            /*tickets.forEach(ticket -> {
                System.out.println("=== TICKET ===");
                System.out.println("Client: " + ticket.getClient() + " - " + ticket.getNumeroTicket());
                System.out.println("Date: " + ticket.getDate());
                System.out.println("Nombre d'articles: " + ticket.getDetails().size());
                if (ticket.getPaiement() != null) {
                    System.out.println("Paiement: " + ticket.getPaiement().getMoyenPaiement() +
                            " - " + ticket.getPaiement().getMontant());
                }
                ticket.getDetails().forEach(detail -> {
                    System.out.println("  - " + detail.getReference() + " | " +
                            detail.getDescription() + " | " + detail.getQuantite() + "x");
                });
                System.out.println();
            });*/

            // 2. Transformer les tickets en factures
            List<FacturePayload> factures = ticketService.transformTicketsToFactures(tickets);

            // 3. Afficher les résultats
            ticketService.processAndDisplayTickets(tickets);

            return ResponseEntity.ok(factures);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
