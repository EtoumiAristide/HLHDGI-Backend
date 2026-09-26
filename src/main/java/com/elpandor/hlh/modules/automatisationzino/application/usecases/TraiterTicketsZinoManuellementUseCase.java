package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.application.dto.TraiterTicketsZinoRequest;
import com.elpandor.hlh.modules.automatisationzino.application.dto.TraiterTicketsZinoResponse;
import com.elpandor.hlh.modules.automatisationzino.batch.processor.FichierProcessor;
import com.elpandor.hlh.modules.automatisationzino.domain.exception.EnvoiFNEException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestration du même pipeline métier que le batch ({@code FichierProcessor}),
 * mais à partir de tickets déjà fournis en JSON (sans téléchargement ni parsing de fichier).
 * <p>
 * Étapes :
 * <ol>
 *   <li>Envoi à la FNE ({@link EnvoyerFactureZinoUseCase})</li>
 *   <li>Persistance des tickets + répartition paiements ({@link PersisterFactureUseCase})</li>
 *   <li>Historisation de l'envoi ({@link HistoriserEnvoiUseCase})</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraiterTicketsZinoManuellementUseCase {

    private static final DateTimeFormatter NOM_SOURCE_FMT =
            DateTimeFormatter.ofPattern("'MANUEL_'yyyyMMdd_HHmmss");

    private final EnvoyerFactureZinoUseCase envoyerFactureZinoUseCase;
    private final PersisterFactureUseCase persisterFactureUseCase;
    private final HistoriserEnvoiUseCase historiserEnvoiUseCase;
    private final FichierProcessor processor;

    @Transactional
    public TraiterTicketsZinoResponse executer(TraiterTicketsZinoRequest request) {
        long startTime = System.currentTimeMillis();

        List<TicketVenteZino> tickets = request.getTickets();
        String nomFichierSource = resoudreNomFichierSource(request.getNomFichierSource());
        request.setNomFichierSource(nomFichierSource);
        log.info("Traitement manuel de {} tickets Zino (source: {})", tickets.size(), nomFichierSource);

        ResultatEnvoiFNE resultat = processor.manuelProcess(request);

        long executionTime = System.currentTimeMillis() - startTime;

        return TraiterTicketsZinoResponse.builder()
                .succes(resultat.isSucces())
                .message(resultat.getMessage())
                .nomFichierSource(nomFichierSource)
                .nombreTicketsRecus(tickets.size())
                .idTransaction(resultat.getIdTransaction())
                .codeErreur(resultat.getCodeErreur())
                .details(resultat.getDetails())
                .tempsExecutionMs(executionTime)
                .liensFactureFNE(resultat.getLiensFactureFNE())
                .build();

    }

    private String resoudreNomFichierSource(String nomFourni) {
        if (nomFourni != null && !nomFourni.isBlank()) {
            return nomFourni.trim();
        }
        return NOM_SOURCE_FMT.format(LocalDateTime.now());
    }

    /**
     * Reprend le premier code produit non null des détails, comme indicateur principal
     * pour l'historique (équivalent approximatif de {@code FichierSource.codeProduitPrincipal}).
     */
    private String extraireCodeProduitPrincipal(List<TicketVenteZino> tickets) {
        if (tickets == null) {
            return null;
        }
        for (TicketVenteZino ticket : tickets) {
            if (ticket.getCodeProduitPrincipal() != null && !ticket.getCodeProduitPrincipal().isBlank()) {
                return ticket.getCodeProduitPrincipal();
            }
            if (ticket.getDetails() != null) {
                for (var detail : ticket.getDetails()) {
                    if (detail.getCodeProduit() != null && !detail.getCodeProduit().isBlank()) {
                        return detail.getCodeProduit();
                    }
                }
            }
        }
        return null;
    }
}
