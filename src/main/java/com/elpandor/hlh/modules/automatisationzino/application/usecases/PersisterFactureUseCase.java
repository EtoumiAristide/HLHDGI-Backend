package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.domain.repository.RepartitionPaiementRepository;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.TicketVenteRepository;
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
    private final TicketVenteRepository ticketVenteRepository;
    private final RepartitionPaiementRepository repartitionPaiementRepository;

    /**
     * Persiste les tickets de vente extraits ainsi que leur répartition par mode de paiement.
     *
     * @param tickets         tickets extraits du fichier CSV Zino
     * @param nomFichierSource nom du fichier source, pour rattacher les tickets à leur origine
     */
    public void executer(List<TicketVenteZino> tickets, String nomFichierSource) {
        log.info("Persistance des factures Zino. {} tickets.", tickets.size());

        if (tickets == null || tickets.isEmpty()) {
            log.warn("Aucun ticket à persister");
            return;
        }

        try {
            // 1. Persistance des tickets bruts, rattachés au fichier source
            List<TicketVenteZino> ticketsPersistes = ticketVenteRepository.saveAll(tickets, nomFichierSource);
            log.info("{} tickets persistés en base", ticketsPersistes.size());

            // 2. Calcul et persistance de la répartition par mode de paiement
            List<RepartitionParModePaiement> repartition = extractor.calculerRepartitionParModePaiement(tickets);
            List<RepartitionParModePaiement> repartitionPersistee = repartitionPaiementRepository.saveAll(repartition);
            log.info("{} lignes de répartition persistées en base", repartitionPersistee.size());

        } catch (Exception e) {
            log.error("Erreur lors de la persistance: {}", e.getMessage());
            throw new RuntimeException("Erreur de persistance", e);
        }
    }
}