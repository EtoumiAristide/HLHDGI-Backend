package com.elpandor.hlh.modules.automatisationzino.batch.processor;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.application.usecases.*;
import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
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
    private final PersisterFactureUseCase persisterFactureUseCase;
    private final HistoriserEnvoiUseCase historiserEnvoiUseCase;
    private final RenommerFichierUseCase renommerFichierUseCase;
    private final FichierSourceRepository fichierSourceRepository;
    private final ObjectMapper objectMapper;

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
            ResultatEnvoiFNE resultat = envoyerFactureZinoUseCase.executer(tickets, fichierSource.getNomFichier());
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
}