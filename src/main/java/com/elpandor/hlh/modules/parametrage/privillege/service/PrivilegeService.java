package com.elpandor.hlh.modules.parametrage.privillege.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.privillege.model.Privilege;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;

import java.util.List;

public interface PrivilegeService extends GenericService<Privilege, Integer, PrivilegeDto> {
    public List<PrivilegeDto> findAllByRole(Integer roleId);
}
