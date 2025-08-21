package com.elpandor.hlh.modules.parametrage.role.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;
import com.elpandor.hlh.modules.parametrage.privillege.service.PrivilegeService;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import com.elpandor.hlh.modules.parametrage.role.service.RoleService;
import com.elpandor.hlh.modules.parametrage.role.validator.RoleDtoValidator;
import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Ensemble des APIs de manipulation des roles")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RoleController {
    private Logger log = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;
    private final PrivilegeService privilegeService;
    private final KeycloakRoleService keycloakRoleService;

    public RoleController(RoleService roleService, PrivilegeService carteService, KeycloakRoleService keycloakRoleService) {
        this.roleService = roleService;
        this.privilegeService = carteService;
        this.keycloakRoleService = keycloakRoleService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        RoleDto roleDto = roleService.get(id);
        if (roleDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, roleDto, "Rôle trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Rôle avec l'id " + id + " non trouvé");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<RoleDto> roles = roleService.getAll();
        if (roles != null && !roles.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, roles, "Liste des rôles");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun rôles trouvé", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<RoleDto> pages = roleService.getAllPagined(pageNum, size);
        List<RoleDto> roles = pages.getContent();
        if (!roles.isEmpty()) {
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des rôles");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucun rôles trouvé", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody RoleDto roleDto) {
        log.trace("Starting processing Post request!");
        try {
            //Controle de conformité
            List<String> errors = RoleDtoValidator.validate(roleDto);
            if (!errors.isEmpty()) {
                log.error("Rôle n'est pas valide {}", roleDto);
                throw new InvalidEntityException("Rôle n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
            if (roleDto.getId() == 0) roleDto.setId(null);

            //Insertion dans keycloak
            Optional<CreateUserResponse> response = keycloakRoleService.create(roleDto);
            if (response.isPresent() && response.get().isStatus()) {
                //Insertion dans la BD
                roleDto = roleService.saveOrUpdate(roleDto);
            }

        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            log.error(err.getMessage(), err);
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, roleDto, "Rôle créé avec succès");
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody RoleDto roleDto) {

        log.trace("Starting processing put for id :" + id);

        try {
            //Controle de conformité
            List<String> errors = RoleDtoValidator.validate(roleDto);
            if (!errors.isEmpty()) {
                log.error("Rôle n'est pas valide {}", roleDto);
                throw new InvalidEntityException("Rôle n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'role dans la BD
        RoleDto roleDtoSearch = roleService.get(id);

        if (roleDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            roleDto.setId(roleDtoSearch.getId());

            try {
                //Suppression de l'ancien role dans keycloak
                Optional<CreateUserResponse> responseDelete = keycloakRoleService.delete(roleDtoSearch);
                if (responseDelete.isPresent() && responseDelete.get().isStatus()) {
                    //Insertion dans keycloak
                    Optional<CreateUserResponse> response = keycloakRoleService.create(roleDto);
                    if (response.isPresent() && response.get().isStatus()) {
                        //Mise à jour dans la BD
                        roleDto = roleService.saveOrUpdate(roleDto);
                    }
                }
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return Utilities.createErrorResponse("Modification échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, roleDto, "Rôle modifié avec succès");
        }

        log.info("Id mismatch for model (" + roleDto + ") and request param :" + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, roleDto, "Rôle avec l'id " + id + " non trouvé");
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!roleService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Rôle avec l'id" + id + " non trouvé", null, HttpStatus.NOT_FOUND);
        }


        //Suppression dans keycloak
        RoleDto roleDto = roleService.get(id);
        Optional<CreateUserResponse> response = keycloakRoleService.delete(roleDto);
        if (response.isPresent() && response.get().isStatus()) {

            //Suppression de la liaison avec les privilèges
            List<PrivilegeDto> privillege = privilegeService.findAllByRole(id);
            if (!privillege.isEmpty()) {
                privillege.forEach(carteDto -> privilegeService.delete(carteDto.getId()));
            }

            //Insertion dans la BD
            roleService.delete(id);
        }
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Rôle supprimé avec succès");
    }
}
