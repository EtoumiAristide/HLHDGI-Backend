package com.elpandor.hlh.modules.parametrage.compteutilisateur.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;

import java.util.List;

public interface CompteUtilisateurService extends GenericService<CompteUtilisateur, Integer, CompteUtilisateurDto> {
    public CompteUtilisateurDto findByUtilisateurId(String utilisateurId);
    public CompteUtilisateurDto findByLogin(String login);
}
