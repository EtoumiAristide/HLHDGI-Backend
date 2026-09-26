package com.elpandor.hlh.modules.automatisationzino.domain.repository;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;

import java.util.List;

/**
 * Port de persistance des tickets de vente Zino extraits des fichiers CSV.
 */
public interface TicketVenteRepository {

    /**
     * Persiste un lot de tickets, en les rattachant au nom du fichier source d'origine.
     */
    List<TicketVenteZino> saveAll(List<TicketVenteZino> tickets, String nomFichierSource);

    List<TicketVenteZino> findByNomFichierSource(String nomFichierSource);
}
