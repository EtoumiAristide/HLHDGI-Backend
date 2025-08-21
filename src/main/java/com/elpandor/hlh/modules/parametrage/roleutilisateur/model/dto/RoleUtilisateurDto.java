package com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto;

import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RoleUtilisateurDto {
    private Integer id;
    private LocalDate date;
    private RoleDto role;
    private CompteUtilisateurDto compteUtilisateur;
}
