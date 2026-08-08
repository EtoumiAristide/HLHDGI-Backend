package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.ApiTelechargementClient;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.dto.FichierDisponibleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecouvrirNouveauxFichiersUseCase {

    private final ApiTelechargementClient apiTelechargementClient;
    private final FichierSourceRepository fichierSourceRepository;

    // ===== Pattern pour extraire le code produit du nom de fichier =====
    private static final Pattern CODE_PATTERN = Pattern.compile("FNE_\\d+");

    public int executer() {
        log.info("Début de la découverte des nouveaux fichiers");

        try {
            // 1. Récupération de la liste des fichiers disponibles
            List<FichierDisponibleDTO> fichiersDisponibles = apiTelechargementClient.listerFichiersDisponibles();

            if (fichiersDisponibles == null || fichiersDisponibles.isEmpty()) {
                log.info("Aucun fichier disponible sur l'API");
                return 0;
            }

            //On retire les fichiers portant la mention OK s'il en existe
            fichiersDisponibles = fichiersDisponibles.stream().filter(fichierDisponibleDTO -> !fichierDisponibleDTO.getNomFichier().toUpperCase().contains("_OK")).toList();

            log.info("{} fichiers disponibles sur l'API", fichiersDisponibles.size());
            fichiersDisponibles.forEach(f ->
                    log.debug("   - {} ({} octets, {})",
                            f.getNomFichier(), f.getTaille(), f.getTypeFichier())
            );

            // 2. Traitement des fichiers
            int nouveauxFichiers = 0;
            int fichiersIgnores = 0;

            for (FichierDisponibleDTO fichierDisponible : fichiersDisponibles) {
                String nomFichier = fichierDisponible.getNomFichier();

                // Vérification du nom
                if (nomFichier == null || nomFichier.isBlank()) {
                    log.warn("Fichier disponible sans nom, ignoré: {}", fichierDisponible);
                    fichiersIgnores++;
                    continue;
                }

                // Vérification si déjà en base
                if (fichierSourceRepository.existsByNomFichier(nomFichier)) {
                    log.debug("Fichier déjà connu, ignoré: {}", nomFichier);
                    fichiersIgnores++;
                    continue;
                }

                // Création du nouveau fichier
                FichierSource nouveauFichier = creerFichierSource(fichierDisponible);
                fichierSourceRepository.save(nouveauFichier);
                nouveauxFichiers++;

                log.info("Nouveau fichier détecté et enregistré: {}", nomFichier);
            }

            log.info("Découverte terminée: {} nouveau(x) fichier(s) sur {} disponible(s) ({} ignorés)",
                    nouveauxFichiers, fichiersDisponibles.size(), fichiersIgnores);

            return nouveauxFichiers;

        } catch (RestClientException e) {
            // Erreur de communication avec l'API
            log.error("Erreur de communication avec l'API de téléchargement: {}", e.getMessage(), e);
            // On ne bloque pas le batch, on retourne 0
            return 0;

        } catch (Exception e) {
            // Erreur inattendue
            log.error("Erreur inattendue lors de la découverte: {}", e.getMessage(), e);
            return 0;
        }
    }


    //Crée un objet FichierSource à partir d'un FichierDisponibleDTO

    private FichierSource creerFichierSource(FichierDisponibleDTO dto) {
        String nomFichier = dto.getNomFichier();

        return FichierSource.builder()
                .nomFichier(nomFichier)
                .statut("PENDING")
                .tentativeEnvoi(0)
                .dateCreation(LocalDateTime.now())
                .dateDerniereModification(LocalDateTime.now())
                .codeProduitPrincipal(extraireCodeProduit(nomFichier))
                .cheminAcces(null)
                .dernierMessageErreur(null)
                .build();
    }

    //Extrait le code produit du nom du fichier
    private String extraireCodeProduit(String nomFichier) {
        if (nomFichier == null || nomFichier.isBlank()) {
            return null;
        }

        try {
            // Méthode 1: Extraire la partie après "FNE_" et avant ".csv"
            int idxFNE = nomFichier.indexOf("FNE_");
            if (idxFNE != -1) {
                String afterFNE = nomFichier.substring(idxFNE + 4); // +4 pour "FNE_"
                int idxDot = afterFNE.indexOf(".");
                if (idxDot != -1) {
                    return afterFNE.substring(0, idxDot);
                }
                return afterFNE;
            }

            // Méthode 2: Utiliser une regex pour trouver les chiffres après FNE_
            var matcher = CODE_PATTERN.matcher(nomFichier);
            if (matcher.find()) {
                String match = matcher.group();
                return match.replace("FNE_", "");
            }

            // Méthode 3: Extraire le dernier groupe de chiffres
            String[] parts = nomFichier.split("_");
            if (parts.length >= 3) {
                String lastPart = parts[parts.length - 1];
                int dotIdx = lastPart.indexOf(".");
                if (dotIdx != -1) {
                    return lastPart.substring(0, dotIdx);
                }
                return lastPart;
            }

            log.warn("Impossible d'extraire le code produit de: {}", nomFichier);
            return null;

        } catch (Exception e) {
            log.warn("Erreur lors de l'extraction du code produit: {}", e.getMessage());
            return null;
        }
    }
}