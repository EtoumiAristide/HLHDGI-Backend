package com.elpandor.hlh.modules.automatisation.service;

import com.elpandor.hlh.modules.automatisation.model.zino.TicketVente;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.FacturePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketToFactureTransformer transformer;

    /**
     * Transforme un ticket en facture
     */
    public FacturePayload transformTicketToFacture(TicketVente ticket) {
        log.info("Transformation du ticket {} en facture", ticket.getNumeroTicket());
        return transformer.transform(ticket);
    }

    /**
     * Transforme tous les tickets en factures
     */
    public List<FacturePayload> transformTicketsToFactures(List<TicketVente> tickets) {
        log.info("Transformation de {} tickets en factures", tickets.size());
        return transformer.transformAll(tickets);
    }

    /**
     * Transforme et affiche les résultats
     */
    public void processAndDisplayTickets(List<TicketVente> tickets) {
        List<FacturePayload> factures = transformTicketsToFactures(tickets);

        factures.forEach(facture -> {
            log.info("=== FACTURE {} ===", facture.getNumeroFacture());
            log.info("Client: {}", facture.getClientPayload().getNom());
            log.info("Date: {}", facture.getDateFacture());
            log.info("Type client: {}", facture.getTypeClient());
            log.info("Mode paiement: {}", facture.getModePaiement());
            log.info("Nombre de produits: {}", facture.getLignes().size());
            log.info("HT: {}", facture.getTotauxPayload().getHt());
            log.info("TVA: {}", facture.getTotauxPayload().getTva().getMontant());
            log.info("TDT: {}", facture.getTotauxPayload().getTdt().getMontant());
            log.info("TCN: {}", facture.getTotauxPayload().getTcn().getMontant());
            log.info("TTC: {}", facture.getTotauxPayload().getTtc());
            log.info("-----------------------------------");
        });

        log.info("✅ Total factures transformées: {}", factures.size());
    }
}
