package com.elpandor.hlh.modules.parametrage.compteutilisateur.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.service.UtiliityService;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.service.CompteUtilisateurService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.validator.CompteUtilisateurDtoValidator;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto.RoleUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.service.RoleUtilisateurService;
import com.elpandor.hlh.modules.parametrage.users.dto.CreateUserResponse;
import com.elpandor.hlh.modules.parametrage.users.dto.UserDto;
import com.elpandor.hlh.modules.parametrage.users.service.KeycloakUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/compte-utilisateurs")
@Tag(name = "CompteUtilisateurs", description = "Ensemble des APIs de manipulation des compteUtilisateurs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CompteUtilisateurController {
    private Logger log = LoggerFactory.getLogger(CompteUtilisateurController.class);

    private final CompteUtilisateurService compteUtilisateurService;
    private final KeycloakUserService keycloakUserService;
    private final RoleUtilisateurService roleUtilisateurService;
    private final UtiliityService utiliityService;

    @Value("${app.environnement}")
    private String environnement;

    public CompteUtilisateurController(CompteUtilisateurService compteUtilisateurService, KeycloakUserService keycloakUserService, RoleUtilisateurService roleUtilisateurService, UtiliityService utiliityService) {
        this.compteUtilisateurService = compteUtilisateurService;
        this.keycloakUserService = keycloakUserService;
        this.roleUtilisateurService = roleUtilisateurService;
        this.utiliityService = utiliityService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        CompteUtilisateurDto compteUtilisateurDto = compteUtilisateurService.get(id);
        if (compteUtilisateurDto != null) {

            //Recupération des rôles
            List<RoleUtilisateurDto> roleUtilisateurs = roleUtilisateurService.findAllByUtilisateur(compteUtilisateurDto.getId());
            compteUtilisateurDto.setRoles(new ArrayList<>());
            roleUtilisateurs.forEach(roleUtilisateurDto -> {
                compteUtilisateurDto.getRoles().add(roleUtilisateurDto.getRole());
            });

            return Utilities.createSuccessResponse(HttpStatus.OK, compteUtilisateurDto, "CompteUtilisateur trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "CompteUtilisateur avec l'id " + id + " non trouvée");
    }

    @GetMapping(path = "/userkeycloakid/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        log.trace("Starting processing get request for id :" + id);

        CompteUtilisateurDto compteUtilisateurDto = compteUtilisateurService.findByUtilisateurId(id);
        if (compteUtilisateurDto != null) {

            //Recupération des rôles
            List<RoleUtilisateurDto> roleUtilisateurs = roleUtilisateurService.findAllByUtilisateur(compteUtilisateurDto.getId());
            compteUtilisateurDto.setRoles(new ArrayList<>());
            roleUtilisateurs.forEach(roleUtilisateurDto -> {
                compteUtilisateurDto.getRoles().add(roleUtilisateurDto.getRole());
            });

            return Utilities.createSuccessResponse(HttpStatus.OK, compteUtilisateurDto, "CompteUtilisateur trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "CompteUtilisateur avec l'id " + id + " non trouvée");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<CompteUtilisateurDto> compteUtilisateurs = compteUtilisateurService.getAll();
        if (compteUtilisateurs != null && !compteUtilisateurs.isEmpty()) {
            compteUtilisateurs.forEach(compteUtilisateurDto -> {

                //Recupération des rôles
                List<RoleUtilisateurDto> roleUtilisateurs = roleUtilisateurService.findAllByUtilisateur(compteUtilisateurDto.getId());
                compteUtilisateurDto.setRoles(new ArrayList<>());
                roleUtilisateurs.forEach(roleUtilisateurDto -> {
                    compteUtilisateurDto.getRoles().add(roleUtilisateurDto.getRole());
                });
            });
            return Utilities.createSuccessResponse(HttpStatus.OK, compteUtilisateurs, "Liste des compteUtilisateur");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune compteUtilisateur trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<CompteUtilisateurDto> pages = compteUtilisateurService.getAllPagined(pageNum, size);
        List<CompteUtilisateurDto> compteUtilisateurs = pages.getContent();
        if (!compteUtilisateurs.isEmpty()) {
            pages.getContent().forEach(compteUtilisateurDto -> {

                //Recupération des rôles
                List<RoleUtilisateurDto> roleUtilisateurs = roleUtilisateurService.findAllByUtilisateur(compteUtilisateurDto.getId());
                compteUtilisateurDto.setRoles(new ArrayList<>());
                roleUtilisateurs.forEach(roleUtilisateurDto -> {
                    compteUtilisateurDto.getRoles().add(roleUtilisateurDto.getRole());
                });
            });
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des compteUtilisateur");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune compteUtilisateur trouvée", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody CompteUtilisateurDto compteUtilisateurDto) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = CompteUtilisateurDtoValidator.validate(compteUtilisateurDto, true);
            if (!errors.isEmpty()) {
                log.error("CompteUtilisateur n'est pas valide {}", compteUtilisateurDto);
                throw new InvalidEntityException("CompteUtilisateur n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            if (compteUtilisateurDto.getId() == 0) compteUtilisateurDto.setId(null);

            //Recuperation de la personne physique/morale liée
            UserDto userDto = new UserDto();
            userDto.setFirstName(compteUtilisateurDto.getFirstname());
            userDto.setLastName(compteUtilisateurDto.getLastname());
            userDto.setUsername(compteUtilisateurDto.getLogin());
            userDto.setEmail(compteUtilisateurDto.getEmail());
            userDto.setPhone(compteUtilisateurDto.getPhone());
            userDto.setPassword(compteUtilisateurDto.getPassword());
            userDto.setRoles(new ArrayList<>());
            userDto.setEnable(compteUtilisateurDto.isEnable());
            compteUtilisateurDto.getRoles().forEach(roleDto -> {
                userDto.getRoles().add(roleDto.getLibelle());
            });

            //Ajout de l'utilisateur dans keycloak
            Optional<CreateUserResponse> userResponse = keycloakUserService.create(userDto);
            if (userResponse.isPresent() && userResponse.get().isStatus()) {
                compteUtilisateurDto.setKeycloakUserId(String.valueOf(userResponse.get().getData()));

                //Insertion de CompteUtilisateur dans la BD
                CompteUtilisateurDto compteUtilisateurDtoCreate = compteUtilisateurService.saveOrUpdate(compteUtilisateurDto);

                //AJout des rôles dans la BD
                compteUtilisateurDto.getRoles().forEach(roleDto -> {
                    RoleUtilisateurDto roleUtilisateurDto = RoleUtilisateurDto.builder()
                            .compteUtilisateur(compteUtilisateurDtoCreate)
                            .role(roleDto)
                            .date(LocalDate.now())
                            .build();

                    roleUtilisateurService.saveOrUpdate(roleUtilisateurDto);
                });

                //Envoi du mail à l'utilisateur
                if (environnement.equalsIgnoreCase("production") || environnement.equalsIgnoreCase("recette")) {
                    utiliityService.sendMailToNewUser(compteUtilisateurDto);
                }
            } else {
                return Utilities.createErrorResponse("Création échouée", userResponse.get().getData(), HttpStatus.BAD_REQUEST);
            }

            return Utilities.createSuccessResponse(HttpStatus.CREATED, compteUtilisateurDto, "CompteUtilisateur créé avec succès");

        } catch (InvalidEntityException ex) {
            ex.printStackTrace();
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            err.printStackTrace();
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody CompteUtilisateurDto compteUtilisateurDto) {

        log.trace("Starting processing put for id :" + id);
        System.out.println(compteUtilisateurDto);
        try {
            //Controle de conformité
            List<String> errors = CompteUtilisateurDtoValidator.validate(compteUtilisateurDto, false);
            if (!errors.isEmpty()) {
                log.error("CompteUtilisateur n'est pas valide {}", compteUtilisateurDto);
                throw new InvalidEntityException("CompteUtilisateur n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'typeCompteUtilisateur dans la BD
        CompteUtilisateurDto compteUtilisateurDtoSearch = compteUtilisateurService.get(id);

        if (compteUtilisateurDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            compteUtilisateurDto.setId(compteUtilisateurDtoSearch.getId());

            try {

                //Recuperation de la personne physique/morale liée
                UserDto userDto = new UserDto();

                userDto.setFirstName(compteUtilisateurDto.getFirstname());
                userDto.setLastName(compteUtilisateurDto.getLastname());
                userDto.setUsername(compteUtilisateurDto.getLogin());
                userDto.setEmail(compteUtilisateurDto.getEmail());
                userDto.setPhone(compteUtilisateurDto.getPhone());
                userDto.setPassword(compteUtilisateurDto.getPassword());
                userDto.setRoles(new ArrayList<>());
                userDto.setOldRoles(new ArrayList<>());
                userDto.setEnable(compteUtilisateurDto.isEnable());
                compteUtilisateurDto.getRoles().forEach(roleDto -> {
                    userDto.getRoles().add(roleDto.getLibelle());
                });

                //Recuperation des anciens roles de l'user
                List<RoleUtilisateurDto> roleUtilisateurDtos = roleUtilisateurService.findAllByUtilisateur(compteUtilisateurDto.getId());
                roleUtilisateurDtos.forEach(roleUtilisateurDto -> {
                    userDto.getOldRoles().add(roleUtilisateurDto.getRole().getLibelle());
                });

                //Mis à jour de l'utilisateur et ses roles dans keycloak
                Optional<CreateUserResponse> userResponse = keycloakUserService.update(compteUtilisateurDto.getKeycloakUserId(), userDto);
                if (userResponse.isPresent() && userResponse.get().isStatus()) {

                    //Suppression des roles existant dans la BD
                    roleUtilisateurDtos.forEach(roleUtilisateurDto -> {
                        roleUtilisateurService.delete(roleUtilisateurDto.getId());
                    });

                    //Mise à jour del'user dans la BD
                    CompteUtilisateurDto compteUtilisateurDtoCreate = compteUtilisateurService.saveOrUpdate(compteUtilisateurDto);

                    //AJout des rôles dans la BD
                    compteUtilisateurDto.getRoles().forEach(roleDto -> {
                        RoleUtilisateurDto roleUtilisateurDto = RoleUtilisateurDto.builder()
                                .compteUtilisateur(compteUtilisateurDtoCreate)
                                .role(roleDto)
                                .date(LocalDate.now())
                                .build();

                        roleUtilisateurService.saveOrUpdate(roleUtilisateurDto);
                    });

                } else {
                    return Utilities.createErrorResponse("Modification échouée", userResponse.get().getData(), HttpStatus.BAD_REQUEST);
                }

                compteUtilisateurDto = compteUtilisateurService.saveOrUpdate(compteUtilisateurDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return Utilities.createErrorResponse("Modification échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, compteUtilisateurDto, "CompteUtilisateur modifiée avec succès");
        }

        log.info("Id mismatch for model (" + compteUtilisateurDto + ") and request param :" + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, compteUtilisateurDto, "CompteUtilisateur avec l'id " + id + " non trouvée");
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!compteUtilisateurService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("CompteUtilisateur avec l'id" + id + " non trouvée", Optional.empty(), HttpStatus.NO_CONTENT);
        }

        compteUtilisateurService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "CompteUtilisateur supprimée avec succès");
    }

    @GetMapping("/{userId}/{password}")
    public ResponseEntity<CreateUserResponse> reinitPassword(@PathVariable("password") String password, @PathVariable("userId") String userId) {
        CreateUserResponse response = new CreateUserResponse();
        response = keycloakUserService.reinitPassword(userId, password)
                .orElseThrow(() -> new RuntimeException("Impossible de réinitialiser le mot de passe de l'utilisateur"));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
