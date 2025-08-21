package com.elpandor.hlh.modules.parametrage.role.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.role.mapper.RoleMapper;
import com.elpandor.hlh.modules.parametrage.role.model.Role;
import com.elpandor.hlh.modules.parametrage.role.model.dto.RoleDto;
import com.elpandor.hlh.modules.parametrage.role.service.RoleService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends GenericServiceImpl<Role, Integer, RoleDto> implements RoleService {

    public RoleServiceImpl(JpaRepository<Role, Integer> repository) {
        super(repository);
    }

    @Override
    public Role transformDTOToEntity(RoleDto element) {
        Role role = RoleMapper.INSTANCE.toEntity(element);
        role.setIsdelete(false);
        return role;
    }

    @Override
    public RoleDto transformEntityToDTO(Role element) {
        return RoleMapper.INSTANCE.toDto(element);
    }
}
