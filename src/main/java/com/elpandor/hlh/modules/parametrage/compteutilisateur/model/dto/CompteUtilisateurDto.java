package com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto;

import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CompteUtilisateurDto {
    private Integer id;
    private String keycloakUserId;
    private String firstname;
    private String lastname;
    private String phone;
    private String login;
    private String password;
    private String email;
    private String fonction;
    private boolean enable;
    private List<RoleDto> roles;
    private OrganisationDto organisation;
}
