package com.elpandor.hlh.modules.automatisationzino.rest;

import com.elpandor.hlh.common.service.EmailService;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.automatisationzino.application.dto.RapportExtractionRequest;
import com.elpandor.hlh.modules.automatisationzino.application.dto.RapportExtractionResponse;
import com.elpandor.hlh.modules.automatisationzino.application.dto.RapportFactureDto;
import com.elpandor.hlh.modules.automatisationzino.application.usecases.RapportExtractionUseCase;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Rapport d'état des extractions automatiques Zino : consultation, export (Excel/PDF/Word)
 * et envoi par mail au responsable Zino.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/automatisationzino/rapport")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class RapportExtractionZinoApi {

    private final RapportExtractionUseCase rapportExtractionUseCase;
    private final EmailService emailService;

    private static final DateTimeFormatter FMT_FICHIER = DateTimeFormatter.ofPattern("ddMMyyyy");

    @PostMapping
    public ResponseEntity<Map<String, Object>> getRapport(@Valid @RequestBody RapportExtractionRequest request) {
        RapportExtractionResponse rapport = rapportExtractionUseCase.genererRapport(request);
        return Utilities.createSuccessResponse(HttpStatus.OK, rapport, "Rapport d'état des extractions Zino");
    }

    @PostMapping("/factures")
    public ResponseEntity<Map<String, Object>> getFacturesPaginees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @Valid @RequestBody RapportExtractionRequest request) {

        Page<RapportFactureDto> factures = rapportExtractionUseCase.getFacturesPaginees(request, page, size, sortBy, direction);

        return Utilities.createSuccessResponse(HttpStatus.OK, factures, "Détail des factures - Rapport Zino");
    }

    @PostMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(@Valid @RequestBody RapportExtractionRequest request) {
        return construireReponseFichier(request, "excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
    }

    @PostMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody RapportExtractionRequest request) {
        return construireReponseFichier(request, "pdf", MediaType.APPLICATION_PDF_VALUE, "pdf");
    }

    @PostMapping(value = "/export/word", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> exportWord(@Valid @RequestBody RapportExtractionRequest request) {
        return construireReponseFichier(request, "word",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
    }

    @PostMapping("/envoyer-mail")
    public ResponseEntity<Map<String, Object>> envoyerParMail(@Valid @RequestBody EnvoyerRapportMailRequest requeteEnvoi) {
        try {
            byte[] fichier = rapportExtractionUseCase.exporter(requeteEnvoi.getRequest(), requeteEnvoi.getFormat());
            String extension = extensionPour(requeteEnvoi.getFormat());
            String nomFichier = nomFichier(requeteEnvoi.getRequest(), extension);

            String sujet = "Rapport d'état des extractions Zino - "
                    + requeteEnvoi.getRequest().getDateDebut() + " au " + requeteEnvoi.getRequest().getDateFin();
            String corps = "Bonjour,<br/><br/>Veuillez trouver ci-joint le rapport d'état des extractions automatiques Zino "
                    + "pour la période du " + requeteEnvoi.getRequest().getDateDebut()
                    + " au " + requeteEnvoi.getRequest().getDateFin() + ".<br/><br/>Cordialement.";

            boolean ok = emailService.sendEmailWithAttachment(
                    requeteEnvoi.getDestinataire(), sujet, corps, fichier, nomFichier);

            if (!ok) {
                return Utilities.createErrorResponse("Échec de l'envoi de l'email", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, "Rapport envoyé par mail à " + requeteEnvoi.getDestinataire(), null);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi du rapport par mail: {}", e.getMessage(), e);
            return Utilities.createErrorResponse("Erreur lors de l'envoi de l'email: " + e.getMessage(), List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Erreur lors de la génération/envoi du rapport: {}", e.getMessage(), e);
            return Utilities.createErrorResponse("Erreur: " + e.getMessage(), List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<byte[]> construireReponseFichier(RapportExtractionRequest request, String format, String mediaType, String extension) {
        try {
            byte[] fichier = rapportExtractionUseCase.exporter(request, format);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mediaType));
            headers.setContentDispositionFormData("attachment", nomFichier(request, extension));
            headers.setContentLength(fichier.length);

            return ResponseEntity.ok().headers(headers).body(fichier);
        } catch (Exception e) {
            log.error("Erreur lors de l'export {} du rapport Zino: {}", format, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String nomFichier(RapportExtractionRequest request, String extension) {
        return "rapport_extraction_zino_" + request.getDateDebut() + "_" + request.getDateFin() + "." + extension;
    }

    private String extensionPour(String format) {
        return switch (format.toLowerCase()) {
            case "excel", "xlsx" -> "xlsx";
            case "pdf" -> "pdf";
            case "word", "docx" -> "docx";
            default -> "bin";
        };
    }
}
