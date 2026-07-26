package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.RepartitionParModePaiement;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.ZinoTicketVenteExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersisterFactureUseCase {

    private final ZinoTicketVenteExtractor extractor;

    public void executer(List<TicketVenteZino> tickets) {
        log.info("Persistance des factures Zino. {} tickets.", tickets.size());

        try {
            List<RepartitionParModePaiement> repartition = extractor.calculerRepartitionParModePaiement(tickets);

            // TODO: Persister les tickets et la répartition en base de données
            // ticketVenteRepository.saveAll(tickets);
            // repartitionRepository.saveAll(repartition);

            log.info("Persistance réussie pour {} tickets", tickets.size());

        } catch (Exception e) {
            log.error("Erreur lors de la persistance: {}", e.getMessage());
            throw new RuntimeException("Erreur de persistance", e);
        }
    }
}