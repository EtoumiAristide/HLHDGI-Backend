package com.elpandor.hlh.modules.hlh.rest;

import com.elpandor.hlh.common.service.impl.FileStorageServiceImpl;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.FactureLoadDto;
import com.elpandor.hlh.modules.hlh.model.dto.payload.AvoirRequest;
import com.elpandor.hlh.modules.hlh.model.dto.payload.FactureAvoirPayload;
import com.elpandor.hlh.modules.hlh.model.dto.payload.TokenResponse;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.BKExtractedData;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.BKExtratedData2;
import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment;
import com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte.DeloitteFactureDTO;
import com.elpandor.hlh.modules.hlh.model.dto.payload.hlh.*;
import com.elpandor.hlh.modules.hlh.model.dto.payload.zino.ZinoExtractedData;
import com.elpandor.hlh.modules.hlh.model.dto.payload.zino.ZinoExtractedDataOrdered;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.elpandor.hlh.modules.hlh.service.FactureLoadService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.service.impl.*;
import com.elpandor.hlh.modules.hlh.utils.*;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("api/v1/factures")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FactureApi {
    private Logger log = LoggerFactory.getLogger(FactureApi.class);

    private final FileStorageServiceImpl fileStorageService;
    private final FactureService factureService;
    private final FactureLoadService factureLoadService;
    private final ApimService hlhApimService;
    private final ApimService bkApimService;
    private final ApimService zinoApimService;
    private final ApimService camApimService;
    private final ApimService pagimApimService;
    private final EtablissementService etablissementService;
    private final PointVenteService pointVenteService;

    private BKExtractedData bkExtractedData;
    private List<ZinoExtractedData> extractedDatas;

    @Value("${bk.api.entreprise}")
    private String entrepriseBK;

    @Value("${hlh.api.entreprise}")
    private String entrepriseHLH;

    @Value("${zino.api.entreprise}")
    private String entrepriseZino;

    @Value("${cam.api.entreprise}")
    private String entrepriseCam;

    @Value("${pagim.api.entreprise}")
    private String entreprisePagim;

    @Autowired
    private DeloittePDFExtractor2 deloittePDFExtractor2;
    @Autowired
    private DeloittePDFExtractor3 deloittePDFExtractor3;
    @Autowired
    private DeloittePDFExtractor4 deloittePDFExtractor4;

    public FactureApi(FileStorageServiceImpl fileStorageService, FactureService factureService, FactureLoadService factureLoadService, HLHApimServiceImpl hlhApimService, BurgerKingApimServiceImpl burgerKingApimService, ZinoApimServiceImpl zinoApimService, CamApimServiceImpl camApimService, PAGIMApimServiceImpl pagimApimService, EtablissementService etablissementService, PointVenteService pointVenteService) {
        this.fileStorageService = fileStorageService;
        this.factureService = factureService;
        this.factureLoadService = factureLoadService;
        this.hlhApimService = hlhApimService;
        this.bkApimService = burgerKingApimService;
        this.zinoApimService = zinoApimService;
        this.camApimService = camApimService;
        this.pagimApimService = pagimApimService;
        this.etablissementService = etablissementService;
        this.pointVenteService = pointVenteService;
        this.deloittePDFExtractor2 = deloittePDFExtractor2;
        this.deloittePDFExtractor3 = deloittePDFExtractor3;
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

    @PostMapping(path = "/bynumfne/{numfacture}")
    public ResponseEntity<Map<String, Object>> getByNumeFactureFNE(@PathVariable String numfacture,
                                                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing get request for numfacture :" + numfacture);
        Map<String, Object> result = new HashMap<>();

        try {

            FactureDto factureDto = factureService.findByNumFactureFNE(numfacture);
            if (factureDto == null) {
                log.info("Entity having id not found, numfacture : " + numfacture);
                return Utilities.createSuccessResponse(HttpStatus.OK, factureDto, "Facture trouvé");
            }
            result.put("factureVente", factureDto);

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Entreprise agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));
            //PointVenteDto pointVenteDto = pointVenteService.get(Integer.valueOf(pointVente));

            if (file != null) {
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
                if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm") && !fileName.endsWith(".pdf")) {
                    log.error("Unsupported file type: {}", fileType);
                    return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }

                List<FacturePayload> factures = traitementFacture(etablissement, file, null, null, null, null, null);

                result.put("donneesExtraite", factures);
            }


            return Utilities.createSuccessResponse(HttpStatus.OK, result, "Fichier chargé et traité avec succès");

            //return Utilities.createErrorResponse("Facture avec le numero " + numfacture + " non trouvé", Optional.empty(), HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            log.error("IOException occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        if (groups == null) groups = List.of();
        if (groups.isEmpty())
            return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

        //Recuperation de l'établissement
        EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

        Page<FactureDto> pages = factureService.findByEntreprise(pageable, etablissement != null ? etablissement.getOrganisation().getRaisonSocial() : "");
        List<FactureDto> typeCartes = pages.getContent();
        if (!typeCartes.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des types d'intervention");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('Admin') or hasRole('Agent') or hasRole('Compta-BK')")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(name = "type", defaultValue = "FACTURE_VENTE") String typeFacture,
                                                          @RequestParam(name = "client", defaultValue = "B2C") String typeClient,
                                                          @RequestParam(name = "paiement", defaultValue = "cash") String modePaiement,
                                                          @RequestParam(name = "pointvente", defaultValue = "pv") String pointVente,
                                                          @RequestParam(name = "facturation", defaultValue = "") String facturation,
                                                          @RequestParam(name = "messageCommercial", defaultValue = "") String messageCommercial,
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
            PointVenteDto pointVenteDto = pointVenteService.get(Integer.valueOf(pointVente));

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
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm") && !fileName.endsWith(".pdf")) {
                log.error("Unsupported file type: {}", fileType);
                return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            List<FacturePayload> factures = traitementFacture(etablissement, file, typeFacture, typeClient, modePaiement, pointVenteDto, facturation);

            Map<String, Object> result = new HashMap<>();
            result.put("factures", factures);
            if (bkExtractedData != null && bkExtractedData.getPayments() != null) {
                result.put("payments", bkExtractedData.getPayments().stream().filter(payment -> payment.getTotal() != null).toList());
            }

            return Utilities.createSuccessResponse(HttpStatus.OK, result, "Fichier chargé et traité avec succès");

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
    public ResponseEntity<Map<String, Object>> save(@RequestParam(value = "file", required = false) MultipartFile file,
                                                    @RequestParam(name = "type", defaultValue = "FACTURE_VENTE") String typeFacture,
                                                    @RequestParam(name = "client", defaultValue = "B2C") String typeClient,
                                                    @RequestParam(name = "paiement", defaultValue = "cash") String modePaiement,
                                                    @RequestParam(name = "pointvente", defaultValue = "pv") String pointVente,
                                                    @RequestParam(name = "facturation", defaultValue = "") String facturation,
                                                    @RequestParam(name = "messageCommercial", defaultValue = "") String messageCommercial,
                                                    @RequestParam(name = "dataFacture", required = false) String dataFacture,
                                                    @RequestParam(name = "dataFactureLoadId", required = false) String dataFactureLoadId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing get request for save");
        try {

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));
            PointVenteDto pointVenteDto = pointVenteService.findByNom(pointVente);

            List<FacturePayload> factures = new ArrayList<>();
            FactureLoadDto factureLoadDto = null;

            if (file != null) {
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
                if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm")) {
                    log.error("Unsupported file type: {}", fileType);
                    return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                }
                factures = traitementFacture(etablissement, file, typeFacture, typeClient, modePaiement, pointVenteDto, facturation);
            }

            if (dataFacture != null) {
                factures = new ObjectMapper().readValue(dataFacture, new TypeReference<List<FacturePayload>>() {});
                if (dataFactureLoadId != null) {
                    factureLoadDto = factureLoadService.get(UUID.fromString(dataFactureLoadId));
                }
            }
//
            for (FacturePayload facture : factures) {
                facture.setReception(messageCommercial);
                //Appel de l'api DGI
                TokenResponse tokenResponse = null;
                ResponseEntity<String> response = null;
                String request = Json.pretty(facture);

                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseHLH)) {
                    tokenResponse = hlhApimService.auth();
                    response = hlhApimService.sendData(tokenResponse.getAccessToken(), facture);
                }

                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entreprisePagim)) {
                    tokenResponse = pagimApimService.auth();
                    System.out.println("tokenResponse " + tokenResponse);
                    response = pagimApimService.sendData(tokenResponse.getAccessToken(), facture);
                }


                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseBK)) {
                    tokenResponse = bkApimService.auth();
                    response = bkApimService.sendData(tokenResponse.getAccessToken(), facture);
                }
                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseZino)) {
                    tokenResponse = zinoApimService.auth();
                    response = zinoApimService.sendData(tokenResponse.getAccessToken(), facture);
                }

                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseCam)) {
                    tokenResponse = camApimService.auth();
                    response = camApimService.sendData(tokenResponse.getAccessToken(), facture);
                }
