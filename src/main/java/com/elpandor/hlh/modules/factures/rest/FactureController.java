package com.elpandor.hlh.modules.factures.rest;

import com.elpandor.hlh.common.service.impl.FileStorageServiceImpl;
import com.elpandor.hlh.common.utils.ExcelFactureExtractor;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.factures.model.dto.FactureDto;
import com.elpandor.hlh.modules.factures.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.factures.model.TypeFacture;
import com.elpandor.hlh.modules.factures.service.FactureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/factures")
public class FactureController {
    private Logger log = LoggerFactory.getLogger(FactureController.class);

    private final FileStorageServiceImpl fileStorageService;
    private final FactureService factureService;

    public FactureController(FileStorageServiceImpl fileStorageService, FactureService factureService) {
        this.fileStorageService = fileStorageService;
        this.factureService = factureService;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "type", defaultValue = "1") Integer typeFacture) {
        log.trace("Starting processing get request for uploadExcelFile");
        try {
            // Process the uploaded file
            if (file.isEmpty()) {
                log.error("Fichier inexistant!");
                return Utilities.createErrorResponse("Aucun fichier chargé!", List.of(), HttpStatus.BAD_REQUEST);
            }

            // Log file details
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            long fileSize = file.getSize();

            log.info("Received file: Name={}, Type={}, Size={}", fileName, fileType, fileSize);

            // Validate file type by extension
            assert fileName != null;
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
                log.error("Unsupported file type: {}", fileType);
                return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            InputStream is = file.getInputStream();

//            List<MyObject> objects = ExcelParser.parseExcelFile(is);
            //ExcelParser.parseExcelFile(is);
            ExcelFactureExtractor extractor = new ExcelFactureExtractor();
            FacturePayload facture = extractor.extractFacture(is);
            facture.setTypeFacture(TypeFacture.FACTURE_VENTE);

//            System.out.println(facture);
            //Sauvegarde du fichier
            String storeName = fileStorageService.storeFile(file, "Facture-" + new SimpleDateFormat("yyyyMMdddHHmmss").format(new Date()));

            //TODO: Enregistrer en BD
            FactureDto factureDto = FactureDto.builder()
                    .numFacture(facture.getNumeroFacture())
                    .dateFacture(LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .nomClient(facture.getClientPayload().getNom())
                    .lienFichier(storeName)
                    .typeFacture(TypeFacture.FACTURE_VENTE)
                    .build();
            factureService.saveOrUpdate(factureDto);

            return Utilities.createSuccessResponse(HttpStatus.OK, facture, "Fichier chargé et traité avec succès");

        } catch (IOException e) {
            log.error("IOException occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
