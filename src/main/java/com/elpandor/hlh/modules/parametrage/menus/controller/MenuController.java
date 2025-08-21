package com.elpandor.hlh.modules.parametrage.menus.controller;

import com.elpandor.hlh.common.core.exceptions.ErrorCodes;
import com.elpandor.hlh.common.core.exceptions.InvalidEntityException;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuDto;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.MenuSectionDto;
import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import com.elpandor.hlh.modules.parametrage.menus.service.MenuSectionService;
import com.elpandor.hlh.modules.parametrage.menus.service.MenuService;
import com.elpandor.hlh.modules.parametrage.menus.service.SousMenuService;
import com.elpandor.hlh.modules.parametrage.menus.validator.MenuDtoValidator;
import com.elpandor.hlh.modules.parametrage.menus.validator.MenuSectionDtoValidator;
import com.elpandor.hlh.modules.parametrage.menus.validator.SousMenuDtoValidator;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;
import com.elpandor.hlh.modules.parametrage.privillege.service.PrivilegeService;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/menus")
@Tag(name = "Menus", description = "Ensemble des APIs de manipulation des menus")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MenuController {
    private Logger log = LoggerFactory.getLogger(MenuController.class);

    private final MenuService menuService;
    private final MenuSectionService menuSectionService;
    private final SousMenuService sousMenuService;
    private final PrivilegeService privilegeService;

    public MenuController(MenuService menuService, MenuSectionService menuSectionService, SousMenuService sousMenuService, PrivilegeService privilegeService) {
        this.menuService = menuService;
        this.menuSectionService = menuSectionService;
        this.sousMenuService = sousMenuService;
        this.privilegeService = privilegeService;
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        MenuDto MenuDto = menuService.get(id);
        if (MenuDto != null) {
            return Utilities.createSuccessResponse(HttpStatus.OK, MenuDto, "Menus trouvé");
        }

        log.info("Entity having id not found, id : " + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, Optional.empty(), "Menus avec l'id " + id + " non trouvée");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        log.trace("Starting processing getAll request!");

        List<MenuDto> menus = menuService.getAll();
        if (menus != null && !menus.isEmpty()) {
            menus.sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, menus, "Liste des Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Menu trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("/bysection/{id}")
    public ResponseEntity<Map<String, Object>> getAllBySection(@PathVariable("id") Integer sectionId) {
        log.trace("Starting processing getAll getAllBySection!");

        List<MenuDto> menus = menuService.findAllByMenuSection(sectionId);
        if (menus != null && !menus.isEmpty()) {
            menus.sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, menus, "Liste des Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Menu trouvée", List.of(), HttpStatus.OK);
    }

    @GetMapping("menu-section")
    public ResponseEntity<Map<String, Object>> getAllMenuSection() {
        log.trace("Starting processing getAllMenuSection!");

        List<MenuSectionDto> menuSections = menuSectionService.getAll();

        if (menuSections != null && !menuSections.isEmpty()) {
            //Ordonnancement des sections par position
            menuSections.sort((menuSection1, menuSection2) -> menuSection1.getPosition().compareTo(menuSection2.getPosition()));

            menuSections.forEach(menuSectionDto -> {
                List<MenuDto> menus = menuService.findAllByMenuSection(menuSectionDto.getId());

                if (menus != null && !menus.isEmpty()) {

                    menus.forEach(menuDto -> {
                        List<SubMenuDto> subMenus = sousMenuService.findAllByMenu(menuDto.getId());

                        if (subMenus != null && !subMenus.isEmpty()) {

                            menuDto.setSubMenus(subMenus);
                        }
                    });

                    menuSectionDto.setMenu(menus);
                }
            });
            return Utilities.createSuccessResponse(HttpStatus.OK, menuSections, "Liste des MenuSection");
        }

        log.info("No element found while hitting getAllMenuSection");
        return Utilities.createErrorResponse("Aucune MenuSection trouvée", List.of(), HttpStatus.OK);
    }

    @PostMapping("menu-section-byroles")
    public ResponseEntity<Map<String, Object>> getAllMenuSectionByRoles(@RequestBody List<RoleDto> roles) {
        log.trace("Starting processing getAllMenuSectionByRoles!");

        List<PrivilegeDto> privileges = new ArrayList<>();
        List<MenuSectionDto> menuSections = new ArrayList<>();
        if (roles != null && !roles.isEmpty()) {
            roles.forEach(roleDto -> {
                privileges.addAll(privilegeService.findAllByRole(roleDto.getId()));
            });

//            System.out.println("privileges avant tri: " + privileges);
            //Trie de la liste par ordre croissant des id privilege
            privileges.sort((privilege1, privilege2) -> privilege1.getSubMenu().getId().compareTo(privilege2.getSubMenu().getId()));
//            System.out.println("privileges apres tri: " + privileges);
            //Suppression des doublons
            List<PrivilegeDto> privilegesTries = privileges.stream().distinct().toList();

            //Constitution des données

            //Ajout de la section du dashboard
            MenuSectionDto menuSectionDashboard = menuSectionService.findByTittle("Menu");
            if (menuSectionDashboard != null) {
                //Ajout du menu dashboard
                MenuDto menuDto = menuService.findByMenuValue("Dashboard");
                if (menuDto != null) {
                    //System.out.println(menuDto);
                    menuDto.setMenuSection(null);
                    menuSectionDashboard.setMenu(new ArrayList<>());
                    menuSectionDashboard.getMenu().add(menuDto);

                }
//                System.out.println(menuSectionDashboard);

                menuSections.add(menuSectionDashboard);
            }
            privilegesTries.forEach(privilegeDto -> {
                if (menuSections.stream().noneMatch(menuSectionDto -> menuSectionDto.getId().equals(privilegeDto.getSubMenu().getMenu().getMenuSection().getId()))) {
//                    System.out.println(privilegeDto.getSubMenu().getMenu().getMenuSection());
                    menuSections.add(privilegeDto.getSubMenu().getMenu().getMenuSection());
                }
            });
            //System.out.println(menuSections.size());

            //Ordonnancement des sections par position
            menuSections.sort((menuSection1, menuSection2) -> menuSection1.getPosition().compareTo(menuSection2.getPosition()));
            //menuSections.forEach(menuSectionDto -> System.out.println(menuSectionDto.getPosition()));

            menuSections.stream().skip(1).forEach(menuSectionDto -> {
                List<MenuDto> menus = new ArrayList<>();

                privilegesTries.forEach(privilegeDto -> {
                    if (Objects.equals(privilegeDto.getSubMenu().getMenu().getMenuSection().getId(), menuSectionDto.getId())
                            && menus.stream().noneMatch(menuDto -> menuDto.getId().equals(privilegeDto.getSubMenu().getMenu().getId()))) {
                        MenuDto menu = MenuDto.builder()
                                .id(privilegeDto.getSubMenu().getMenu().getId())
                                .label(privilegeDto.getSubMenu().getMenu().getLabel())
                                .link(privilegeDto.getSubMenu().getMenu().getLink())
                                .icon(privilegeDto.getSubMenu().getMenu().getIcon())
                                .isLayout(privilegeDto.getSubMenu().getMenu().isLayout())
                                .isTitle(privilegeDto.getSubMenu().getMenu().isTitle())
                                .position(privilegeDto.getSubMenu().getMenu().getPosition())
                                .build();
                        menus.add(menu);
                    }
                });
//                System.out.println(menus.size());

                //Ordonnancement des menus par position
                menus.sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));

                menus.forEach(menuDto -> {
//                    System.out.println(menuDto);
                    List<SubMenuDto> subMenus = new ArrayList<>();
                    privileges.forEach(privilegeDto -> {
                        if (Objects.equals(privilegeDto.getSubMenu().getMenu().getId(), menuDto.getId())
                                && subMenus.stream().noneMatch(subMenuDto -> subMenuDto.getId().equals(privilegeDto.getSubMenu().getId()))) {

                            SubMenuDto subMenuDto = SubMenuDto.builder()
                                    .id(privilegeDto.getSubMenu().getId())
                                    .label(privilegeDto.getSubMenu().getLabel())
                                    .link(privilegeDto.getSubMenu().getLink())
                                    .position(privilegeDto.getSubMenu().getPosition())
                                    .build();
                            subMenus.add(subMenuDto);
                            //System.out.println("privilegeDto.getSubMenu() " + (privilegeDto.getSubMenu().getPosition() == null));
                        }
                    });

                    //Ordonnancement des sous-menus par position
                    subMenus.sort((subMenu1, subMenu2) -> subMenu1.getPosition().compareTo(subMenu2.getPosition()));

                    menuDto.setSubMenus(subMenus);
                });

                menuSectionDto.setMenu(menus);
            });

            return Utilities.createSuccessResponse(HttpStatus.OK, menuSections, "Liste des MenuSection");
        }

        log.info("No element found while hitting getAllMenuSection");
        return Utilities.createErrorResponse("Aucune MenuSection trouvée", List.of(), HttpStatus.OK);
    }


    @GetMapping("/pages")
    public ResponseEntity<Map<String, Object>> getAllByPage(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        Page<MenuDto> pages = menuService.getAllPagined(pageNum, size);
        List<MenuDto> menus = pages.getContent();
        if (!menus.isEmpty()) {
            pages.getContent().sort((menu1, menu2) -> menu1.getPosition().compareTo(menu2.getPosition()));
            return Utilities.createSuccessResponse(HttpStatus.OK, pages, "Liste des Menu");
        }

        log.info("No element found while hitting getAll");
        return Utilities.createErrorResponse("Aucune Menu trouvée", List.of(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody MenuDto MenuDto) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = MenuDtoValidator.validate(MenuDto);
            if (!errors.isEmpty()) {
                log.error("Menus n'est pas valide {}", MenuDto);
                throw new InvalidEntityException("Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            if (MenuDto.getId() == 0) MenuDto.setId(null);

            //Insertion de l'typeMenus
            MenuDto = menuService.saveOrUpdate(MenuDto);

        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return Utilities.createErrorResponse("Création échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return Utilities.createSuccessResponse(HttpStatus.CREATED, MenuDto, "Menus créé avec succès");
    }

    @PostMapping("/load-actual-menu")
    public ResponseEntity<Map<String, Object>> saveActualMenu(@RequestBody List<MenuSectionDto> menuSection) {
        log.trace("Starting processing Post request!");
        try {

            //Controle de conformité
            List<String> errors = new ArrayList<>();
            menuSection.forEach(menuSectionDto -> {
                errors.addAll(MenuSectionDtoValidator.validate(menuSectionDto));
                if (menuSectionDto.getMenu() != null) {
                    menuSectionDto.getMenu().forEach(menuDto -> {
                        errors.addAll(MenuDtoValidator.validate(menuDto));

                        if (menuDto.getSubMenus() != null) {
                            menuDto.getSubMenus().forEach(subMenuDto -> errors.addAll(SousMenuDtoValidator.validate(subMenuDto)));
                        }
                    });
                }
            });

            if (!errors.isEmpty()) {
                log.error("Menus n'est pas valide {}", menuSection);
                throw new InvalidEntityException("Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }

            //Sauvegarde des sections
//            System.out.println("menuSection list " + menuSection);
            menuSection.forEach(menuSectionDto -> {
                //if (menuSectionDto.getId() == 0) menuSectionDto.setId(null);
                MenuSectionDto menuSectionCreate = menuSectionService.saveOrUpdate(menuSectionDto);

                //Sauvegarde des menus
                if (menuSectionDto.getMenu() != null) {
//                    System.out.println("menuSectionDto.getMenu() size: " + menuSectionDto.getMenu().size());
                    menuSectionDto.getMenu().forEach(menuDto -> {
                        //if (menuDto.getId() == 0) menuDto.setId(null);
                        menuDto.setMenuSection(menuSectionCreate);
                        MenuDto menuCreate = menuService.saveOrUpdate(menuDto);

                        //Sauvegarde des sous-menus
                        if (menuDto.getSubMenus() != null) {
                            menuDto.getSubMenus().forEach(subMenuDto -> {
                                //if (subMenuDto.getId() == 0) subMenuDto.setId(null);
                                subMenuDto.setMenu(menuCreate);
                                sousMenuService.saveOrUpdate(subMenuDto);
                            });
                        }
                    });
                }
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
        return Utilities.createSuccessResponse(HttpStatus.CREATED, menuSection, "Menus créé avec succès");
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @RequestBody MenuDto menuDto) {

        log.trace("Starting processing put for id :" + id);

        try {
            //Controle de conformité
            List<String> errors = MenuDtoValidator.validate(menuDto);
            if (!errors.isEmpty()) {
                log.error("Menus n'est pas valide {}", menuDto);
                throw new InvalidEntityException("Menus n'est pas valide", ErrorCodes.NOT_VALID, errors);
            }
        } catch (InvalidEntityException ex) {
            log.error("Error Occured while saving, Message : " + ex.getMessage() + "; Cause :" + ex.getCause());
            return Utilities.createErrorResponse("Données invalides", ex.getErrors(), HttpStatus.BAD_REQUEST);
        }

        //Recherche de l'typeMenus dans la BD
        MenuDto MenuDtoSearch = menuService.get(id);

        if (MenuDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            menuDto.setId(MenuDtoSearch.getId());

            try {
                menuDto = menuService.saveOrUpdate(menuDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return Utilities.createErrorResponse("Modification échouée", err.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Utilities.createSuccessResponse(HttpStatus.OK, menuDto, "Menus modifiée avec succès");
        }

        log.info("Id mismatch for model (" + menuDto + ") and request param :" + id);
        return Utilities.createSuccessResponse(HttpStatus.NOT_FOUND, menuDto, "Menus avec l'id " + id + " non trouvée");
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete request for id :" + id);

        if (!menuService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return Utilities.createErrorResponse("Menus avec l'id" + id + " non trouvée", Optional.empty(), HttpStatus.NO_CONTENT);
        }

        menuService.delete(id);
        log.info("Entity deleted having id :" + id);
        return Utilities.createSuccessResponse(HttpStatus.OK, Optional.empty(), "Menus supprimée avec succès");
    }
}
