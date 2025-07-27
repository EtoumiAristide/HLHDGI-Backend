package com.elpandor.hlh.modules.factures.rest;

import com.elpandor.hlh.common.service.impl.FileStorageServiceImpl;
import com.elpandor.hlh.common.utils.ExcelFactureExtractor;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.factures.model.TypeFacture;
import com.elpandor.hlh.modules.factures.model.dto.FactureDto;
import com.elpandor.hlh.modules.factures.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.factures.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.factures.service.ApimService;
import com.elpandor.hlh.modules.factures.service.FactureService;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/factures")
public class FactureController {
    private Logger log = LoggerFactory.getLogger(FactureController.class);

    private final FileStorageServiceImpl fileStorageService;
    private final FactureService factureService;
    private final ApimService apimService;

    public FactureController(FileStorageServiceImpl fileStorageService, FactureService factureService, ApimService apimService) {
        this.fileStorageService = fileStorageService;
        this.factureService = factureService;
        this.apimService = apimService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        FactureDto factureDto = factureService.get(id);
        if (factureDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, factureDto, "Facture trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Facture avec l'id " + id + " non trouvé");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<FactureDto> graviteDtos = factureService.getAll();
        if (graviteDtos != null && !graviteDtos.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, graviteDtos, "Liste des types carte");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<FactureDto> pages = factureService.getAllPagined(pageNum, size);
        List<FactureDto> typeCartes = pages.getContent();
        if (!typeCartes.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des types d'intervention");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "type", defaultValue = "FACTURE_VENTE") String typeFacture) {
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
            return Utilities.createSuccessResponse(HttpStatus.OK, facture, "Fichier chargé et traité avec succès");

        } catch (IOException e) {
            log.error("IOException occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestParam("file") MultipartFile file, @RequestParam(value = "type", defaultValue = "FACTURE_VENTE") String typeFacture) {
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

            //Appel de l'api DGI
            TokenResponse tokenResponse = apimService.auth();
            String request = Json.pretty(facture);
            ResponseEntity<String> response = apimService.sendData(tokenResponse.getAccessToken(), facture);
            if (response.getStatusCode().is2xxSuccessful()) {
                FactureDto factureDto = FactureDto.builder()
                        .numFacture(facture.getNumeroFacture())
                        .dateFacture(LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .nomClient(facture.getClientPayload().getNom())
                        .lienFichier(storeName)
                        .typeFacture(TypeFacture.valueOf(typeFacture))
                        .dataSend(request)
                        .reponseFNE(response.getBody())
                        .build();

                factureService.saveOrUpdate(factureDto);

                return Utilities.createSuccessResponse(HttpStatus.OK, facture, "Fichier chargé avec succès");
            } else {

                log.error("L'authentification de la facture à échoué");
                return Utilities.createErrorResponse("L'authentification de la facture à échoué", response.getBody(), HttpStatus.INTERNAL_SERVER_ERROR);
            }


        } catch (IOException e) {
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping
    public ResponseEntity<Map<String, Object>> update(@RequestParam(name = "file", required = false) MultipartFile file, @RequestParam(value = "type", defaultValue = "FACTURE_VENTE") String typeFacture, @RequestParam("id") Integer id) {
        log.trace("Starting processing get request for uploadExcelFile");
        try {

            //Recupération de la facture
            FactureDto factureDto = factureService.get(id);
            if (factureDto == null)
                return Utilities.createErrorResponse("Facture avec l'id " + id + " n'est pas disponible", List.of(), HttpStatus.BAD_REQUEST);

            FacturePayload facture = null;
            String storeName = null;

            // Process the uploaded file
            if (file != null) {
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
                facture = extractor.extractFacture(is);
                facture.setTypeFacture(TypeFacture.FACTURE_VENTE);

//            System.out.println(facture);
                //Sauvegarde du fichier
                storeName = fileStorageService.storeFile(file, "Facture-" + new SimpleDateFormat("yyyyMMdddHHmmss").format(new Date()));
            }
            if (facture != null) {
                factureDto.setNumFacture(facture.getNumeroFacture());
                factureDto.setDateFacture(LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                factureDto.setNomClient(facture.getClientPayload().getNom());
                factureDto.setLienFichier(storeName);
                factureDto.setTypeFacture(TypeFacture.valueOf(typeFacture));
            }
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

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!factureService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Facture avec l'id" + id + " non trouvé", null, HttpStatus.NOT_FOUND);
        }

        factureService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Facture supprimé avec succès");
    }
}
