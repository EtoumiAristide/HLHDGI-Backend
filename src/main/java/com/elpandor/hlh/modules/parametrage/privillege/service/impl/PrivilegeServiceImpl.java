package com.elpandor.hlh.modules.parametrage.privillege.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.privillege.mapper.PrivilegeMapper;
import com.elpandor.hlh.modules.parametrage.privillege.model.Privilege;
import com.elpandor.hlh.modules.parametrage.privillege.model.dto.PrivilegeDto;
import com.elpandor.hlh.modules.parametrage.privillege.repository.PrivilegeRepository;
import com.elpandor.hlh.modules.parametrage.privillege.service.PrivilegeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivilegeServiceImpl extends GenericServiceImpl<Privilege, Integer, PrivilegeDto> implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;

    public PrivilegeServiceImpl(JpaRepository<Privilege, Integer> repository, PrivilegeRepository carteRepository) {
        super(repository);
        this.privilegeRepository = carteRepository;
    }

    @Override
    public Privilege transformDTOToEntity(PrivilegeDto element) {
        Privilege privilege = PrivilegeMapper.INSTANCE.toEntity(element);
        privilege.setIsdelete(false);
        return privilege;
    }

    @Override
    public PrivilegeDto transformEntityToDTO(Privilege element) {
        return PrivilegeMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<PrivilegeDto> findAllByRole(Integer typeCarteId) {
        return privilegeRepository.findAllByRole_Id(typeCarteId).stream().map(PrivilegeMapper.INSTANCE::toDto).collect(Collectors.toList());
    }
}
