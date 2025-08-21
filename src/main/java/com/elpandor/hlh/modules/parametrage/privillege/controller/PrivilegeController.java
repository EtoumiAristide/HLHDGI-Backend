package com.elpandor.hlh.modules.parametrage.privillege.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegePayload;
import com.elpandor.hlh.modules.parametrage.privillege.service.PrivilegeService;
import com.elpandor.hlh.modules.parametrage.privillege.validator.PrivilegeDtoValidator;
import com.elpandor.hlh.modules.parametrage.privillege.validator.PrivilegePayloadValidator;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/privilleges")
@Tag(name = "Privilleges", description = "Ensemble des APIs de manipulation des privillèges")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PrivilegeController {
    private Logger log = LoggerFactory.getLogger(PrivilegeController.class);

    private final PrivilegeService privilegeService;

    public PrivilegeController(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        PrivilegeDto carteDto = privilegeService.get(id);
        if (carteDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, carteDto, "Privillège trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Privillège avec l'id " + id + " non trouvée");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<PrivilegeDto> cartes = privilegeService.getAll();
        if (cartes != null && !cartes.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, cartes, "Liste des privillège");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune privillège trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<PrivilegeDto> pages = privilegeService.getAllPagined(pageNum, size);
        List<PrivilegeDto> typePrivilèges = pages.getContent();
        if (!typePrivilèges.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des privillège");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun privillège trouvé", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody PrivilegeDto carteDto) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = PrivilegeDtoValidator.validate(carteDto);
            if (!errors.isEmpty()) {
                log.error("Privilège n'est pas valide {}", carteDto);
                throw new InvalidEntityException("Privilège n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            if (carteDto.getId() == 0) carteDto.setId(null);

            //Insertion de l'typePrivilège
            carteDto = privilegeService.saveOrUpdate(carteDto);

        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, carteDto, "Privilège créé avec succès");
    }

    @PostMapping("save-all")
    public ResponseEntity<Map<String, Object>> saveAll(@RequestBody List<PrivilegePayload> privilegePayloads) {
        log.trace("Starting processing Post request!");
        System.out.println("privilegePayloads " + privilegePayloads);
        try {

            //Controle de conformité
            List<String> errors = new ArrayList<>();
            privilegePayloads.forEach(privilegePayload -> {
                errors.addAll(PrivilegePayloadValidator.validate(privilegePayload));
            });
            if (!errors.isEmpty()) {
                log.error("Privilège n'est pas valide {}", privilegePayloads);
                throw new InvalidEntityException("Privilège n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            //Suppression des privillèges existants s'il y a lieu
            List<PrivilegeDto> privillegeExists = privilegeService.getAll();
            if (!privillegeExists.isEmpty()) {
                privillegeExists.forEach(privillegeDto -> privilegeService.delete(privillegeDto.getId()));
            }

            //Insertion des privilèges
            privilegePayloads.forEach(privilegePayload -> {

                privilegePayload.getSubMenus().forEach(subMenuDto -> {
                    PrivilegeDto privilege = PrivilegeDto.builder()
                            .role(privilegePayload.getRole())
                            .subMenu(subMenuDto)
                            .build();
                    privilegeService.saveOrUpdate(privilege);
                });
            });

        } catch (InvalidEntityException ex) {
            ex.printStackTrace();
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            err.printStackTrace();
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, privilegePayloads, "Privilège créé avec succès");
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody PrivilegeDto carteDto) {

        log.trace("Starting processing put for id :" + id);

        try {
            //Controle de conformité
            List<String> errors = PrivilegeDtoValidator.validate(carteDto);
            if (!errors.isEmpty()) {
                log.error("Privilège n'est pas valide {}", carteDto);
                throw new InvalidEntityException("Privilège n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'typePrivilège dans la BD
        PrivilegeDto carteDtoSearch = privilegeService.get(id);

        if (carteDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            carteDto.setId(carteDtoSearch.getId());

            try {
                carteDto = privilegeService.saveOrUpdate(carteDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return Utilities.createErrorResponse("Modification échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, carteDto, "Privilège modifiée avec succès");
        }

        log.info("Id mismatch for model (" + carteDto + ") and request param :" + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, carteDto, "Privilège avec l'id " + id + " non trouvée");
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!privilegeService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Privilège avec l'id" + id + " non trouvée", Optional.empty(), HttpStatus.NO_CONTENT);
        }

        privilegeService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Privilège supprimée avec succès");
    }
}