//                PointVenteDto pointVenteDto = pointVenteService.findByNom(pointVente);
//                assert response != null;

                //Gestion de la date de facture
                LocalDate dateFacture = null;
                try {
                    dateFacture = facture.getDateFacture() != null ? LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now();
                } catch (Exception ex) {
                    dateFacture = LocalDate.now();
                    ex.printStackTrace();
                }

                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    FactureDto factureDto = FactureDto.builder()
                            .numFacture(facture.getNumeroFacture())
                            .dateFacture(dateFacture)
                            .nomClient(facture.getClientPayload().getNom())
                            //.lienFichier(storeName)
                            .typeFacture(facture.getTypeFacture())
                            .typeClient(facture.getTypeClient())
                            .modePaiement(facture.getModePaiement())
                            .dataSend(request)
                            .reponseFNE(response.getBody())
                            .bkExtractedData(new ObjectMapper().writeValueAsString(bkExtractedData))
                            .pointVente(pointVenteDto)
                            .build();

                    factureService.saveOrUpdate(factureDto);

                    //On supprime la facture chare s'il y'a lieu
                    if (factureLoadDto != null) {
                        factureLoadService.delete(factureLoadDto.getId());
                    }
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

    @PostMapping("/partial-save")
    @PreAuthorize("hasRole('Compta-BK')")
    public ResponseEntity<Map<String, Object>> partialSave(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(name = "type", defaultValue = "FACTURE_VENTE") String typeFacture,
                                                           @RequestParam(name = "client", defaultValue = "B2C") String typeClient,
                                                           @RequestParam(name = "paiement", defaultValue = "cash") String modePaiement,
                                                           @RequestParam(name = "pointvente", defaultValue = "pv") String pointVente,
                                                           @RequestParam(name = "facturation", defaultValue = "") String facturation,
                                                           @RequestParam(name = "messageCommercial", defaultValue = "") String messageCommercial,
                                                           @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing get request for partial save");
        try {

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));
            PointVenteDto pointVenteDto = pointVenteService.findByNom(pointVente);

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
            if (fileName != null && !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm")) {
                log.error("Unsupported file type: {}", fileType);
                return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            List<FacturePayload> factures = traitementFacture(etablissement, file, typeFacture, typeClient, modePaiement, pointVenteDto, facturation);

            String data = Json.pretty(factures);

            FactureLoadDto factureDto = FactureLoadDto.builder()
                    .lienFichier(fileName)
                    .dataFacture(data)
                    .reception(messageCommercial)
                    .bkExtractedData(new ObjectMapper().writeValueAsString(bkExtractedData))
                    .pointVente(pointVenteDto)
                    .build();

            factureLoadService.saveOrUpdate(factureDto);

            return Utilities.createSuccessResponse(HttpStatus.OK, factures, "Fichier chargé avec succès");


        } catch (IOException e) {
            return Utilities.createErrorResponse("Une erreur est survenue lors du traitement du fichier", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/loaded/byentreprise")
    public ResponseEntity<Map<String, Object>> getAllLoadedByEntreprise(@RequestParam(value = "pageNum", defaultValue = "0", required = false) Integer pageNum, @RequestParam(value = "size", defaultValue = "10", required = false) Integer size, @AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing getAll request!");

        Pageable pageable = PageRequest.of(pageNum, size);

        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        if (groups == null) groups = List.of();
        if (groups.isEmpty())
            return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

        //Recuperation de l'établissement
        EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

        Page<FactureLoadDto> pages = factureLoadService.findByEntreprise(pageable, etablissement != null ? etablissement.getOrganisation().getRaisonSocial() : "");
        List<FactureLoadDto> factures = pages.getContent();
        if (!factures.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des factures chargées");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun Facture trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping(path = "/{id}/saved")
    public ResponseEntity<Map<String, Object>> getSaved(@PathVariable UUID id) {
        log.trace("Starting processing get request for id :" + id);

        FactureLoadDto factureDto = factureLoadService.get(id);
        if (factureDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, factureDto, "Facture trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Facture avec l'id " + id + " non trouvé");
    }

    @PostMapping("/avoir")
    @PreAuthorize("hasRole('Admin') or hasRole('Agent')")
    public ResponseEntity<Map<String, Object>> saveAvoir(@ModelAttribute AvoirRequest requestData,
                                                         @AuthenticationPrincipal Jwt jwt) {

        log.trace("Starting processing get request for saveAvoir");
        try {

            //Recuperation du group
            List<String> groups = jwt.getClaim("groups");
            if (groups == null) groups = List.of();
            if (groups.isEmpty())
                return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

            //Recuperation de l'établissement
            EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

            FactureDto factureSearch = factureService.findByNumFactureFNE(requestData.getNumeroFacture());
            if (factureSearch == null)
                return Utilities.createErrorResponse("Facture avec le numero " + requestData.getNumeroFacture() + " non trouvé", Optional.empty(), HttpStatus.NOT_FOUND);

            factureSearch.setTypeFacture(TypeFacture.FACTURE_AVOIR);

            List<String> finalGroups = groups;

//            for (FacturePayload facture : factures) {
            //Appel de l'api DGI
            TokenResponse tokenResponse = null;
            ResponseEntity<String> response = null;
            String request = factureSearch.getReponseFNE();

            Gson gson = new Gson();
            JsonObject facture = new JsonObject();
            JsonObject respondeFNE = gson.fromJson(factureSearch.getReponseFNE(), JsonObject.class);
            facture.addProperty("typeFacture", requestData.getType());
            requestData.setNumeroFacture(respondeFNE.get("invoice").getAsJsonObject().get("id").getAsString());
            if (etablissement.getOrganisation().getIsAvoirFirstVersion()) {
                facture.add("data", gson.fromJson(factureSearch.getReponseFNE(), JsonObject.class));
            } else {
                facture.addProperty("data", gson.toJson(requestData));
            }

            if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseHLH)) {
                tokenResponse = hlhApimService.auth();
                response = hlhApimService.sendData(tokenResponse.getAccessToken(), facture);
            }

            if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entreprisePagim)) {
                tokenResponse = pagimApimService.auth();
                response = pagimApimService.sendData(tokenResponse.getAccessToken(), facture);
            }

            if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseBK)) {
                tokenResponse = bkApimService.auth();
                response = bkApimService.sendData(tokenResponse.getAccessToken(), facture);
            }
            if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseZino)) {
                tokenResponse = zinoApimService.auth();
                response = zinoApimService.sendData(tokenResponse.getAccessToken(), facture);
            }
            if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseCam)) {
                tokenResponse = camApimService.auth();
                response = camApimService.sendData(tokenResponse.getAccessToken(), facture);
            }

            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                factureSearch.setDataSend(request);
                factureSearch.setReponseFNE(response.getBody());
                factureSearch.setId(null);

                factureService.saveOrUpdate(factureSearch);
            } else {
                log.error("L'authentification de la facture à échoué");
                return Utilities.createErrorResponse("L'authentification de la facture à échoué", response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
//            }
            return Utilities.createSuccessResponse(HttpStatus.OK, factureSearch, "Fichier chargé avec succès");


        } catch (Exception e) {
            log.error("Exception occurred while processing file", e);
            return Utilities.createErrorResponse("Une erreur interne est survenue", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private List<FacturePayload> traitementFacture(EtablissementDto etablissement, MultipartFile file, String typeFacture, String typeClient, String modePaiement, PointVenteDto pointVente, String facturation) throws IOException {
        List<FacturePayload> factures = new ArrayList<>();

        if (etablissement.getOrganisation() != null) {

            switch (etablissement.getOrganisation().getRaisonSocial().toUpperCase()) {
                case "HOTEL AND LUXURY HOUSING", "CAM & SONS ENTREPRISES", "PAGIM SERVICES SARL":
                    factures = traitementFactureHLH(file.getInputStream(), etablissement);
                    bkExtractedData = null;
                    break;
                case "SIA RESTAURATION RAPIDE COTE D'IVOIRE":
//                    factures = traitementFactureBK(file.getInputStream(), etablissement);
                    factures = traitementFactureBK2(file.getInputStream(), etablissement);
                    break;
                case "ZINO COTE D'IVOIRE":
                    bkExtractedData = null;
                    factures = facturation != null && facturation.equalsIgnoreCase("FACTURE_CONSOLIDE") ? traitementFactureHLH(file.getInputStream(), etablissement) : traitementFactureZino(file.getInputStream(), etablissement);
                    break;
                case "DELOITTE COTE D'IVOIRE":
                    traitementFactureDeloitte(file.getBytes(), etablissement);
                    bkExtractedData = null;
                    break;
                default:
                    System.out.println("Entreprise" + etablissement.getOrganisation().getRaisonSocial() + " non prise en charge");
            }

//                if (!etablissement.getOrganisation().getIsFactureInitiale()) {
//                } else {
//
//                }
            //System.out.println("factures " + factures);
        }
//        List<String> finalGroups = groups;
        factures.forEach(facturePayload -> {
            facturePayload.setTypeFacture(typeFacture != null ? TypeFacture.valueOf(typeFacture) : null);
            facturePayload.setTypeClient(typeClient != null ? TypeClient.valueOf(typeClient) : null);
            if (etablissement.getOrganisation().getIsOrderedByPaiementMethod()) {
                //facturePayload.setModePaiement(facturePayload.getSheetName().toLowerCase().contains("mobile money") ? ModePaiement.mobilemoney : (facturePayload.getSheetName().equalsIgnoreCase("cash") ? ModePaiement.cash : ModePaiement.card));

                if (facturePayload.getSheetName().toLowerCase().contains("mobile money".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("Wave".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("Orange".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("MTN".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("MOOV".toLowerCase()))
                    facturePayload.setModePaiement(ModePaiement.mobilemoney);

                if (facturePayload.getSheetName().toLowerCase().contains("cash".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("Espèces".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("Glovo".toLowerCase()))
                    facturePayload.setModePaiement(ModePaiement.cash);

                if (facturePayload.getSheetName().toLowerCase().contains("CC".toLowerCase())
                        || facturePayload.getSheetName().toLowerCase().contains("Carte Bancaire".toLowerCase()))
                    facturePayload.setModePaiement(ModePaiement.card);

                if (facturePayload.getSheetName().toLowerCase().contains("Chèque".toLowerCase()))
                    facturePayload.setModePaiement(ModePaiement.check);


                if (etablissement.getOrganisation().getIsPrixUnitaireDefined()) {
                    //On ajoute le prix unitaire dans les données
                    facturePayload.getLignes().forEach(ligneProduitPayload -> {
                        ligneProduitPayload.setPrixUnitaireHT(ligneProduitPayload.getMontantHT() / ligneProduitPayload.getQuantite());
                    });
                }

                if (facturation != null && facturation.equalsIgnoreCase("FACTURE_CONSOLIDE")) {
                    facturePayload.setModePaiement(modePaiement != null ? ModePaiement.valueOf(modePaiement) : null);
                }
            } else {
                facturePayload.setModePaiement(modePaiement != null ? ModePaiement.valueOf(modePaiement) : null);
            }
//            facturePayload.setEntreprise(etablissement.getNom());
            if (pointVente != null) {
                facturePayload.setEntreprise(pointVente.getEtablissement().getNom());
                facturePayload.setPointVente(pointVente.getNom());
            }
            if (facturePayload.getClientPayload() != null && facturePayload.getClientPayload().getNumeroCC() == null)
                facturePayload.getClientPayload().setNumeroCC("");

            facturePayload.setPourcentageTVA(etablissement.getOrganisation().getValeurTVA());
            facturePayload.setPourcentageTDT(etablissement.getOrganisation().getValeurTDT());
            facturePayload.setValeurTCN(etablissement.getOrganisation().getValeurTCN());
        });

        return factures;
    }

    private List<FacturePayload> traitementFactureHLH(InputStream is, EtablissementDto etablissement) throws IOException {
        List<FacturePayload> factures = new HLHExcelFactureExtractor().extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier());

        //Mies des valeurs de taux par défaut si non trouvé
        /*factures.forEach(facture -> {
            if (facture.getTotauxPayload().getTva().getMontant() != 0 && facture.getTotauxPayload().getTva().getTaux() < etablissement.getOrganisation().getValeurTVA()) {
                facture.getTotauxPayload().getTva().setTaux(etablissement.getOrganisation().getValeurTVA());
            }
            if (facture.getTotauxPayload().getTdt().getMontant() != 0 && facture.getTotauxPayload().getTdt().getTaux() < etablissement.getOrganisation().getValeurTDT()) {
                facture.getTotauxPayload().getTdt().setTaux(etablissement.getOrganisation().getValeurTDT());
            }
            if (facture.getTotauxPayload().getTcn().getMontant() != 0 && facture.getTotauxPayload().getTcn().getTaux() < etablissement.getOrganisation().getValeurTCN()) {
                facture.getTotauxPayload().getTcn().setTaux(etablissement.getOrganisation().getValeurTCN());
            }
        });*/

        return factures;
    }

    private List<FacturePayload> traitementFactureBK(InputStream is, EtablissementDto etablissement) throws IOException {
        bkExtractedData = new BKExcelDataExtraction().extractDataFromExcel(is, etablissement.getOrganisation().getIndexLectureFichier());
//                    System.out.println("bkExtractedData " + bkExtractedData);
        //Constitution de la facture
        FacturePayload factureCash = new FacturePayload();
        FacturePayload factureCC = new FacturePayload();
        FacturePayload factureWave = new FacturePayload();
        List<LigneProduitPayload> ligneProduitsCash = new ArrayList<>();
        List<LigneProduitPayload> ligneProduitsCC = new ArrayList<>();
        List<LigneProduitPayload> ligneProduitsWave = new ArrayList<>();

        //CASH
        AtomicReference<Double> totalCash = new AtomicReference<>((double) 0);
        AtomicReference<Double> totalCash2 = new AtomicReference<>((double) 0);
        AtomicReference<Integer> nbCash = new AtomicReference<>((int) 0);
        AtomicReference<Integer> nbCash2 = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> (payment.getPaymentType() == Payment.PaymentType.CASH || payment.getPaymentType() == Payment.PaymentType.HD_GLOVO) && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalCash.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        if (payment.getAmount().doubleValue() < 5000) {
                            totalCash2.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbCash2.updateAndGet(v -> (v + 1));
                        } else {
                            totalCash.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbCash.updateAndGet(v -> (v + 1));
                        }
                    }
                });
        LigneProduitPayload ligneProduitCash = new LigneProduitPayload();
        ligneProduitCash.setProduit("Ventes en espèce");
        ligneProduitCash.setMontantHT(totalCash.get());
        ligneProduitCash.setQuantite(nbCash.get());
        LigneProduitPayload ligneProduitCash2 = new LigneProduitPayload();
        ligneProduitCash2.setProduit("Ventes en espèce");
        ligneProduitCash2.setMontantHT(totalCash2.get());
        ligneProduitCash2.setQuantite(nbCash2.get());
        if (nbCash.get() != 0) ligneProduitsCash.add(ligneProduitCash);
        if (nbCash2.get() != 0) ligneProduitsCash.add(ligneProduitCash2);

        ClientPayload clientCash = new ClientPayload();
        clientCash.setNom("CASH");
        clientCash.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadCash = new TotauxPayload();
        totauxPayloadCash.setHt(totalCash.get() + totalCash2.get());

        TaxePayload tdtCash = new TaxePayload();
        tdtCash.setBase(totalCash.get() + totalCash2.get());
        tdtCash.setTaux(1.5);
        tdtCash.setMontant((totalCash.get() + totalCash2.get()) * 0.015);//1.5%
        totauxPayloadCash.setTdt(tdtCash);

        TaxePayload tvaCash = new TaxePayload();
        tvaCash.setTaux(18.0);
        tvaCash.setBase((totalCash.get() + totalCash2.get()) + tdtCash.getMontant());
        tvaCash.setMontant(tvaCash.getBase() * 0.18);
        totauxPayloadCash.setTva(tvaCash);

        totauxPayloadCash.setTtc(totauxPayloadCash.getHt() + tdtCash.getMontant() + tvaCash.getMontant());
        totauxPayloadCash.setModePaiement(ModePaiement.cash.toString());


        factureCash.setSheetName("CASH");
        factureCash.setLignes(ligneProduitsCash);
        factureCash.setClientPayload(clientCash);
        factureCash.setTotauxPayload(totauxPayloadCash);

        //WAVE
        AtomicReference<Double> totalWave = new AtomicReference<>((double) 0);
        AtomicReference<Double> totalWave2 = new AtomicReference<>((double) 0);
        AtomicReference<Integer> nbWave = new AtomicReference<>((int) 0);
        AtomicReference<Integer> nbWave2 = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> payment.getPaymentType() == Payment.PaymentType.CASH_WAVE && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalWave.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        if (payment.getAmount().doubleValue() < 5000) {
                            totalWave2.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbWave2.updateAndGet(v -> (v + 1));
                        } else {
                            totalWave.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbWave.updateAndGet(v -> (v + 1));
                        }
                    }
                });
        LigneProduitPayload ligneProduitWave = new LigneProduitPayload();
        ligneProduitWave.setProduit("Ventes via Wave");
        ligneProduitWave.setMontantHT(totalWave.get());
        ligneProduitWave.setQuantite(nbWave.get());
        LigneProduitPayload ligneProduitWave2 = new LigneProduitPayload();
        ligneProduitWave2.setProduit("Ventes via Wave");
        ligneProduitWave2.setMontantHT(totalWave2.get());
        ligneProduitWave2.setQuantite(nbWave2.get());
        if (nbWave.get() != 0) ligneProduitsWave.add(ligneProduitWave);
        if (nbWave2.get() != 0) ligneProduitsWave.add(ligneProduitWave2);

        ClientPayload clientWave = new ClientPayload();
        clientWave.setNom("WAVE");
        clientWave.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadWave = new TotauxPayload();
        totauxPayloadWave.setHt(totalWave.get() + totalWave2.get());

        TaxePayload tdtWave = new TaxePayload();
        tdtWave.setBase(totalWave.get() + totalWave2.get());
        tdtWave.setTaux(1.5);
        tdtWave.setMontant((totalWave.get() + totalWave2.get()) * 0.015);//1.5%
        totauxPayloadWave.setTdt(tdtWave);

        TaxePayload tvaWave = new TaxePayload();
        tvaWave.setTaux(18.0);
        tvaWave.setBase((totalWave.get() + totalWave2.get()) + tdtWave.getMontant());
        tvaWave.setMontant(tvaWave.getBase() * 0.18);
        totauxPayloadWave.setTva(tvaWave);

        totauxPayloadWave.setTtc(totauxPayloadWave.getHt() + tdtWave.getMontant() + tvaWave.getMontant());
        totauxPayloadWave.setModePaiement(ModePaiement.mobilemoney.toString());

        factureWave.setSheetName("Mobile Money");
        factureWave.setLignes(ligneProduitsWave);
        factureWave.setClientPayload(clientWave);
        factureWave.setTotauxPayload(totauxPayloadWave);

        //CC
        AtomicReference<Double> totalCC = new AtomicReference<>((double) 0);
        AtomicReference<Double> totalCC2 = new AtomicReference<>((double) 0);
        AtomicReference<Integer> nbCC = new AtomicReference<>((int) 0);
        AtomicReference<Integer> nbCC2 = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> payment.getPaymentType() == Payment.PaymentType.BACKUP_CC && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalCC.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        if (payment.getAmount().doubleValue() < 5000) {
                            totalCC2.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbCC2.updateAndGet(v -> (v + 1));
                        } else {
                            totalCC.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                            nbCC.updateAndGet(v -> (v + 1));
                        }
                    }
                });
        LigneProduitPayload ligneProduitCC = new LigneProduitPayload();
        ligneProduitCC.setProduit("Ventes via cartes bancaires");
        ligneProduitCC.setMontantHT(totalCC.get());
        ligneProduitCC.setQuantite(nbCC.get());
        LigneProduitPayload ligneProduitCC2 = new LigneProduitPayload();
        ligneProduitCC2.setProduit("Ventes via cartes bancaires");
        ligneProduitCC2.setMontantHT(totalCC2.get());
        ligneProduitCC2.setQuantite(nbCC2.get());
        if (nbCC.get() != 0) ligneProduitsCC.add(ligneProduitCC);
        if (nbCC2.get() != 0) ligneProduitsCC.add(ligneProduitCC2);

        ClientPayload clientCC = new ClientPayload();
        clientCC.setNom("CC");
        clientCC.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadCC = new TotauxPayload();
        totauxPayloadCC.setHt(totalCC.get() + totalCC2.get());

        TaxePayload tdtCC = new TaxePayload();
        tdtCC.setBase(totalCC.get() + totalCC2.get());
        tdtCC.setTaux(1.5);
        tdtCC.setMontant((totalCC.get() + totalCC2.get()) * 0.015);//1.5%
        totauxPayloadCC.setTdt(tdtCC);

        TaxePayload tvaCC = new TaxePayload();
        tvaCC.setTaux(18.0);
        tvaCC.setBase((totalCC.get() + totalCC2.get()) + tdtCC.getMontant());
        tvaCC.setMontant(tvaCC.getBase() * 0.18);
        totauxPayloadCC.setTva(tvaCC);

        totauxPayloadCC.setTtc(totauxPayloadCC.getHt() + tdtCC.getMontant() + tvaCC.getMontant());
        totauxPayloadCC.setModePaiement(ModePaiement.card.toString());

        factureCC.setSheetName("CC");
        factureCC.setLignes(ligneProduitsCC);
        factureCC.setClientPayload(clientCC);
        factureCC.setTotauxPayload(totauxPayloadCC);

        //Mise à jour de la facture générale
        List<FacturePayload> factures = new ArrayList<>();
        if (!factureCash.getLignes().isEmpty())
            factures.add(factureCash);
        if (!factureWave.getLignes().isEmpty())
            factures.add(factureWave);
        if (!factureCC.getLignes().isEmpty())
            factures.add(factureCC);
