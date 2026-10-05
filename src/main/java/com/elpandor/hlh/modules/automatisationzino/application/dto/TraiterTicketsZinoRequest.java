package com.elpandor.hlh.modules.automatisationzino.application.dto;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Corps de la requête pour le traitement manuel (webservice) des tickets Zino.
 * Les tickets sont déjà extraits (équivalent au résultat de {@code TransformerFichierZinoUseCase}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraiterTicketsZinoRequest {

    /**
     * Identifiant logique de la source (ex. nom de fichier Excel d'origine).
     * Utilisé pour l'historisation, la persistance et le champ automatisationFileName côté FNE.
     * Si absent, une valeur générée est utilisée.
     */
    private String nomFichierSource;

    @NotEmpty(message = "Le point de vente ne peut être vide")
    @Valid
    private String pointDeVente;

    @NotEmpty(message = "Le code établissement ne peut être vide")
    @Valid
    private String codeEtabblissement;

    /**
     * Liste des tickets de vente à envoyer à la FNE (mêmes objets que ceux produits par le batch).
     */
    @NotEmpty(message = "La liste des tickets ne peut pas être vide")
    @Valid
    private List<TicketVenteZino> tickets;
}
