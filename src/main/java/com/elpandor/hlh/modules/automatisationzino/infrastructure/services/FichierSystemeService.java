package com.elpandor.hlh.modules.automatisationzino.infrastructure.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class FichierSystemeService {

    @Value("${app.temp.dir:${java.io.tmpdir}/automatisationzino}")
    private String tempDir;

    public void nettoyerFichiersTemporaires() {
        try {
            Path tempPath = Paths.get(tempDir);
            if (Files.exists(tempPath)) {
                FileUtils.cleanDirectory(tempPath.toFile());
                log.info("Fichiers temporaires nettoyés: {}", tempDir);
            }
        } catch (IOException e) {
            log.warn("Erreur lors du nettoyage des fichiers temporaires: {}", e.getMessage());
        }
    }

    public File creerRepertoireTemp() {
        try {
            Path tempPath = Paths.get(tempDir);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                log.info("Répertoire temporaire créé: {}", tempDir);
            }
            return tempPath.toFile();
        } catch (IOException e) {
            log.error("Erreur lors de la création du répertoire temporaire: {}", e.getMessage());
            return new File(System.getProperty("java.io.tmpdir"));
        }
    }

    public boolean supprimerFichier(File fichier) {
        try {
            if (fichier != null && fichier.exists()) {
                boolean deleted = Files.deleteIfExists(fichier.toPath());
                if (deleted) {
                    log.debug("Fichier supprimé: {}", fichier.getAbsolutePath());
                }
                return deleted;
            }
            return false;
        } catch (IOException e) {
            log.warn("Erreur lors de la suppression du fichier: {}", e.getMessage());
            return false;
        }
    }
}