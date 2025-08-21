package com.elpandor.hlh.modules.parametrage.role.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDto {
    private Integer id;
    private String libelle;
    private String description;
}