//        return List.of(factureCash, factureWave, factureCC);
        return factures;
    }

    private List<FacturePayload> traitementFactureBK2(InputStream is, EtablissementDto etablissement) throws IOException {
        //return new BKExcelDataExtraction2().extractAllSheetsData(is);
        Map<String, List<BKExtratedData2>> bkExtratedData2List = new BKExcelDataExtraction2().extractAllSheetsData(is);

        //Constitution de la facture
        FacturePayload factureCash = new FacturePayload();
        FacturePayload factureCC = new FacturePayload();
        FacturePayload factureWave = new FacturePayload();
        FacturePayload factureGlovo = new FacturePayload();
        List<LigneProduitPayload> ligneProduitsCash = new ArrayList<>();
        List<LigneProduitPayload> ligneProduitsCC = new ArrayList<>();
        List<LigneProduitPayload> ligneProduitsWave = new ArrayList<>();
        List<LigneProduitPayload> ligneProduitsGlovo = new ArrayList<>();

        if (bkExtratedData2List.containsKey("CASH")) {
            //CASH
            AtomicReference<Double> totalCash = new AtomicReference<>((double) 0);
            AtomicReference<Double> totalCash2 = new AtomicReference<>((double) 0);
            AtomicReference<Integer> nbCash = new AtomicReference<>((int) 0);
            AtomicReference<Integer> nbCash2 = new AtomicReference<>((int) 0);

            bkExtratedData2List.get("CASH").forEach(bkExtratedData2 -> {
                if (!bkExtratedData2.getCheckNumber().toLowerCase().contains("total")) {
                    if (bkExtratedData2.getHt() < 5000) {
                        totalCash2.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbCash2.updateAndGet(v -> (v + 1));
                    } else {
                        totalCash.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbCash.updateAndGet(v -> (v + 1));
                    }
                } else {
                    factureCash.setNumeroFacture(bkExtratedData2.getReference());
                }
                factureCash.setDateFacture(bkExtratedData2.getDate());
            });

            LigneProduitPayload ligneProduitCash = new LigneProduitPayload();
            ligneProduitCash.setDate(factureCash.getDateFacture());
            ligneProduitCash.setProduit("Ventes en espèce");
            ligneProduitCash.setMontantHT(totalCash.get());
            ligneProduitCash.setQuantite(nbCash.get());
            LigneProduitPayload ligneProduitCash2 = new LigneProduitPayload();
            ligneProduitCash2.setDate(factureCash.getDateFacture());
            ligneProduitCash2.setProduit("Ventes en espèce");
            ligneProduitCash2.setMontantHT(totalCash2.get());
            ligneProduitCash2.setQuantite(nbCash2.get());
            if (nbCash.get() != 0) ligneProduitsCash.add(ligneProduitCash);
            if (nbCash2.get() != 0) ligneProduitsCash.add(ligneProduitCash2);

            ClientPayload clientCash = new ClientPayload();
            clientCash.setNom("CASH-" + factureCash.getNumeroFacture());
            clientCash.setNumeroCC("");

            //Montant & taxes
            TotauxPayload totauxPayloadCash = new TotauxPayload();
            totauxPayloadCash.setHt(totalCash.get() + totalCash2.get());

            TaxePayload tdtCash = new TaxePayload();
            tdtCash.setBase(totalCash.get() + totalCash2.get());
            tdtCash.setTaux(etablissement.getOrganisation().getValeurTDT());
            tdtCash.setMontant((totalCash.get() + totalCash2.get()) * (tdtCash.getTaux() / 100));
            totauxPayloadCash.setTdt(tdtCash);

            TaxePayload tvaCash = new TaxePayload();
            tvaCash.setTaux(etablissement.getOrganisation().getValeurTVA());
            tvaCash.setBase((totalCash.get() + totalCash2.get()) + tdtCash.getMontant());
            tvaCash.setMontant(tvaCash.getBase() * (tvaCash.getTaux() / 100));
            totauxPayloadCash.setTva(tvaCash);

            totauxPayloadCash.setTtc(totauxPayloadCash.getHt() + tdtCash.getMontant() + tvaCash.getMontant());
            totauxPayloadCash.setModePaiement(ModePaiement.cash.toString());


            factureCash.setSheetName("CASH-" + factureCash.getNumeroFacture());
            factureCash.setLignes(ligneProduitsCash);
            factureCash.setClientPayload(clientCash);
            factureCash.setTotauxPayload(totauxPayloadCash);
        }

        if (bkExtratedData2List.containsKey("CC")) {
            //CASH
            AtomicReference<Double> totalCC = new AtomicReference<>((double) 0);
            AtomicReference<Double> totalCC2 = new AtomicReference<>((double) 0);
            AtomicReference<Integer> nbCC = new AtomicReference<>((int) 0);
            AtomicReference<Integer> nbCC2 = new AtomicReference<>((int) 0);

            bkExtratedData2List.get("CC").forEach(bkExtratedData2 -> {
                if (!bkExtratedData2.getCheckNumber().toLowerCase().contains("total")) {
                    if (bkExtratedData2.getHt() < 5000) {
                        totalCC2.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbCC2.updateAndGet(v -> (v + 1));
                    } else {
                        totalCC.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbCC.updateAndGet(v -> (v + 1));
                    }
                } else {
                    factureCC.setNumeroFacture(bkExtratedData2.getReference());
                }
                factureCC.setDateFacture(bkExtratedData2.getDate());
            });

            LigneProduitPayload ligneProduitCC = new LigneProduitPayload();
            ligneProduitCC.setDate(factureCC.getDateFacture());
            ligneProduitCC.setProduit("Ventes en espèce");
            ligneProduitCC.setMontantHT(totalCC.get());
            ligneProduitCC.setQuantite(nbCC.get());
            LigneProduitPayload ligneProduitCC2 = new LigneProduitPayload();
            ligneProduitCC2.setDate(factureCC.getDateFacture());
            ligneProduitCC2.setProduit("Ventes via carte bancaire");
            ligneProduitCC2.setMontantHT(totalCC2.get());
            ligneProduitCC2.setQuantite(nbCC2.get());
            if (nbCC.get() != 0) ligneProduitsCC.add(ligneProduitCC);
            if (nbCC2.get() != 0) ligneProduitsCC.add(ligneProduitCC2);

            ClientPayload clientCC = new ClientPayload();
            clientCC.setNom("CC-" + factureCC.getNumeroFacture());
            clientCC.setNumeroCC("");

            //Montant & taxes
            TotauxPayload totauxPayloadCC = new TotauxPayload();
            totauxPayloadCC.setHt(totalCC.get() + totalCC2.get());

            TaxePayload tdtCC = new TaxePayload();
            tdtCC.setBase(totalCC.get() + totalCC2.get());
            tdtCC.setTaux(etablissement.getOrganisation().getValeurTDT());
            tdtCC.setMontant((totalCC.get() + totalCC2.get()) * (tdtCC.getTaux() / 100));
            totauxPayloadCC.setTdt(tdtCC);

            TaxePayload tvaCC = new TaxePayload();
            tvaCC.setTaux(etablissement.getOrganisation().getValeurTVA());
            tvaCC.setBase((totalCC.get() + totalCC2.get()) + tdtCC.getMontant());
            tvaCC.setMontant(tvaCC.getBase() * (tvaCC.getTaux() / 100));
            totauxPayloadCC.setTva(tvaCC);

            totauxPayloadCC.setTtc(totauxPayloadCC.getHt() + tdtCC.getMontant() + tvaCC.getMontant());
            totauxPayloadCC.setModePaiement(ModePaiement.card.toString());


            factureCC.setSheetName("CC-" + factureCC.getNumeroFacture());
            factureCC.setLignes(ligneProduitsCC);
            factureCC.setClientPayload(clientCC);
            factureCC.setTotauxPayload(totauxPayloadCC);
        }

        if (bkExtratedData2List.containsKey("HD GLOVO")) {
            //CASH
            AtomicReference<Double> totalGlovo = new AtomicReference<>((double) 0);
            AtomicReference<Double> totalGlovo2 = new AtomicReference<>((double) 0);
            AtomicReference<Integer> nbGlovo = new AtomicReference<>((int) 0);
            AtomicReference<Integer> nbGlovo2 = new AtomicReference<>((int) 0);

            bkExtratedData2List.get("HD GLOVO").forEach(bkExtratedData2 -> {
                if (!bkExtratedData2.getCheckNumber().toLowerCase().contains("total")) {
                    if (bkExtratedData2.getHt() < 5000) {
                        totalGlovo2.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbGlovo2.updateAndGet(v -> (v + 1));
                    } else {
                        totalGlovo.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbGlovo.updateAndGet(v -> (v + 1));
                    }
                } else {
                    factureGlovo.setNumeroFacture(bkExtratedData2.getReference());
                }
                factureGlovo.setDateFacture(bkExtratedData2.getDate());
            });

            LigneProduitPayload ligneProduitGlovo = new LigneProduitPayload();
            ligneProduitGlovo.setDate(factureGlovo.getDateFacture());
            ligneProduitGlovo.setProduit("Ventes via Glovo");
            ligneProduitGlovo.setMontantHT(totalGlovo.get());
            ligneProduitGlovo.setQuantite(nbGlovo.get());
            LigneProduitPayload ligneProduitGlovo2 = new LigneProduitPayload();
            ligneProduitGlovo2.setDate(factureGlovo.getDateFacture());
            ligneProduitGlovo2.setProduit("Ventes via Glovo");
            ligneProduitGlovo2.setMontantHT(totalGlovo2.get());
            ligneProduitGlovo2.setQuantite(nbGlovo2.get());
            if (nbGlovo.get() != 0) ligneProduitsGlovo.add(ligneProduitGlovo);
            if (nbGlovo2.get() != 0) ligneProduitsGlovo.add(ligneProduitGlovo2);

            ClientPayload clientGlovo = new ClientPayload();
            clientGlovo.setNom("HD Glovo-" + factureGlovo.getNumeroFacture());
            clientGlovo.setNumeroCC("");

            //Montant & taxes
            TotauxPayload totauxPayloadGlovo = new TotauxPayload();
            totauxPayloadGlovo.setHt(totalGlovo.get() + totalGlovo2.get());

            TaxePayload tdtGlovo = new TaxePayload();
            tdtGlovo.setBase(totalGlovo.get() + totalGlovo2.get());
            tdtGlovo.setTaux(etablissement.getOrganisation().getValeurTDT());
            tdtGlovo.setMontant((totalGlovo.get() + totalGlovo2.get()) * (tdtGlovo.getTaux() / 100));
            totauxPayloadGlovo.setTdt(tdtGlovo);

            TaxePayload tvaGlovo = new TaxePayload();
            tvaGlovo.setTaux(etablissement.getOrganisation().getValeurTVA());
            tvaGlovo.setBase((totalGlovo.get() + totalGlovo2.get()) + tdtGlovo.getMontant());
            tvaGlovo.setMontant(tvaGlovo.getBase() * (tvaGlovo.getTaux() / 100));
            totauxPayloadGlovo.setTva(tvaGlovo);

            totauxPayloadGlovo.setTtc(totauxPayloadGlovo.getHt() + tdtGlovo.getMontant() + tvaGlovo.getMontant());
            totauxPayloadGlovo.setModePaiement(ModePaiement.cash.toString());


            factureGlovo.setSheetName("HD GLOVO-" + factureGlovo.getNumeroFacture());
            factureGlovo.setLignes(ligneProduitsGlovo);
            factureGlovo.setClientPayload(clientGlovo);
            factureGlovo.setTotauxPayload(totauxPayloadGlovo);
        }

        if (bkExtratedData2List.containsKey("WAVE")) {
            //CASH
            AtomicReference<Double> totalWave = new AtomicReference<>((double) 0);
            AtomicReference<Double> totalWave2 = new AtomicReference<>((double) 0);
            AtomicReference<Integer> nbWave = new AtomicReference<>((int) 0);
            AtomicReference<Integer> nbWave2 = new AtomicReference<>((int) 0);

            bkExtratedData2List.get("WAVE").forEach(bkExtratedData2 -> {
                if (!bkExtratedData2.getCheckNumber().toLowerCase().contains("total")) {
                    if (bkExtratedData2.getHt() < 5000) {
                        totalWave2.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbWave2.updateAndGet(v -> (v + 1));
                    } else {
                        totalWave.updateAndGet(v -> (v + bkExtratedData2.getHt()));
                        nbWave.updateAndGet(v -> (v + 1));
                    }
                } else {
                    factureWave.setNumeroFacture(bkExtratedData2.getReference());
                }
                factureWave.setDateFacture(bkExtratedData2.getDate());
            });

            LigneProduitPayload ligneProduitWave = new LigneProduitPayload();
            ligneProduitWave.setDate(factureWave.getDateFacture());
            ligneProduitWave.setProduit("Ventes via wave");
            ligneProduitWave.setMontantHT(totalWave.get());
            ligneProduitWave.setQuantite(nbWave.get());
            LigneProduitPayload ligneProduitWave2 = new LigneProduitPayload();
            ligneProduitWave2.setDate(factureWave.getDateFacture());
            ligneProduitWave2.setProduit("Ventes via wave");
            ligneProduitWave2.setMontantHT(totalWave2.get());
            ligneProduitWave2.setQuantite(nbWave2.get());
            if (nbWave.get() != 0) ligneProduitsWave.add(ligneProduitWave);
            if (nbWave2.get() != 0) ligneProduitsWave.add(ligneProduitWave2);

            ClientPayload clientWave = new ClientPayload();
            clientWave.setNom("WAVE-" + factureWave.getNumeroFacture());
            clientWave.setNumeroCC("");

            //Montant & taxes
            TotauxPayload totauxPayloadWave = new TotauxPayload();
            totauxPayloadWave.setHt(totalWave.get() + totalWave2.get());

            TaxePayload tdtWave = new TaxePayload();
            tdtWave.setBase(totalWave.get() + totalWave2.get());
            tdtWave.setTaux(etablissement.getOrganisation().getValeurTDT());
            tdtWave.setMontant((totalWave.get() + totalWave2.get()) * (tdtWave.getTaux() / 100));
            totauxPayloadWave.setTdt(tdtWave);

            TaxePayload tvaWave = new TaxePayload();
            tvaWave.setTaux(etablissement.getOrganisation().getValeurTVA());
            tvaWave.setBase((totalWave.get() + totalWave2.get()) + tdtWave.getMontant());
            tvaWave.setMontant(tvaWave.getBase() * (tvaWave.getTaux() / 100));
            totauxPayloadWave.setTva(tvaWave);

            totauxPayloadWave.setTtc(totauxPayloadWave.getHt() + tdtWave.getMontant() + tvaWave.getMontant());
            totauxPayloadWave.setModePaiement(ModePaiement.mobilemoney.toString());


            factureWave.setSheetName("WAVE-" + factureWave.getNumeroFacture());
            factureWave.setLignes(ligneProduitsWave);
            factureWave.setClientPayload(clientWave);
            factureWave.setTotauxPayload(totauxPayloadWave);
        }

        //Mise à jour de la facture générale
        List<FacturePayload> factures = new ArrayList<>();
        if (!factureCash.getLignes().isEmpty())
            factures.add(factureCash);
        if (!factureWave.getLignes().isEmpty())
            factures.add(factureWave);
        if (!factureCC.getLignes().isEmpty())
            factures.add(factureCC);
        if (!factureGlovo.getLignes().isEmpty())
            factures.add(factureGlovo);
//        return List.of(factureCash, factureWave, factureCC);
        return factures;
    }

    private List<FacturePayload> traitementFactureZino(InputStream is, EtablissementDto etablissement) throws IOException {
        List<FacturePayload> factures = new ArrayList<>();

        //System.out.println(new ZinoExcelDataExtraction().extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier()));
        ZinoExcelDataExtraction zinoExcelDataExtraction = new ZinoExcelDataExtraction();

        //Extraction des données brutes
        extractedDatas = zinoExcelDataExtraction.extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier());

        //Trie pas mode de paiement
        List<ZinoExtractedDataOrdered> extractedDataOrdereds = zinoExcelDataExtraction.calculerRepartitionParModePaiement(extractedDatas);
