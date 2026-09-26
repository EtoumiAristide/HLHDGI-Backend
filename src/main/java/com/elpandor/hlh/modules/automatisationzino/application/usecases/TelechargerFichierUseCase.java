package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.domain.exception.TelechargementException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.ApiTelechargementClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelechargerFichierUseCase {

    private final ApiTelechargementClient apiTelechargementClient;

    public File executer(String nomFichier) throws TelechargementException {
        log.info("Téléchargement du fichier: {}", nomFichier);

        try {
            MultipartFile multipartFile = apiTelechargementClient.telechargerFichier(nomFichier);

            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = UUID.randomUUID().toString() + "_" + nomFichier;
            Path filePath = Paths.get(tempDir, fileName);

            Files.write(filePath, multipartFile.getBytes());
            File fichier = filePath.toFile();

            log.info("Fichier téléchargé avec succès: {}", fichier.getAbsolutePath());
            return fichier;

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier {}: {}", nomFichier, e.getMessage());
            throw new TelechargementException("Erreur de téléchargement: " + e.getMessage(), e);
        }
    }
}