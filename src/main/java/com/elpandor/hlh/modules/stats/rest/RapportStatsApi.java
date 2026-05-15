package com.elpandor.hlh.modules.stats.rest;

import com.elpandor.hlh.common.utils.Utilities;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import com.elpandor.hlh.modules.parametrage.organisations.service.OrganisationService;
import com.elpandor.hlh.modules.stats.model.FactureTimbre;
import com.elpandor.hlh.modules.stats.model.FactureTimbreRequest;
import com.elpandor.hlh.modules.stats.model.FactureTimbreTotaux;
import com.elpandor.hlh.modules.stats.service.RapportStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rapport-stats")
@RequiredArgsConstructor
public class RapportStatsApi {
    private final RapportStatsService rapportStatsService;
    private final OrganisationService organisationService;
    private final EtablissementService etablissementService;

    @PostMapping("/facture-timbre")
    public ResponseEntity<Map<String, Object>> getFactureTimbre(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size,
                                                                @RequestParam(defaultValue = "mois") String sortBy,
                                                                @RequestParam(defaultValue = "ASC") String direction,
                                                                @RequestBody FactureTimbreRequest request,
                                                                @AuthenticationPrincipal Jwt jwt) {

        //Recuperation automatique du numero ncc
        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        String entreprise = "";
        EtablissementDto etablissement = null;
        if (groups != null) {
            entreprise = groups.get(0);
            etablissement = etablissementService.findByNom(entreprise);
        }

        if (etablissement == null)
            return Utilities.createErrorResponse("Vous n'êtes pas autorisé à utliser cette ressource", List.of(), HttpStatus.UNAUTHORIZED);

        request.setNumcc(etablissement.getOrganisation().getNumcc());
        Page<FactureTimbre> factureTimbres = rapportStatsService.getFactureTimbre(page, size, sortBy, direction, request);

        return Utilities.createSuccessResponse(HttpStatus.OK, factureTimbres, "Liste des timbres");
    }

    @PostMapping(value = "/facture-timbre/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportTimbreToExcel(@Valid @RequestBody FactureTimbreRequest request,
                                                      @AuthenticationPrincipal Jwt jwt) {

        // Récupération automatique du numero ncc
        List<String> groups = jwt.getClaim("groups");
        String entreprise = "";
        EtablissementDto etablissement = null;
        if (groups != null) {
            entreprise = groups.get(0);
            etablissement = etablissementService.findByNom(entreprise);
        }

        if (etablissement == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        request.setNumcc(etablissement.getOrganisation().getNumcc());

        try {
            byte[] excelBytes = rapportStatsService.exportTimbreToExcel(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "timbre_factures_" + request.getDateDebut() + "_" + request.getDateFin() + ".xlsx");
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/totaux-facture-timbre")
    public ResponseEntity<Map<String, Object>> getTotauxFactureTimbre(@RequestBody FactureTimbreRequest request,
                                                                      @AuthenticationPrincipal Jwt jwt) {

        //Recuperation automatique du numero ncc
        //Recuperation du group
        List<String> groups = jwt.getClaim("groups");
        String entreprise = "";
        EtablissementDto etablissement = null;
        if (groups != null) {
            entreprise = groups.get(0);
            etablissement = etablissementService.findByNom(entreprise);
        }

        if (etablissement == null)
            return Utilities.createErrorResponse("Vous n'êtes pas autorisé à utliser cette ressource", List.of(), HttpStatus.UNAUTHORIZED);

        request.setNumcc(etablissement.getOrganisation().getNumcc());
        Map<String, Map<String, Map<String, FactureTimbreTotaux>>> factureTimbres = rapportStatsService.getFactureTimbreTotaux(request);

        return Utilities.createSuccessResponse(HttpStatus.OK, factureTimbres, "Liste des totaux timbres");
    }
}
