package com.elpandor.hlh.modules.parametrage.organisations.rest;

import com.elpandor.hlh.common.service.FileStorageService;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementEntreprise;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.OrganisationService;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("api/v1/etablissement")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EtablissementApi {
    private Logger log = LoggerFactory.getLogger(EtablissementApi.class);

    private final EtablissementService etablissementService;
    private final OrganisationService organisationService;

    private final FileStorageService fileStorageService;

    //@Value("${upload_dir}")
    private String uploadsDir = "uploads";

    private Path root;

    @Autowired
    public EtablissementApi(EtablissementService etablissementService, OrganisationService organisationService, FileStorageService fileStorageService) {
        this.etablissementService = etablissementService;
        this.organisationService = organisationService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<EtablissementDto> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        EtablissementDto etablissementDto = etablissementService.get(id);
        if (etablissementDto != null) {
            return new ResponseEntity<>(etablissementDto, HttpStatus.OK);
        }

        log.info("Entity having id not found, id : " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        List<EtablissementDto> etablissements = etablissementService.getAll();
        if (etablissements != null && !etablissements.isEmpty()) {
//            return new ResponseEntity<>(etablissements, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissements, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<EtablissementDto> pages = etablissementService.getAllPagined(pageNum, size);
        List<EtablissementDto> etablissementDtos = pages.getContent();
        if (!etablissementDtos.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des type carte");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun type carte trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping("byEntreprise")
    public ResponseEntity<Map<String, Object>> getAllByEntreprise(@AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing getAll request!");

        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        String entreprise = "";
        OrganisationDto organisationDto = null;
        if (groups != null) {
            entreprise = groups.get(0);
            organisationDto = organisationService.findByRaisonSocial(entreprise);
        }

        List<EtablissementDto> etablissements = organisationDto != null ? etablissementService.getAllByOrganosation(organisationDto.getId()) : List.of();
        if (!etablissements.isEmpty()) {
//            return new ResponseEntity<>(etablissementEntreprises, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissements, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }

    @GetMapping("bykeycloakgroup")
    public ResponseEntity<Map<String, Object>> getAllByKeycloakGroup(@AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing getAll request!");

        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        if (groups == null) groups = List.of();
        if (groups.isEmpty())
            return Utilities.createErrorResponse("Etablissement agent inconnue", List.of(), HttpStatus.BAD_REQUEST);

        //Recuperation de l'établissement
        EtablissementDto etablissement = etablissementService.findByNom(groups.get(0));

        if (etablissement != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissement, "Etablissement inexistant ");
        } else {
            log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
        }
    }

    @GetMapping("sortbyEntreprise")
    public ResponseEntity<Map<String, Object>> getAllByEntreprise() {
        log.trace("Starting processing getAll request!");

        List<EtablissementDto> etablissements = etablissementService.getAll();
        List<EtablissementEntreprise> etablissementEntreprises = new ArrayList<>();
        if (etablissements != null && !etablissements.isEmpty()) {

            initDataForGrid(etablissements, etablissementEntreprises);

//            return new ResponseEntity<>(etablissementEntreprises, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissementEntreprises, "Liste des établissement");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }


    private void initDataForGrid(List<EtablissementDto> etablissements, List<EtablissementEntreprise> etablissementEntreprises) {
        //Ordonnancement de la liste par ID
        etablissements.sort(Comparator.comparingInt(EtablissementDto::getId));

        //Regroupement par entreprise
        List<OrganisationDto> organisations = new ArrayList<>();

        etablissements.forEach(etablissementDto -> {
            if (organisations.stream().noneMatch(organisationDto -> organisationDto.getId().equals(etablissementDto.getOrganisation().getId()))) {
                organisations.add(etablissementDto.getOrganisation());
            }
        });

        organisations.sort(Comparator.comparingInt(OrganisationDto::getId));
        organisations.forEach(organisationDto -> {
            EtablissementEntreprise etablissementEntreprise = new EtablissementEntreprise();
            etablissementEntreprise.setOrganisation(organisationDto);
            etablissementEntreprise.setEtablissements(new ArrayList<>());

            etablissements.forEach(etablissementDto -> {
                if (etablissementDto.getOrganisation().getId().equals(organisationDto.getId())) {
                    etablissementEntreprise.getEtablissements().add(etablissementDto);
                }
            });

            etablissementEntreprises.add(etablissementEntreprise);
        });
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<Map<String, Object>> getByOrganisation(@PathVariable("organisationId") Integer organisationId) {
        log.trace("Starting processing getAll request!");
        List<EtablissementDto> etablissementDtoList = etablissementService.getAllByOrganosation(organisationId);

        if (etablissementDtoList != null) {
//            return new ResponseEntity<>(etablissementDtoList, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissementDtoList, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }


    @PostMapping()
    @PreAuthorize("hasRole('Super-Admin')")
    public ResponseEntity<Map<String, Object>> save(@RequestBody EtablissementDto etablissementDto) {
        log.trace("Starting processing Post request!");
        try {
            //Insertion de l'etablissement
            etablissementDto.setId(null);
            etablissementDto = etablissementService.saveOrUpdate(etablissementDto);

        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            return Utilities.createErrorResponse("Une erreur est survenue", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
//        return new ResponseEntity<>(etablissementDto, HttpStatus.CREATED);
        return Utilities.createSuccessResponse(HttpStatus.OK, etablissementDto, "Point de vente créé");
    }

    @PutMapping(path = "/{id}")
    @PreAuthorize("hasRole('Super-Admin')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody EtablissementDto etablissementDto) {

        log.trace("Starting processing put for id :" + id);
        //Recherche de l'etablissement dans la BD
        EtablissementDto etablissementDtoSearch = etablissementService.get(id);

        if (etablissementDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            etablissementDto.setId(etablissementDtoSearch.getId());

            try {

                etablissementDto = etablissementService.saveOrUpdate(etablissementDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
//                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                return Utilities.createErrorResponse("Une erreur est survenue", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
//            return new ResponseEntity<>(etablissementDto, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, etablissementDto, "Point de vente mis à jour");
        }

        log.info("Id mismatch for model (" + etablissementDto + ") and request param :" + id);
//        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        return Utilities.createErrorResponse("Aucune modification éffectué", List.of(), HttpStatus.NOT_MODIFIED);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete rrequest for id :" + id);

        if (!etablissementService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return Utilities.createErrorResponse("Point de vente avec l'id " + id + " n'est pas disponible", List.of(), HttpStatus.NOT_FOUND);
        }

        etablissementService.delete(id);
        log.info("Entity deleted having id :" + id);
//        return new ResponseEntity<>(HttpStatus.OK);
        return Utilities.createSuccessResponse(HttpStatus.NO_CONTENT, List.of(), "Point de vente supprimé");
    }
}
