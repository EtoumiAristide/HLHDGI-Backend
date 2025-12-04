package com.elpandor.hlh.modules.hlh.rest;

import com.elpandor.hlh.common.service.impl.FileStorageServiceImpl;
import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.dto.payload.*;
import com.elpandor.hlh.modules.hlh.service.impl.BurgerKingApimServiceImpl;
import com.elpandor.hlh.modules.hlh.service.impl.HLHApimServiceImpl;
import com.elpandor.hlh.modules.hlh.service.impl.ZinoApimServiceImpl;
import com.elpandor.hlh.modules.hlh.utils.BKExcelDataExtraction;
import com.elpandor.hlh.modules.hlh.utils.HLHExcelFactureExtractor;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.service.ApimService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import com.elpandor.hlh.modules.hlh.utils.ZinoExcelDataExtraction;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("api/v1/factures")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FactureApi {
    private Logger log = LoggerFactory.getLogger(FactureApi.class);

    private final FileStorageServiceImpl fileStorageService;
    private final FactureService factureService;
    private final ApimService hlhApimService;
    private final ApimService bkApimService;
    private final ApimService zinoApimService;
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

    public FactureApi(FileStorageServiceImpl fileStorageService, FactureService factureService, HLHApimServiceImpl hlhApimService, BurgerKingApimServiceImpl burgerKingApimService, ZinoApimServiceImpl zinoApimService, EtablissementService etablissementService, PointVenteService pointVenteService) {
        this.fileStorageService = fileStorageService;
        this.factureService = factureService;
        this.hlhApimService = hlhApimService;
        this.bkApimService = burgerKingApimService;
        this.zinoApimService = zinoApimService;
        this.etablissementService = etablissementService;
        this.pointVenteService = pointVenteService;
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
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm")) {
                log.error("Unsupported file type: {}", fileType);
                return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            //InputStream is = file.getInputStream();

//            List<MyObject> objects = ExcelParser.parseExcelFile(is);
            //ExcelParser.parseExcelFile(is);
//            ExcelFactureExtractor extractor = new ExcelFactureExtractor();
//            System.out.println("etablissement.getOrganisation() " + etablissement);
            List<FacturePayload> factures = new ArrayList<>();
//            BKExtractedData bkExtractedData = new BKExtractedData();

            if (etablissement.getOrganisation() != null) {

                switch (etablissement.getOrganisation().getRaisonSocial()) {
                    case "HOTEL AND LUXURY HOUSING":
                        factures = traitementFactureHLH(file.getInputStream(), etablissement);
                        break;
                    case "SIA RESTAURATION RAPIDE COTE D'IVOIRE":
                        factures = traitementFactureBK(file.getInputStream(), etablissement);
                        break;
                    case "ZINO COTE D'IVOIRE":
                        factures = traitementFactureZino(file.getInputStream(), etablissement);
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
            List<String> finalGroups = groups;
            factures.forEach(facturePayload -> {
                facturePayload.setTypeFacture(TypeFacture.valueOf(typeFacture));
                facturePayload.setTypeClient(TypeClient.valueOf(typeClient));
                if (etablissement.getOrganisation().getIsOrderedByPaiementMethod()) {
                    //facturePayload.setModePaiement(facturePayload.getSheetName().toLowerCase().contains("mobile money") ? ModePaiement.mobilemoney : (facturePayload.getSheetName().equalsIgnoreCase("cash") ? ModePaiement.cash : ModePaiement.card));

                    if (facturePayload.getSheetName().toLowerCase().contains("mobile money".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Wave".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Orange".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("MTN".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("MOOV".toLowerCase()))
                        facturePayload.setModePaiement(ModePaiement.mobilemoney);

                    if (facturePayload.getSheetName().toLowerCase().contains("cash".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Espèces".toLowerCase()))
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
                } else {
                    facturePayload.setModePaiement(ModePaiement.valueOf(modePaiement));
                }
                facturePayload.setEntreprise(finalGroups.get(0));
                facturePayload.setPointVente(pointVente);
            });
            //facture.setTypeFacture(TypeFacture.valueOf(typeFacture));

            Map<String, Object> result = new HashMap<>();
            result.put("factures", factures);
            if (bkExtractedData != null && bkExtractedData.getPayments() != null) {
                result.put("payments", bkExtractedData.getPayments().stream().filter(payment -> payment.getTotal() != null).toList());
            }

            /*if (extractedDatas != null && !extractedDatas.isEmpty()) {
                result.put("payments", extractedDatas);
            }*/

//            System.out.println(facture);
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
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm")) {
                log.error("Unsupported file type: {}", fileType);
                return Utilities.createErrorResponse("Format de fichier non supporté!", List.of(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            //InputStream is = file.getInputStream();

//            List<MyObject> objects = ExcelParser.parseExcelFile(is);
            //ExcelParser.parseExcelFile(is);
            //HLHExcelFactureExtractor extractor = new HLHExcelFactureExtractor();
//            System.out.println("etablissement.getOrganisation() " + etablissement);
            List<FacturePayload> factures = new ArrayList<>();
//            BKExtractedData bkExtractedData = new BKExtractedData();

            if (etablissement.getOrganisation() != null) {

                switch (etablissement.getOrganisation().getRaisonSocial()) {
                    case "HOTEL AND LUXURY HOUSING":
                        factures = traitementFactureHLH(file.getInputStream(), etablissement);
                        break;
                    case "SIA RESTAURATION RAPIDE COTE D'IVOIRE":
                        factures = traitementFactureBK(file.getInputStream(), etablissement);
                        break;
                    case "ZINO COTE D'IVOIRE":
                        factures = traitementFactureZino(file.getInputStream(), etablissement);
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
            List<String> finalGroups = groups;
            factures.forEach(facturePayload -> {
                facturePayload.setTypeFacture(TypeFacture.valueOf(typeFacture));
                facturePayload.setTypeClient(TypeClient.valueOf(typeClient));
                if (etablissement.getOrganisation().getIsOrderedByPaiementMethod()) {
                    //facturePayload.setModePaiement(facturePayload.getSheetName().toLowerCase().contains("mobile money") ? ModePaiement.mobilemoney : (facturePayload.getSheetName().equalsIgnoreCase("cash") ? ModePaiement.cash : ModePaiement.card));

                    if (facturePayload.getSheetName().toLowerCase().contains("mobile money".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Wave".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Orange".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("MTN".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("MOOV".toLowerCase()))
                        facturePayload.setModePaiement(ModePaiement.mobilemoney);

                    if (facturePayload.getSheetName().toLowerCase().contains("cash".toLowerCase())
                            || facturePayload.getSheetName().toLowerCase().contains("Espèces".toLowerCase()))
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
                } else {
                    facturePayload.setModePaiement(ModePaiement.valueOf(modePaiement));
                }
                facturePayload.setEntreprise(finalGroups.get(0));//En réalité il s'agit de l'établissement
                facturePayload.setPointVente(pointVente);
            });
            //facture.setTypeFacture(TypeFacture.valueOf(typeFacture));

//            System.out.println(factures);
            //Sauvegarde du fichier
            //String storeName = fileStorageService.storeFile(file, "Facture-" + new SimpleDateFormat("yyyyMMdddHHmmss").format(new Date()));

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
                if (etablissement.getOrganisation().getRaisonSocial().equalsIgnoreCase(entrepriseZino)) {
                    tokenResponse = zinoApimService.auth();
                    response = zinoApimService.sendData(tokenResponse.getAccessToken(), facture);
                }
                PointVenteDto pointVenteDto = pointVenteService.findByNom(pointVente);
                assert response != null;

                //Gestion de la date de facture
                LocalDate dateFacture = null;
                try {
                    dateFacture = facture.getDateFacture() != null ? LocalDate.parse(facture.getDateFacture(), DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now();
                } catch (Exception ex) {
                    dateFacture = LocalDate.now();
                    ex.printStackTrace();
                }

                if (response.getStatusCode().is2xxSuccessful()) {
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

    private List<FacturePayload> traitementFactureHLH(InputStream is, EtablissementDto etablissement) throws IOException {
        return new HLHExcelFactureExtractor().extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier());
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
        AtomicReference<Integer> nbCash = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> (payment.getPaymentType() == Payment.PaymentType.CASH || payment.getPaymentType() == Payment.PaymentType.HD_GLOVO) && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalCash.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        totalCash.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                        nbCash.updateAndGet(v -> (v + 1));
                    }
                });
        LigneProduitPayload ligneProduitCash = new LigneProduitPayload();
        ligneProduitCash.setProduit("Ventes en espèce");
        ligneProduitCash.setMontantHT(totalCash.get());
        ligneProduitCash.setQuantite(nbCash.get());
        ligneProduitsCash.add(ligneProduitCash);

        ClientPayload clientCash = new ClientPayload();
        clientCash.setNom("CASH");
        clientCash.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadCash = new TotauxPayload();
        totauxPayloadCash.setHt(totalCash.get());

        TaxePayload tdtCash = new TaxePayload();
        tdtCash.setBase(totalCash.get());
        tdtCash.setTaux(1.5);
        tdtCash.setMontant(totalCash.get() * 0.015);//1.5%
        totauxPayloadCash.setTdt(tdtCash);

        TaxePayload tvaCash = new TaxePayload();
        tvaCash.setTaux(18.0);
        tvaCash.setBase(totalCash.get() + tdtCash.getMontant());
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
        AtomicReference<Integer> nbWave = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> payment.getPaymentType() == Payment.PaymentType.CASH_WAVE && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalWave.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        totalWave.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                        nbWave.updateAndGet(v -> (v + 1));
                    }
                });
        LigneProduitPayload ligneProduitWave = new LigneProduitPayload();
        ligneProduitWave.setProduit("Ventes via Wave");
        ligneProduitWave.setMontantHT(totalWave.get());
        ligneProduitWave.setQuantite(nbWave.get());
        ligneProduitsWave.add(ligneProduitWave);

        ClientPayload clientWave = new ClientPayload();
        clientWave.setNom("WAVE");
        clientWave.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadWave = new TotauxPayload();
        totauxPayloadWave.setHt(totalWave.get());

        TaxePayload tdtWave = new TaxePayload();
        tdtWave.setBase(totalWave.get());
        tdtWave.setTaux(1.5);
        tdtWave.setMontant(totalWave.get() * 0.015);//1.5%
        totauxPayloadWave.setTdt(tdtWave);

        TaxePayload tvaWave = new TaxePayload();
        tvaWave.setTaux(18.0);
        tvaWave.setBase(totalWave.get() + tdtWave.getMontant());
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
        AtomicReference<Integer> nbCC = new AtomicReference<>((int) 0);
        bkExtractedData.getPayments()
                .stream()
                .filter(payment -> payment.getPaymentType() == Payment.PaymentType.BACKUP_CC && payment.getTotal() != null)
                .forEach(payment -> {
                    if (!payment.getCheckNumber().toLowerCase().contains("total")) {
//                                    totalCC.updateAndGet(v -> (v + payment.getTotal().doubleValue()));
                        totalCC.updateAndGet(v -> (v + payment.getAmount().doubleValue()));
                        nbCC.updateAndGet(v -> (v + 1));
                    }
                });
        LigneProduitPayload ligneProduitCC = new LigneProduitPayload();
        ligneProduitCC.setProduit("Ventes via cartes bancaires");
        ligneProduitCC.setMontantHT(totalCC.get());
        ligneProduitCC.setQuantite(nbCC.get());
        ligneProduitsCC.add(ligneProduitCC);

        ClientPayload clientCC = new ClientPayload();
        clientCC.setNom("CC");
        clientCC.setNumeroCC("");

        //Montant & taxes
        TotauxPayload totauxPayloadCC = new TotauxPayload();
        totauxPayloadCC.setHt(totalCC.get());

        TaxePayload tdtCC = new TaxePayload();
        tdtCC.setBase(totalCC.get());
        tdtCC.setTaux(1.5);
        tdtCC.setMontant(totalCC.get() * 0.015);//1.5%
        totauxPayloadCC.setTdt(tdtCC);

        TaxePayload tvaCC = new TaxePayload();
        tvaCC.setTaux(18.0);
        tvaCC.setBase(totalCC.get() + tdtCC.getMontant());
        tvaCC.setMontant(tvaCC.getBase() * 0.18);
        totauxPayloadCC.setTva(tvaCC);

        totauxPayloadCC.setTtc(totauxPayloadCC.getHt() + tdtCC.getMontant() + tvaCC.getMontant());
        totauxPayloadCC.setModePaiement(ModePaiement.card.toString());

        factureCC.setSheetName("CC");
        factureCC.setLignes(ligneProduitsCC);
        factureCC.setClientPayload(clientCC);
        factureCC.setTotauxPayload(totauxPayloadCC);

        //Mise à jour de la facture générale
//        factures.add(factureCash);
//        factures.add(factureWave);
//        factures.add(factureCC);
        return List.of(factureCash, factureWave, factureCC);
    }

    private List<FacturePayload> traitementFactureZino(InputStream is, EtablissementDto etablissement) throws IOException {
        List<FacturePayload> factures = new ArrayList<>();

        //System.out.println(new ZinoExcelDataExtraction().extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier()));
        ZinoExcelDataExtraction zinoExcelDataExtraction = new ZinoExcelDataExtraction();

        //Extraction des données brutes
        extractedDatas = zinoExcelDataExtraction.extractFacture(is, etablissement.getOrganisation().getIndexLectureFichier());

        //Trie pas mode de paiement
        List<ZinoExtractedDataOrdered> extractedDataOrdereds = zinoExcelDataExtraction.calculerRepartitionParModePaiement(extractedDatas);

        //Constitution de la liste facture
        extractedDataOrdereds.forEach(zinoExtractedDataOrdered -> {
            FacturePayload facture = new FacturePayload();
            List<LigneProduitPayload> ligneProduits = new ArrayList<>();

            LigneProduitPayload ligneProduit = new LigneProduitPayload();
            ligneProduit.setProduit("Ventes via " + zinoExtractedDataOrdered.getModePaiement());
            ligneProduit.setPrixUnitaireHT(zinoExtractedDataOrdered.getTotalMontantHT());
            ligneProduit.setMontantHT(zinoExtractedDataOrdered.getTotalMontantHT());
            ligneProduit.setQuantite(zinoExtractedDataOrdered.getNombreTransactions());
            ligneProduit.setDate(new SimpleDateFormat("dd/MM/yyyy").format(zinoExtractedDataOrdered.getDate()));
            ligneProduits.add(ligneProduit);

            ClientPayload client = new ClientPayload();
            client.setNom(zinoExtractedDataOrdered.getModePaiement());
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

            facture.setDateFacture(new SimpleDateFormat("dd/MM/yyyy").format(zinoExtractedDataOrdered.getDate()));
            facture.setSheetName(zinoExtractedDataOrdered.getModePaiement());
            facture.setLignes(ligneProduits);
            facture.setClientPayload(client);
            facture.setTotauxPayload(totauxPayload);

            factures.add(facture);
        });

        return factures;

    }

    //    @GetMapping("export")
//    public ResponseEntity<Map<String, Object>> export(){
//        log.trace("Starting  processing of delete request for id :" + id);
//    }

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
