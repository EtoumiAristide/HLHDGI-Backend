package com.elpandor.hlh.modules.parametrage.privillege.model.dto;

import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrivilegeDto {
    private Integer id;
//    private String libelle;
//    private String description;
    private RoleDto role;
    private SubMenuDto subMenu;
}
