package com.elpandor.hlh.modules.parametrage.menus.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;
import com.elpandor.hlh.modules.parametrage.menus.service.MenuSectionService;
import com.elpandor.hlh.modules.parametrage.menus.validator.MenuSectionDtoValidator;
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
@RequestMapping("/api/v1/menu-section")
@Tag(name = "Section Menus", description = "Ensemble des APIs de manipulation des sections menus")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MenuSectionController {
    private Logger log = LoggerFactory.getLogger(MenuSectionController.class);

    private final MenuSectionService menuSectionService;

    public MenuSectionController(MenuSectionService menuSectionService) {
        this.menuSectionService = menuSectionService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        MenuSectionDto menuSectionDto = menuSectionService.get(id);
        if (menuSectionDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, menuSectionDto, "Section Menus trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Section Menus avec l'id " + id + " non trouvée");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<MenuSectionDto> menus = menuSectionService.getAll();
        if (menus != null && !menus.isEmpty()) {
            menus.sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, menus, "Liste des Sections Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Section Menu trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<MenuSectionDto> pages = menuSectionService.getAllPagined(pageNum, size, "position");
        if (!pages.getContent().isEmpty()) {
            //pages.getContent().sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des Sections Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Sections Menu trouvée", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody MenuSectionDto menuSectionDto) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = MenuSectionDtoValidator.validate(menuSectionDto);
            if (!errors.isEmpty()) {
                log.error("Section Menus n'est pas valide {}", menuSectionDto);
                throw new InvalidEntityException("Section Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            if (menuSectionDto.getId() == 0) menuSectionDto.setId(null);
            if (menuSectionDto.getLabel().trim().isEmpty()) menuSectionDto.setLabel(null);

            //Insertion de l'typeSection Menus
            menuSectionDto = menuSectionService.saveOrUpdate(menuSectionDto);

        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, menuSectionDto, "Section Menus créé avec succès");
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody MenuSectionDto menuDto) {

        log.trace("Starting processing put for id :" + id);

        try {
            //Controle de conformité
            List<String> errors = MenuSectionDtoValidator.validate(menuDto);
            if (!errors.isEmpty()) {
                log.error("Section Menus n'est pas valide {}", menuDto);
                throw new InvalidEntityException("Section Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'typeSection Menus dans la BD
        MenuSectionDto MenuSectionDtoSearch = menuSectionService.get(id);

        if (MenuSectionDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            menuDto.setId(MenuSectionDtoSearch.getId());
            if (menuDto.getLabel().trim().isEmpty()) menuDto.setLabel(null);

            try {
                menuDto = menuSectionService.saveOrUpdate(menuDto);
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

        if (!menuSectionService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Section Menus avec l'id" + id + " non trouvée", Optional.empty(), HttpStatus.NO_CONTENT);
        }

        menuSectionService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Section Menus supprimée avec succès");
    }
}
