package com.elpandor.hlh.modules.parametrage.roleutilisateur.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.mapper.RoleUtilisateurMapper;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.RoleUtilisateur;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto.RoleUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.repository.RoleUtilisateurRepository;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.service.RoleUtilisateurService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleUtilisateurServiceImpl extends GenericServiceImpl<RoleUtilisateur, Integer, RoleUtilisateurDto> implements RoleUtilisateurService {

    private final RoleUtilisateurRepository roleUtilisateurRepository;

    public RoleUtilisateurServiceImpl(JpaRepository<RoleUtilisateur, Integer> repository, RoleUtilisateurRepository roleUtilisateurRepository) {
        super(repository);
        this.roleUtilisateurRepository = roleUtilisateurRepository;
    }

    @Override
    public RoleUtilisateur transformDTOToEntity(RoleUtilisateurDto element) {
        RoleUtilisateur directionMission = RoleUtilisateurMapper.INSTANCE.toEntity(element);
        directionMission.setIsdelete(false);
        return directionMission;
    }

    @Override
    public RoleUtilisateurDto transformEntityToDTO(RoleUtilisateur element) {
        return RoleUtilisateurMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<RoleUtilisateurDto> findAllByRole(Integer roleId) {
        return roleUtilisateurRepository.findAllByRole_Id(roleId).stream().map(RoleUtilisateurMapper.INSTANCE::toDto).toList();
    }

    @Override
    public List<RoleUtilisateurDto> findAllByUtilisateur(Integer userId) {
        return roleUtilisateurRepository.findAllByCompteUtilisateur_Id(userId).stream().map(RoleUtilisateurMapper.INSTANCE::toDto).toList();
    }
}
