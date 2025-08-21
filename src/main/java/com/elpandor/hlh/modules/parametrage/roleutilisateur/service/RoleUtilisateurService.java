package com.elpandor.hlh.modules.parametrage.roleutilisateur.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.RoleUtilisateur;
import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.dto.RoleUtilisateurDto;

import java.util.List;

public interface RoleUtilisateurService extends GenericService<RoleUtilisateur, Integer, RoleUtilisateurDto> {
    public List<RoleUtilisateurDto> findAllByRole(Integer roleId);
    public List<RoleUtilisateurDto> findAllByUtilisateur(Integer userId);
}
