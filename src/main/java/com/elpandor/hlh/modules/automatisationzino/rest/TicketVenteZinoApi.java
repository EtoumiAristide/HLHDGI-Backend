package com.elpandor.hlh.modules.automatisationzino.rest;

import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.automatisationzino.application.dto.TraiterTicketsZinoRequest;
import com.elpandor.hlh.modules.automatisationzino.application.dto.TraiterTicketsZinoResponse;
import com.elpandor.hlh.modules.automatisationzino.application.usecases.TraiterTicketsZinoManuellementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webservice d'injection manuelle des tickets de vente Zino.
 * <p>
 * Reçoit un tableau JSON de {@code TicketVenteZino} (avec leurs {@code DetailZino})
 * et applique le même traitement métier que le job Spring Batch :
 * conversion → envoi FNE → persistance → historisation.
 * <p>
 * Endpoint principal : {@code POST /api/v1/automatisationzino/tickets/traiter}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/automatisationzino/tickets")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TicketVenteZinoApi {

    private final TraiterTicketsZinoManuellementUseCase traiterTicketsZinoManuellementUseCase;

    /**
     * Traite une liste de tickets Zino déjà structurés (issus d'un Excel / CSV parsé côté client
     * ou d'un autre système) et les envoie à la FNE.
     *
     * @param request corps JSON contenant {@code nomFichierSource} (optionnel) et {@code tickets}
     * @return résultat agrégé de l'envoi (succès / partial / erreur)
     */
    @PostMapping("/toFacture")
    public ResponseEntity<Map<String, Object>> traiterTickets(
            @Valid @RequestBody TraiterTicketsZinoRequest request) {

        log.info("Réception webservice : {} tickets, source={}",
                request.getTickets() != null ? request.getTickets().size() : 0,
                request.getNomFichierSource());

        TraiterTicketsZinoResponse response = traiterTicketsZinoManuellementUseCase.executer(request);

        HttpStatus status = response.isSucces() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = response.isSucces()
                ? "Traitement des tickets Zino terminé avec succès"
                : "Traitement des tickets Zino terminé avec des erreurs";

        return Utilities.createSuccessResponse(status, response, message);
    }
}
