package com.elpandor.hlh.modules.parametrage.privillege.model.dto;

import com.elpandor.hlh.modules.parametrage.menus.model.dto.SubMenuDto;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PrivilegePayload {
    private RoleDto role;
    private List<SubMenuDto> subMenus;
}
