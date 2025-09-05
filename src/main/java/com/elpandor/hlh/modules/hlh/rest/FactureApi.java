package com.elpandor.hlh.modules.hlh.rest;

import com.elpandor.hlh.common.service.impl.FileStorageServiceImpl;
import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.service.impl.BurgerKingApimServiceImpl;
import com.elpandor.hlh.modules.hlh.service.impl.HLHApimServiceImpl;
import com.elpandor.hlh.modules.hlh.utils.ExcelFactureExtractor;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.FacturePayload;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FactureApi {
    private Logger log = LoggerFactory.getLogger(FactureApi.class);

    private final FileStorageServiceImpl fileStorageService;
    private final FactureService factureService;
    private final ApimService hlhApimService;
    private final ApimService bkApimService;
    private final EtablissementService etablissementService;

    @Value("${bk.api.entreprise}")
    private String entrepriseBK;

    @Value("${hlh.api.entreprise}")
    private String entrepriseHLH;

    public FactureApi(FileStorageServiceImpl fileStorageService, FactureService factureService, HLHApimServiceImpl hlhApimService, BurgerKingApimServiceImpl burgerKingApimService, EtablissementService etablissementService) {
        this.fileStorageService = fileStorageService;
        this.factureService = factureService;
        this.hlhApimService = hlhApimService;
        this.bkApimService = burgerKingApimService;
        this.etablissementService = etablissementService;
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
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", defaultValue = "0", required = false) Integer pageNum, @RequestParam(value = "size", defaultValue = "10", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<FactureDto> pages = factureService.getAllPagined(pageNum, size);
        List<FactureDto> typeCartes = pages.getContent();
        if (!typeCartes.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des types d'intervention");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping("/byentreprise")
    public ResponseEntity<Map<String, Object>> getAllByEntreprise(@RequestParam(value = "pageNum", defaultValue = "0", required = false) Integer pageNum, @RequestParam(value = "size", defaultValue = "10", required = false) Integer size, @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing getAll request!");

        Pageable pageable = PageRequest.of(pageNum, size);

        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        String entreprise = "";
        if (groups != null) entreprise = groups.get(0);
//        System.out.println(entreprise);

        Page<FactureDto> pages = factureService.findByEntreprise(pageable, entreprise);
        List<FactureDto> typeCartes = pages.getContent();
        if (!typeCartes.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des types d'intervention");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('Admin') or hasRole('Agent')")
    public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file,
                                                               @RequestParam(name = "type", defaultValue = "FACTURE_VENTE") String typeFacture,
                                                               @RequestParam(name = "client", defaultValue = "B2C") String typeClient,
                                                               @RequestParam(name = "paiement", defaultValue = "cash") String modePaiement,
                                                               @RequestParam(name = "pointvente", defaultValue = "pv") String pointVente,
                                                               @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing get request for uploadExcelFile");
        try {

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Entreprise agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

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
            List<FacturePayload> factures = extractor.extractFacture(is, etablissement != null ? etablissement.getOrganisation().getIndexLectureFichier() : 0);
            List<String> finalGroups = groups;
            factures.forEach(facturePayload -> {
                facturePayload.setTypeFacture(TypeFacture.valueOf(typeFacture));
                facturePayload.setTypeClient(TypeClient.valueOf(typeClient));
                if (etablissement != null) {
                    if (etablissement.getOrganisation().getIsOrderedByPaiementMethod()) {
                        facturePayload.setModePaiement(facturePayload.getSheetName().toLowerCase().contains("mobile money") ? ModePaiement.mobilemoney : (facturePayload.getSheetName().equalsIgnoreCase("cash") ? ModePaiement.cash : ModePaiement.card));

                        if (etablissement.getOrganisation().getIsPrixUnitaireDefined()) {
                            //On ajoute le prix unitaire dans les données
                            facturePayload.getLignes().forEach(ligneProduitPayload -> {
                                ligneProduitPayload.setPrixUnitaireHT(ligneProduitPayload.getMontantHT() / ligneProduitPayload.getQuantite());
                            });
                        }
                    } else {
                        facturePayload.setModePaiement(ModePaiement.valueOf(modePaiement));
                    }
                }
                facturePayload.setEntreprise(finalGroups.get(0));
                facturePayload.setPointVente(pointVente);
            });
            //facture.setTypeFacture(TypeFacture.valueOf(typeFacture));

//            System.out.println(facture);
            return Utilities.createSuccessResponse(HttpStatus.OK, factures, "Fichier chargé et traité avec succès");

        } catch (IOException e) {
            log.error("IOException occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Agent')")
    public ResponseEntity<Map<String, Object>> save(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(name = "type", defaultValue = "FACTURE_VENTE") String typeFacture,
                                                    @RequestParam(name = "client", defaultValue = "B2C") String typeClient,
                                                    @RequestParam(name = "paiement", defaultValue = "cash") String modePaiement,
                                                    @RequestParam(name = "pointvente", defaultValue = "pv") String pointVente,
                                                    @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing get request for uploadExcelFile");
        try {

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

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
            List<FacturePayload> factures = extractor.extractFacture(is, etablissement != null ? etablissement.getOrganisation().getIndexLectureFichier() : 0);

            List<String> finalGroups = groups;
            factures.forEach(facturePayload -> {
                facturePayload.setTypeFacture(TypeFacture.valueOf(typeFacture));
                facturePayload.setTypeClient(TypeClient.valueOf(typeClient));
                if (etablissement != null) {
                    if (etablissement.getOrganisation().getIsOrderedByPaiementMethod()) {
                        facturePayload.setModePaiement(facturePayload.getSheetName().toLowerCase().contains("mobile money") ? ModePaiement.mobilemoney : (facturePayload.getSheetName().equalsIgnoreCase("cash") ? ModePaiement.cash : ModePaiement.card));

                        if (etablissement.getOrganisation().getIsPrixUnitaireDefined()) {
                            //On ajoute le prix unitaire dans les données
                            facturePayload.getLignes().forEach(ligneProduitPayload -> {
                                ligneProduitPayload.setPrixUnitaireHT(ligneProduitPayload.getMontantHT() / ligneProduitPayload.getQuantite());
                            });
                        }

                    } else {
                        facturePayload.setModePaiement(ModePaiement.valueOf(modePaiement));
                    }
                }
                facturePayload.setEntreprise(finalGroups.get(0));
                facturePayload.setPointVente(pointVente);
            });
            //facture.setTypeFacture(TypeFacture.valueOf(typeFacture));

//            System.out.println(facture);
            //Sauvegarde du fichier
            String storeName = fileStorageService.storeFile(file, "Facture-" + new SimpleDateFormat("yyyyMMdddHHmmss").format(new Date()));

            for (FacturePayload facture : factures) {
                //Appel de l'api DGI
                TokenResponse tokenResponse = null;
                ResponseEntity<String> response = null;
                String request = Json.pretty(facture);

                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseHLH)) {
                    tokenResponse = hlhApimService.auth();
                    response = hlhApimService.sendData(tokenResponse.getAccessToken(), facture);
                }
                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseBK)) {
                    tokenResponse = bkApimService.auth();
                    response = bkApimService.sendData(tokenResponse.getAccessToken(), facture);
                }

                if (response.getStatusCode().is2xxSuccessful()) {
                    FactureDto factureDto = FactureDto.builder()
                            .numFacture(facture.getNumeroFacture())
                            .dateFacture(LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .nomClient(facture.getClientPayload().getNom())
                            .lienFichier(storeName)
                            .typeFacture(facture.getTypeFacture())
                            .typeClient(facture.getTypeClient())
                            .modePaiement(facture.getModePaiement())
                            .dataSend(request)
                            .reponseFNE(response.getBody())
                            .build();

                    factureService.saveOrUpdate(factureDto);
                } else {
                    log.error("L'authentification de la facture à échoué");
                    return Utilities.createErrorResponse("L'authentification de la facture à échoué", response.getBody(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, factures, "Fichier chargé avec succès");


        } catch (IOException e) {
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*@PreAuthorize("hasRole('ROLE_ADMIN')")
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
                //facture = extractor.extractFacture(is);

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
    }*/

    @DeleteMapping(path = "/{id}")
    @PreAuthorize("hasRole('Admin') or hasRole('Agent')")
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
