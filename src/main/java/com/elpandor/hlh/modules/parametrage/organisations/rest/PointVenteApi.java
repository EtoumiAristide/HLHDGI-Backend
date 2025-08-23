package com.elpandor.hlh.modules.parametrage.organisations.rest;

import com.elpandor.hlh.common.service.FileStorageService;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteEntreprise;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/point-vente")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PointVenteApi {
    private Logger log = LoggerFactory.getLogger(PointVenteApi.class);

    private final PointVenteService pointVenteService;

    private final FileStorageService fileStorageService;

    //@Value("${upload_dir}")
    private String uploadsDir = "uploads";

    private Path root;

    @Autowired
    public PointVenteApi(PointVenteService pointVenteService, FileStorageService fileStorageService) {
        this.pointVenteService = pointVenteService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PointVenteDto> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        PointVenteDto pointVenteDto = pointVenteService.get(id);
        if (pointVenteDto != null) {
            return new ResponseEntity<>(pointVenteDto, HttpStatus.OK);
        }

        log.info("Entity having id not found, id : " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        List<PointVenteDto> pointVentes = pointVenteService.getAll();
        if (pointVentes != null && !pointVentes.isEmpty()) {
//            return new ResponseEntity<>(pointVentes, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, pointVentes, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<PointVenteDto> pages = pointVenteService.getAllPagined(pageNum, size);
        List<PointVenteDto> pointVenteDtos = pages.getContent();
        if (!pointVenteDtos.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des type carte");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun type carte trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping("sortbyEntreprise")
    public ResponseEntity<Map<String, Object>> getAllByEntreprise() {
        log.trace("Starting processing getAll request!");

        List<PointVenteDto> pointVentes = pointVenteService.getAll();
        List<PointVenteEntreprise> pointVenteEntreprises = new ArrayList<>();
        if (pointVentes != null && !pointVentes.isEmpty()) {

            initDataForGrid(pointVentes, pointVenteEntreprises);

//            return new ResponseEntity<>(pointVenteEntreprises, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, pointVenteEntreprises, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }

    private void initDataForGrid(List<PointVenteDto> pointVentes, List<PointVenteEntreprise> pointVenteEntreprises) {
        //Ordonnancement de la liste par ID
        pointVentes.sort(Comparator.comparingInt(PointVenteDto::getId));

        //Regroupement par entreprise
        List<OrganisationDto> organisations = new ArrayList<>();

        pointVentes.forEach(pointVenteDto -> {
            if (organisations.stream().noneMatch(organisationDto -> organisationDto.getId().equals(pointVenteDto.getOrganisation().getId()))) {
                organisations.add(pointVenteDto.getOrganisation());
            }
        });

        organisations.sort(Comparator.comparingInt(OrganisationDto::getId));
        organisations.forEach(organisationDto -> {
            PointVenteEntreprise pointVenteEntreprise = new PointVenteEntreprise();
            pointVenteEntreprise.setOrganisation(organisationDto);
            pointVenteEntreprise.setPointVentes(new ArrayList<>());

            pointVentes.forEach(pointVenteDto -> {
                if (pointVenteDto.getOrganisation().getId().equals(organisationDto.getId())) {
                    pointVenteEntreprise.getPointVentes().add(pointVenteDto);
                }
            });

            pointVenteEntreprises.add(pointVenteEntreprise);
        });
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<Map<String, Object>> getByOrganisation(@PathVariable("organisationId") Integer organisationId) {
        log.trace("Starting processing getAll request!");
        List<PointVenteDto> pointVenteDtoList = pointVenteService.getAllByOrganosation(organisationId);

        if (pointVenteDtoList != null) {
//            return new ResponseEntity<>(pointVenteDtoList, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, pointVenteDtoList, "Liste des points de vente");
        }

        log.info("No element found while hitting getAll");
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        return Utilities.createErrorResponse("Aucune donnée trouvé", List.of(), HttpStatus.NO_CONTENT);
    }


    @PostMapping()
    @PreAuthorize("hasRole('Super-Admin')")
    public ResponseEntity<Map<String, Object>> save(@RequestBody PointVenteDto pointVenteDto) {
        log.trace("Starting processing Post request!");
        try {
            //Insertion de l'pointVente
            pointVenteDto.setId(null);
            pointVenteDto = pointVenteService.saveOrUpdate(pointVenteDto);

        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            return Utilities.createErrorResponse("Une erreur est survenue", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
//        return new ResponseEntity<>(pointVenteDto, HttpStatus.CREATED);
        return Utilities.createSuccessResponse(HttpStatus.OK, pointVenteDto, "Point de vente créé");
    }

    @PutMapping(path = "/{id}")
    @PreAuthorize("hasRole('Super-Admin')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody PointVenteDto pointVenteDto) {

        log.trace("Starting processing put for id :" + id);
        //Recherche de l'pointVente dans la BD
        PointVenteDto pointVenteDtoSearch = pointVenteService.get(id);

        if (pointVenteDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            pointVenteDto.setId(pointVenteDtoSearch.getId());

            try {

                pointVenteDto = pointVenteService.saveOrUpdate(pointVenteDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
//                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                return Utilities.createErrorResponse("Une erreur est survenue", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
//            return new ResponseEntity<>(pointVenteDto, HttpStatus.OK);
            return Utilities.createSuccessResponse(HttpStatus.OK, pointVenteDto, "Point de vente mis à jour");
        }

        log.info("Id mismatch for model (" + pointVenteDto + ") and request param :" + id);
//        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        return Utilities.createErrorResponse("Aucune modification éffectué", List.of(), HttpStatus.NOT_MODIFIED);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete rrequest for id :" + id);

        if (!pointVenteService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return Utilities.createErrorResponse("Point de vente avec l'id " + id + " n'est pas disponible", List.of(), HttpStatus.NOT_FOUND);
        }

        pointVenteService.delete(id);
        log.info("Entity deleted having id :" + id);
//        return new ResponseEntity<>(HttpStatus.OK);
        return Utilities.createSuccessResponse(HttpStatus.NO_CONTENT, List.of(), "Point de vente supprimé");
    }
}