//        System.out.println("extractedDataOrdereds " + extractedDataOrdereds);
        //Constitution de la liste facture
        extractedDataOrdereds.forEach(zinoExtractedDataOrdered -> {
            FacturePayload facture = new FacturePayload();
            List<LigneProduitPayload> ligneProduits = new ArrayList<>();

            LigneProduitPayload ligneProduit = new LigneProduitPayload();
            ligneProduit.setProduit("Ventes via " + zinoExtractedDataOrdered.getModePaiement());
            ligneProduit.setPrixUnitaireHT(zinoExtractedDataOrdered.getTotalMontantHT());
            ligneProduit.setMontantHT(zinoExtractedDataOrdered.getTotalMontantHT());
            ligneProduit.setQuantite(zinoExtractedDataOrdered.getNombreTransactions());
            ligneProduit.setDate(zinoExtractedDataOrdered.getDate() != null ? new SimpleDateFormat("dd/MM/yyyy").format(zinoExtractedDataOrdered.getDate()) : null);
            ligneProduits.add(ligneProduit);

            ClientPayload client = new ClientPayload();
            client.setNom(zinoExtractedDataOrdered.getModePaiement() + " - " + zinoExtractedDataOrdered.getSheetName().toLowerCase().replaceAll("fne", ""));
            client.setNumeroCC("");

            //Montant & taxes
            TotauxPayload totauxPayload = new TotauxPayload();
            totauxPayload.setHt(zinoExtractedDataOrdered.getTotalMontantHT());

//            TaxePayload tdt = new TaxePayload();
//            tdt.setBase(total.get());
//            tdt.setTaux(1.5);
//            tdt.setMontant(total.get() * 0.015);//1.5%
//            totauxPayload.setTdt(tdt);

            TaxePayload tva = new TaxePayload();
            tva.setTaux(18.0);
            tva.setBase(zinoExtractedDataOrdered.getTotalMontantHT());
            tva.setMontant(zinoExtractedDataOrdered.getTotalMontantHT() * 0.18);
            totauxPayload.setTva(tva);

            totauxPayload.setTtc(totauxPayload.getHt() + tva.getMontant());
            totauxPayload.setModePaiement(zinoExtractedDataOrdered.getModePaiement());

            facture.setDateFacture(zinoExtractedDataOrdered.getDate() != null ? new SimpleDateFormat("dd/MM/yyyy").format(zinoExtractedDataOrdered.getDate()) : null);
            facture.setSheetName(zinoExtractedDataOrdered.getModePaiement() + " - " + zinoExtractedDataOrdered.getSheetName().toLowerCase().replaceAll("fne", ""));
            facture.setLignes(ligneProduits);
            facture.setClientPayload(client);
            facture.setTotauxPayload(totauxPayload);

            factures.add(facture);
        });

        return factures;

    }

    private void traitementFactureDeloitte(byte[] is, EtablissementDto etablissement) throws IOException {
        //DeloittePDFExtractor deloittePDFExtractor = new DeloittePDFExtractor();
        //System.out.println(deloittePDFExtractor.extraireDonneesFacture(is));
//        System.out.println(deloittePDFExtractor2.extraireDonneesFacture(is));
        System.out.println("Facture " + deloittePDFExtractor3.extraireDonneesFacture(is));
//        System.out.println("Facture " + deloittePDFExtractor4.extraireDonneesFacture(is));

    }

    @GetMapping("/test-ocr")
    public ResponseEntity<String> testerOCR() {
        boolean ok = deloittePDFExtractor2.testerOCR();
        if (ok) {
            return ResponseEntity.ok("OCR configuré avec succès");
        } else {
            return ResponseEntity.badRequest().body("OCR non configuré");
        }
    }

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

    @DeleteMapping(path = "/{id}/saved")
    @PreAuthorize("hasRole('Admin-BK') or hasRole('Compta-BK')")
    public ResponseEntity<Map<String, Object>> deleteLoaded(@PathVariable UUID id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!factureLoadService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Facture avec l'id" + id + " non trouvé", null, HttpStatus.NOT_FOUND);
        }

        factureLoadService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Facture supprimé avec succès");
    }
}
