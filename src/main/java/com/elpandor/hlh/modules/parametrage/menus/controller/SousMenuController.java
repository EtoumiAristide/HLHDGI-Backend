package com.elpandor.hlh.modules.parametrage.menus.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import com.elpandor.hlh.modules.parametrage.menus.service.SousMenuService;
import com.elpandor.hlh.modules.parametrage.menus.validator.SousMenuDtoValidator;
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
@RequestMapping("/api/v1/sous-menu")
@Tag(name = "Section Menus", description = "Ensemble des APIs de manipulation des sous menus")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SousMenuController {
    private Logger log = LoggerFactory.getLogger(SousMenuController.class);

    private final SousMenuService sousMenuService;

    public SousMenuController(SousMenuService sousMenuService) {
        this.sousMenuService = sousMenuService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        SubMenuDto menuDto = sousMenuService.get(id);
        if (menuDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, menuDto, "Section Menus trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Section Menus avec l'id " + id + " non trouvée");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<SubMenuDto> menus = sousMenuService.getAll();
        if (menus != null && !menus.isEmpty()) {
            menus.sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, menus, "Liste des Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Menu trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<SubMenuDto> pages = sousMenuService.getAllPagined(pageNum, size);
        if (!pages.getContent().isEmpty()) {
            pages.getContent().sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Menu trouvée", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody SubMenuDto menuDto) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = SousMenuDtoValidator.validate(menuDto);
            if (!errors.isEmpty()) {
                log.error("Section Menus n'est pas valide {}", menuDto);
                throw new InvalidEntityException("Section Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            if (menuDto.getId() == 0) menuDto.setId(null);

            //Insertion de l'typeSection Menus
            menuDto = sousMenuService.saveOrUpdate(menuDto);

        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, menuDto, "Section Menus créé avec succès");
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody SubMenuDto menuDto) {

        log.trace("Starting processing put for id :" + id);

        try {
            //Controle de conformité
            List<String> errors = SousMenuDtoValidator.validate(menuDto);
            if (!errors.isEmpty()) {
                log.error("Section Menus n'est pas valide {}", menuDto);
                throw new InvalidEntityException("Section Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'typeSection Menus dans la BD
        SubMenuDto SubMenuDtoSearch = sousMenuService.get(id);

        if (SubMenuDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            menuDto.setId(SubMenuDtoSearch.getId());

            try {
                menuDto = sousMenuService.saveOrUpdate(menuDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return Utilities.createErrorResponse("Modification échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, menuDto, "Section Menus modifiée avec succès");
        }

        log.info("Id mismatch for model (" + menuDto + ") and request param :" + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, menuDto, "Section Menus avec l'id " + id + " non trouvée");
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!sousMenuService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Section Menus avec l'id" + id + " non trouvée", Optional.empty(), HttpStatus.NO_CONTENT);
        }

        sousMenuService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Section Menus supprimée avec succès");
    }
}
