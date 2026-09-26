package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.domain.exception.TransformationException;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.ZinoTicketVenteExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformerFichierZinoUseCase {

    private final ZinoTicketVenteExtractor extractor;

    public List<TicketVenteZino> executer(File fichierBrut) throws TransformationException {
        log.info("Transformation du fichier Zino: {}", fichierBrut.getName());

        try (FileInputStream fis = new FileInputStream(fichierBrut)) {
            List<TicketVenteZino> tickets = extractor.extraire(fis);
            log.info("Fichier transformé avec succès. {} tickets extraits.", tickets.size());
//            log.info("Fichier transformé avec succès. {} tickets extraits.", tickets);
            return tickets;

        } catch (Exception e) {
            log.error("Erreur lors de la transformation du fichier: {}", e.getMessage());
            throw new TransformationException("Erreur de transformation: " + e.getMessage(), e);
        }
    }
}