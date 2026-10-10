package com.elpandor.hlh.modules.automatisationzino.batch.processor;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.application.dto.TraiterTicketsZinoRequest;
import com.elpandor.hlh.modules.automatisationzino.application.usecases.*;
import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.impressionzino.application.dto.TicketAImprimer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FichierProcessor implements ItemProcessor<FichierSource, FichierSource> {

    private final TelechargerFichierUseCase telechargerFichierUseCase;
    private final TransformerFichierZinoUseCase transformerFichierZinoUseCase;
    private final EnvoyerFactureZinoUseCase envoyerFactureZinoUseCase;
    private final EnvoyerAvoirZinoUseCase envoyerAvoirZinoUseCase;
    private final PersisterFactureUseCase persisterFactureUseCase;
    private final HistoriserEnvoiUseCase historiserEnvoiUseCase;
    private final RenommerFichierUseCase renommerFichierUseCase;
    private final FichierSourceRepository fichierSourceRepository;
    private final ObjectMapper objectMapper;

    //Traitement manuel
    private final DecouvrirNouveauxFichiersUseCase decouvrirNouveauxFichiersUseCase;

    @Value("${batch.max-tentatives:3}")
    private int maxTentatives;

    @Override
    public FichierSource process(FichierSource fichierSource) throws Exception {
        long startTime = System.currentTimeMillis();

        log.info("Traitement du fichier: {}", fichierSource.getNomFichier());
//        log.info("Tentative {}/{}", fichierSource.getTentativeEnvoi() + 1, maxTentatives);
        log.info("Tentative {}", fichierSource.getTentativeEnvoi() + 1);

        List<TicketVenteZino> tickets = null;
        File fichierBrut = null;

        try {
            // 1️TÉLÉCHARGEMENT
            log.info("Téléchargement du fichier...");
            fichierBrut = telechargerFichierUseCase.executer(fichierSource.getNomFichier());
            log.info("Fichier téléchargé: {} ({} octets)",
                    fichierBrut.getName(), fichierBrut.length());

            // 2️TRANSFORMATION
            log.info("Transformation du fichier...");
            tickets = transformerFichierZinoUseCase.executer(fichierBrut);
            log.info("tickets extraits {}", tickets.size());

            // 3️⃣ SAUVEGARDE DES DONNÉES EXTRAITES EN JSON
            log.info("Sauvegarde des données extraites en JSON...");
            sauvegarderDonneesExtraites(fichierSource.getId(), tickets, fichierSource.getNomFichier());
            log.info("Données sauvegardées");

            // 4️ENVOI À LA FNE VIA ZinoApimService (SANS JWT)
            log.info("Envoi à la FNE via ZinoApimService...");
            ResultatEnvoiFNE resultat = envoyerFactureZinoUseCase.executer(tickets, fichierSource.getNomFichier(), null);
            log.info("Envoi terminé: {}", resultat.getMessage());

            // 5️PERSISTANCE
            log.info("Persistance des factures...");
            persisterFactureUseCase.executer(tickets, fichierSource.getNomFichier());
            log.info("Persistance terminée");

            // 6️HISTORISATION
            long executionTime = System.currentTimeMillis() - startTime;
            historiserEnvoiUseCase.executer(
                    fichierSource.getNomFichier(),
                    resultat,
                    fichierSource.getTentativeEnvoi() + 1,
                    executionTime,
                    fichierSource.getCodeProduitPrincipal()
            );
            log.info("Historique sauvegardé");

            // 7️MISE À JOUR DU STATUT
            if (resultat.isSucces()) {
                fichierSource.setStatut("SENT");
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "SENT",
                        null
                );
                log.info("Fichier {} traité avec SUCCÈS", fichierSource.getNomFichier());

                // 8️⃣ RENOMMAGE CÔTÉ API ZINO
                // Ne doit jamais remettre en cause le statut SENT déjà acquis (facture déjà
                // envoyée à la FNE) : une erreur ici est journalisée mais n'invalide pas le
                // traitement, sous peine de re-déclencher une re-facturation au prochain run.
                try {
                    log.info("Renommage du fichier côté API Zino...");
                    renommerFichierUseCase.executer(fichierSource.getNomFichier());
                } catch (Exception ex) {
                    log.warn("Échec du renommage du fichier {} côté API Zino (traitement local déjà en SUCCÈS, non impacté): {}",
                            fichierSource.getNomFichier(), ex.getMessage());
                }
            } else {
                fichierSource.setStatut("ERROR");
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "ERROR",
                        resultat.getMessage()
                );
                log.warn("Échec du traitement du fichier {}: {}",
                        fichierSource.getNomFichier(),
                        resultat.getMessage());
            }

            fichierSourceRepository.incrementerTentative(fichierSource.getId());
            log.info("Temps total: {}ms", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Erreur lors du traitement du fichier {}: {}",
                    fichierSource.getNomFichier(), e.getMessage(), e);

            // Sauvegarde des données si elles ont été extraites
            if (tickets != null && !tickets.isEmpty()) {
                try {
                    sauvegarderDonneesExtraites(fichierSource.getId(), tickets, fichierSource.getNomFichier());
                } catch (Exception ex) {
                    log.warn("Impossible de sauvegarder les données extraites: {}", ex.getMessage());
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;
            ResultatEnvoiFNE resultatEchec = ResultatEnvoiFNE.builder()
                    .succes(false)
                    .codeErreur("EXCEPTION")
                    .message(e.getMessage())
                    .build();

            historiserEnvoiUseCase.executer(
                    fichierSource.getNomFichier(),
                    resultatEchec,
                    fichierSource.getTentativeEnvoi() + 1,
                    executionTime,
                    fichierSource.getCodeProduitPrincipal()
            );

            //int nouvelleTentative = fichierSource.getTentativeEnvoi() + 1;
            fichierSourceRepository.incrementerTentative(fichierSource.getId());

            //Mis en commentaire car nombre d'essai doit être illimité
            /*if (nouvelleTentative >= maxTentatives) {
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "ECHEC_DEFINITIF",
                        "Échec définitif après " + maxTentatives + " tentatives: " + e.getMessage()
                );
                log.error("Fichier {} en ÉCHEC DÉFINITIF après {} tentatives",
                        fichierSource.getNomFichier(), maxTentatives);
            } else {*/
            fichierSourceRepository.updateStatut(
                    fichierSource.getId(),
                    "ERROR",
                    e.getMessage()
            );
            log.warn("Fichier {} en ERREUR, nouvelle tentative prévue",
                    fichierSource.getNomFichier());
            //}

            // IMPORTANT: ne pas relancer l'exception ici.
            // Le statut ERROR et l'historique ont déjà été persistés ci-dessus.
            // Relancer ferait échouer tout le CHUNK Spring Batch, ce qui provoque
            // un ROLLBACK de la transaction du chunk entier — y compris les fichiers
            // déjà traités AVEC SUCCÈS dans le même chunk (facture déjà envoyée à la
            // FNE mais statut local perdu -> re-traitement et RE-FACTURATION au prochain
            // run). On retourne null pour indiquer à Spring Batch de simplement
            // exclure cet item du chunk sans faire échouer l'étape.
            return null;
        } finally {
            // Nettoyage du fichier temporaire
            if (fichierBrut != null && fichierBrut.exists()) {
                boolean deleted = fichierBrut.delete();
                log.debug("Fichier temporaire supprimé: {} - {}",
                        fichierBrut.getAbsolutePath(), deleted ? "ok" : "supprimé!!");
            }
        }

        return fichierSource;
    }


    //Sauvegarde les données extraites en JSON dans la base

    private void sauvegarderDonneesExtraites(Long fichierSourceId, List<TicketVenteZino> tickets, String nomFichier) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("nomFichier", nomFichier);
            data.put("dateExtraction", System.currentTimeMillis());
            data.put("nombreTickets", tickets.size());
            data.put("tickets", tickets);

            String jsonData = objectMapper.writeValueAsString(data);
            fichierSourceRepository.sauvegarderDonneesExtraites(fichierSourceId, jsonData);

            log.info("tickets sauvegardés en JSON {}", tickets.size());

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des données JSON: {}", e.getMessage());
        }
    }

    public ResultatEnvoiFNE manuelProcess(TraiterTicketsZinoRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Traitement manuel ticket: {}", request.getNomFichierSource());
//        log.info("Tentative {}/{}", fichierSource.getTentativeEnvoi() + 1, maxTentatives);

        File fichierBrut = null;
        FichierSource fichierSource = decouvrirNouveauxFichiersUseCase.creerFichierSource(request);
        ResultatEnvoiFNE resultat = null;

        try {
            //Sauvegarde du fichier source
            fichierSource = fichierSourceRepository.save(fichierSource);

            // 3️⃣ SAUVEGARDE DES DONNÉES EXTRAITES EN JSON
            log.info("Sauvegarde des données extraites en JSON...");
            sauvegarderDonneesExtraites(fichierSource.getId(), request.getTickets(), fichierSource.getNomFichier());
            log.info("Données sauvegardées");

            // 4️ENVOI À LA FNE VIA ZinoApimService (SANS JWT)
            log.info("Envoi à la FNE via ZinoApimService...");
            // Les tickets à montant négatif donnent lieu à une facture d'avoir, les autres à une facture de vente
            List<TicketVenteZino> ticketsVente = request.getTickets().stream()
                    .filter(t -> !EnvoyerAvoirZinoUseCase.estTicketNegatif(t))
                    .toList();
            List<TicketVenteZino> ticketsAvoir = request.getTickets().stream()
                    .filter(EnvoyerAvoirZinoUseCase::estTicketNegatif)
                    .toList();
            log.info("Tickets: {} vente(s), {} avoir(s)", ticketsVente.size(), ticketsAvoir.size());

            ResultatEnvoiFNE resultatVente = ticketsVente.isEmpty() ? null :
                    envoyerFactureZinoUseCase.executer(ticketsVente, fichierSource.getNomFichier(), request.getPointDeVente());
            ResultatEnvoiFNE resultatAvoir = ticketsAvoir.isEmpty() ? null :
                    envoyerAvoirZinoUseCase.executer(ticketsAvoir, fichierSource.getNomFichier(), request.getPointDeVente());
            resultat = fusionnerResultats(resultatVente, resultatAvoir);
            log.info("Envoi terminé: {}", resultat.getMessage());

            // 5 ️PERSISTANCE
            log.info("Persistance des factures...");
            persisterFactureUseCase.executer(request.getTickets(), fichierSource.getNomFichier());
            log.info("Persistance terminée");

            // 6️ HISTORISATION
            long executionTime = System.currentTimeMillis() - startTime;
            historiserEnvoiUseCase.executer(
                    fichierSource.getNomFichier(),
                    resultat,
                    fichierSource.getTentativeEnvoi() + 1,
                    executionTime,
                    fichierSource.getCodeProduitPrincipal()
            );
            log.info("Historique sauvegardé");

            // 7️MISE À JOUR DU STATUT
            if (resultat.isSucces()) {
                fichierSource.setStatut("SENT");
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "SENT",
                        null
                );
                log.info("Fichier {} traité avec SUCCÈS", fichierSource.getNomFichier());

            } else {
                fichierSource.setStatut("ERROR");
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "ERROR",
                        resultat.getMessage()
                );
                log.warn("Échec du traitement du fichier {}: {}",
                        fichierSource.getNomFichier(),
                        resultat.getMessage());
            }

            fichierSourceRepository.incrementerTentative(fichierSource.getId());
            log.info("Temps total: {}ms", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Erreur lors du traitement du fichier {}: {}",
                    request.getNomFichierSource(), e.getMessage(), e);

            // Sauvegarde des données si elles ont été extraites
            if (request.getTickets() != null && !request.getTickets().isEmpty()) {
                try {
                    sauvegarderDonneesExtraites(fichierSource.getId(), request.getTickets(), request.getNomFichierSource());
                } catch (Exception ex) {
                    log.warn("Impossible de sauvegarder les données extraites: {}", ex.getMessage());
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;
            // Les factures déjà acceptées par la FNE avant l'incident (ex. échec de la persistance locale)
            // doivent quand même être imprimées : on conserve leurs tickets dans le résultat d'échec.
            List<TicketAImprimer> dejaAcceptees = resultat != null ? resultat.getFacturesImprimables() : null;
            resultat = ResultatEnvoiFNE.builder()
                    .succes(false)
                    .codeErreur("EXCEPTION")
                    .message(e.getMessage())
                    .facturesImprimables(dejaAcceptees)
                    .build();

            historiserEnvoiUseCase.executer(
                    fichierSource.getNomFichier(),
                    resultat,
                    fichierSource.getTentativeEnvoi() + 1,
                    executionTime,
                    fichierSource.getCodeProduitPrincipal()
            );

            //int nouvelleTentative = fichierSource.getTentativeEnvoi() + 1;
            fichierSourceRepository.incrementerTentative(fichierSource.getId());

            //Mis en commentaire car nombre d'essai doit être illimité
            /*if (nouvelleTentative >= maxTentatives) {
                fichierSourceRepository.updateStatut(
                        fichierSource.getId(),
                        "ECHEC_DEFINITIF",
                        "Échec définitif après " + maxTentatives + " tentatives: " + e.getMessage()
                );
                log.error("Fichier {} en ÉCHEC DÉFINITIF après {} tentatives",
                        fichierSource.getNomFichier(), maxTentatives);
            } else {*/
            fichierSourceRepository.updateStatut(
                    fichierSource.getId(),
                    "ERROR",
                    e.getMessage()
            );
            log.warn("Fichier {} en ERREUR, nouvelle tentative prévue",
                    fichierSource.getNomFichier());
            //}

            // IMPORTANT: ne pas relancer l'exception ici.
            // Le statut ERROR et l'historique ont déjà été persistés ci-dessus.
            // Relancer ferait échouer tout le CHUNK Spring Batch, ce qui provoque
            // un ROLLBACK de la transaction du chunk entier — y compris les fichiers
            // déjà traités AVEC SUCCÈS dans le même chunk (facture déjà envoyée à la
            // FNE mais statut local perdu -> re-traitement et RE-FACTURATION au prochain
            // run). On retourne null pour indiquer à Spring Batch de simplement
            // exclure cet item du chunk sans faire échouer l'étape.
            return resultat;
        } finally {
            // Nettoyage du fichier temporaire
            if (fichierBrut != null && fichierBrut.exists()) {
                boolean deleted = fichierBrut.delete();
                log.debug("Fichier temporaire supprimé: {} - {}",
                        fichierBrut.getAbsolutePath(), deleted ? "ok" : "supprimé!!");
            }
        }

        return resultat;
    }

    /**
     * Fusionne le résultat d'envoi des factures de vente et celui des avoirs.
     * Le succès global exige que chaque volet exécuté ait réussi.
     */
    private static List<TicketAImprimer> fusionnerListes(List<TicketAImprimer> a, List<TicketAImprimer> b) {
        List<TicketAImprimer> tous = new ArrayList<>();
        if (a != null) tous.addAll(a);
        if (b != null) tous.addAll(b);
        return tous;
    }

    private ResultatEnvoiFNE fusionnerResultats(ResultatEnvoiFNE vente, ResultatEnvoiFNE avoir) {
        if (avoir == null) return vente;
        if (vente == null) return avoir;

        List<Map<String, String>> liens = new ArrayList<>();
        if (vente.getLiensFactureFNE() != null) liens.addAll(vente.getLiensFactureFNE());
        if (avoir.getLiensFactureFNE() != null) liens.addAll(avoir.getLiensFactureFNE());

        return ResultatEnvoiFNE.builder()
                .succes(vente.isSucces() && avoir.isSucces())
                .idTransaction(vente.getIdTransaction())
                .codeErreur((vente.getCodeErreur() != null ? vente.getCodeErreur() : "") + (avoir.getCodeErreur() != null ? avoir.getCodeErreur() : ""))
                .message(vente.getMessage() + " | " + avoir.getMessage())
                .details(vente.getDetails() + " | " + avoir.getDetails())
                .liensFactureFNE(liens)
                .facturesImprimables(fusionnerListes(vente.getFacturesImprimables(), avoir.getFacturesImprimables()))
                .build();
    }
}