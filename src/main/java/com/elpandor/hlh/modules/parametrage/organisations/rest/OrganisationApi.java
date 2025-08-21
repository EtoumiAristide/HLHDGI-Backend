package com.elpandor.hlh.modules.parametrage.organisations.rest;

import com.elpandor.hlh.common.service.FileStorageService;
import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.OrganisationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/organisations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class OrganisationApi {
    private Logger log = LoggerFactory.getLogger(OrganisationApi.class);

    private final OrganisationService organisationService;

    private final FileStorageService fileStorageService;

    //@Value("${upload_dir}")
    private String uploadsDir = "uploads";

    private Path root;

    @Autowired
    public OrganisationApi(OrganisationService organisationService, FileStorageService fileStorageService) {
        this.organisationService = organisationService;
        this.fileStorageService = fileStorageService;
        initRootPath();
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<OrganisationDto> get(@PathVariable Integer id) {
        log.trace("Starting processing get request for id :" + id);

        OrganisationDto organisationDto = organisationService.get(id);
        if (organisationDto != null) {
            organisationDto.setLogo(Utilities.getFileUri(organisationDto.getLogo(), "organisations/logo"));
            return new ResponseEntity<>(organisationDto, HttpStatus.OK);
        }

        log.info("Entity having id not found, id : " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    public ResponseEntity<List<OrganisationDto>> getAll(@RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "size", required = false) Integer size) {
        log.trace("Starting processing getAll request!");

        List<OrganisationDto> organisations = organisationService.getAll();
        if (organisations != null && !organisations.isEmpty()) {
            organisations.forEach(organisationDto -> {
                organisationDto.setLogo(Utilities.getFileUri(organisationDto.getLogo(), "organisations/logo"));
            });

            return new ResponseEntity<>(organisations, HttpStatus.OK);
        }

        log.info("No element found while hitting getAll");
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/utilisateur")
    public ResponseEntity<CompteUtilisateurDto> getByUtilisateur(@AuthenticationPrincipal Jwt jwt) {
        log.trace("Starting processing getAll request!");
        String userId = jwt.getClaim("sub");
        CompteUtilisateurDto organisationUtilisateurDto = organisationService.getOrganisationByUtilisateur(UUID.fromString(userId));

        if (organisationUtilisateurDto != null) {
            organisationUtilisateurDto.getOrganisation().setLogo(Utilities.getFileUri(organisationUtilisateurDto.getOrganisation().getLogo(), "organisations/logo"));
            return new ResponseEntity<>(organisationUtilisateurDto, HttpStatus.OK);
        }

        log.info("No element found while hitting getAll");
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrganisationDto> save(@RequestParam(required = false, value = "image") MultipartFile image, @ModelAttribute OrganisationDto organisationDto) {
        log.trace("Starting processing Post request!");
        try {

//            System.out.println(organisationDto);
            String fileName = fileStorageService.storeFile(image, organisationDto.getRaisonSocial().replaceAll("[^a-zA-Z0-9]", ""));

//            String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
//                    .path("api/v1/organisations/logo/")
//                    .path(fileName)
//                    .toUriString();


            //Upload du fichier
            //Path filepath = write(image, this.root.getFileName(), organisationDto.getCode());
            organisationDto.setId(null);
            organisationDto.setLogo(fileName);

            //Insertion de l'organisation
            organisationDto = organisationService.saveOrUpdate(organisationDto);

        } catch (Exception err) {
            log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(organisationDto, HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<OrganisationDto> update(@PathVariable Integer id, @RequestParam(required = false, value = "image") MultipartFile image, @ModelAttribute OrganisationDto organisationDto) {

        log.trace("Starting processing put for id :" + id);
        //Recherche de l'organisation dans la BD
        OrganisationDto organisationDtoSearch = organisationService.get(id);

        if (organisationDtoSearch != null) {
            log.trace("processing put request for id :" + id);
            organisationDto.setId(organisationDtoSearch.getId());

            try {

                if (image != null) {
                    String fileName = fileStorageService.storeFile(image, organisationDto.getRaisonSocial().replaceAll("[^a-zA-Z0-9]", ""));

//                    String fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
//                            .path("api/v1/organisations/logo/")
//                            .path(fileName)
//                            .toUriString();


                    //Upload du fichier
                    //Path filepath = write(image, this.root.getFileName(), organisationDto.getCode());
                    organisationDto.setLogo(fileName);
                } else {
                    organisationDto.setLogo(organisationDtoSearch.getLogo());
                }

//                if (organisationDto.getIsPrincipal() == 1) {
//                    //Actualisation du statut de toutes les entreprises
//                    organisationService.updateIsPrincipal();
//                }

                organisationDto = organisationService.saveOrUpdate(organisationDto);
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(organisationDto, HttpStatus.OK);
        }

        log.info("Id mismatch for model (" + organisationDto + ") and request param :" + id);
        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        log.trace("Starting  processing of delete rrequest for id :" + id);

        if (!organisationService.isExist(id)) {
            log.info("Entity not found while processing the delete request for id :" + id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        organisationService.delete(id);
        log.info("Entity deleted having id :" + id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void initRootPath() {
        //Creation du repertoire de sauvegarde s'il y'a lieu
        File file = new File(uploadsDir);
        if (!file.exists()) {
            file.mkdir();
        }

        root = Paths.get(file.getPath());
    }

    private Path write(MultipartFile file, Path dir, String filename) throws IOException {

        Path filepath = null;
        if (file != null) {
            filepath = Paths.get(dir.toString(), "logo-" + filename + file.getOriginalFilename().substring(file.getOriginalFilename().length() - 4));
            try (OutputStream os = Files.newOutputStream(filepath)) {
                os.write(file.getBytes());
            } catch (Exception err) {
                log.error("Error Occured while saving, Message : " + err.getMessage() + "; Cause :" + err.getCause());
            }
        }
        return filepath;
    }

//    private String getFileUri(String fileName) {
//        return ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("api/v1/organisations/logo/")
//                .path(fileName)
//                .toUriString();
//    }

    @GetMapping("/logo/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
