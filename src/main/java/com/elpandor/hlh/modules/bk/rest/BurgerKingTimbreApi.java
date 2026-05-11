package com.elpandor.hlh.modules.bk.rest;

import com.elpandor.hlh.modules.bk.mapper.BkTimbreMapper;
import com.elpandor.hlh.modules.bk.model.BkTimbreMonthlyReport;
import com.elpandor.hlh.modules.bk.model.BkTimbreRequest;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreDetailDto;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreMonthlyReportDto;
import com.elpandor.hlh.modules.bk.model.dto.BkTimbreRequestDto;
import com.elpandor.hlh.modules.bk.service.BurgerKingTimbreService;
import com.elpandor.hlh.common.utils.Utilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/bk/timbre")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BurgerKingTimbreApi {

    private final BurgerKingTimbreService burgerKingTimbreService;
    private final BkTimbreMapper bkTimbreMapper;

    public BurgerKingTimbreApi(BurgerKingTimbreService burgerKingTimbreService, BkTimbreMapper bkTimbreMapper) {
        this.burgerKingTimbreService = burgerKingTimbreService;
        this.bkTimbreMapper = bkTimbreMapper;
    }

    @PostMapping(path = "/calculate")
    public ResponseEntity<Map<String, Object>> calculate(@RequestBody BkTimbreRequestDto requestDto) {
        BkTimbreRequest request = bkTimbreMapper.toEntity(requestDto);
        BkTimbreMonthlyReport report = burgerKingTimbreService.buildReport(request);
        BkTimbreMonthlyReportDto reportDto = bkTimbreMapper.toDto(report);
        List<BkTimbreDetailDto> details = reportDto.getDetails();

        Map<String, Object> result = new HashMap<>();
        result.put("report", reportDto);
        result.put("details", details);
        return Utilities.createSuccessResponse(HttpStatus.OK, result, "Calcul des droits de timbre BK effectué avec succès");
    }

    @PostMapping(path = "/export/csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportCsv(@RequestBody BkTimbreRequestDto requestDto) {
        BkTimbreRequest request = bkTimbreMapper.toEntity(requestDto);
        String csv = burgerKingTimbreService.exportCsv(burgerKingTimbreService.calculateDetails(request));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=BK_Timbre_Report.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @PostMapping(path = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(@RequestBody BkTimbreRequestDto requestDto) {
        BkTimbreRequest request = bkTimbreMapper.toEntity(requestDto);
        byte[] excel = burgerKingTimbreService.exportExcel(burgerKingTimbreService.calculateDetails(request));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=BK_Timbre_Report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping(path = "/rules")
    public ResponseEntity<Map<String, Object>> rules() {
        System.out.println("🔍 BK API: Rules endpoint called");
        Map<String, Object> rules = new HashMap<>();
        rules.put("eligiblePaymentTypes", new String[]{"CASH", "HD_GLOVO"});
        rules.put("threshold", 5000);
        rules.put("stampDuty", 100);
        rules.put("description", "Droits de timbre automatiques pour Burger King sur paiements Cash et Glovo >= 5000 FCFA.");
        return Utilities.createSuccessResponse(HttpStatus.OK, rules, "Règles de gestion du droit de timbre BK");
    }

    @GetMapping(path = "/roles")
    public ResponseEntity<Map<String, Object>> getAuthorizedRoles() {
        //System.out.println("🔑 BK API: Roles endpoint called");

        // Récupérer les rôles de l'utilisateur connecté depuis le SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        List<String> userRoles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
            .map(role -> role.startsWith("realm_") ? role.substring(6) : role) // Enlever aussi le préfixe realm_
            .filter(role -> role.toLowerCase().contains("bk")) // Garder seulement les rôles BK
            .collect(Collectors.toList());

        Map<String, Object> roles = new HashMap<>();
        roles.put("authorizedRoles", userRoles);
        roles.put("description", "Rôles de l'utilisateur connecté pour Burger King");
        return Utilities.createSuccessResponse(HttpStatus.OK, roles, "Rôles utilisateur BK récupérés");
    }
}
