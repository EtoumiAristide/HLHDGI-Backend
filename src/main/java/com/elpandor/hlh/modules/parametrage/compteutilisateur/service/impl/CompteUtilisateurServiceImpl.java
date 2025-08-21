package com.elpandor.hlh.modules.parametrage.compteutilisateur.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.mapper.CompteUtilisateurMapper;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.repository.CompteUtilisateurRepository;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.service.CompteUtilisateurService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompteUtilisateurServiceImpl extends GenericServiceImpl<CompteUtilisateur, Integer, CompteUtilisateurDto> implements CompteUtilisateurService {

    private final CompteUtilisateurRepository compteUtilisateurRepository;

    public CompteUtilisateurServiceImpl(JpaRepository<CompteUtilisateur, Integer> repository, CompteUtilisateurRepository carteRepository) {
        super(repository);
        this.compteUtilisateurRepository = carteRepository;
    }

    @Override
    public CompteUtilisateur transformDTOToEntity(CompteUtilisateurDto element) {
        CompteUtilisateur carte = CompteUtilisateurMapper.INSTANCE.toEntity(element);
        carte.setIsdelete(false);
        return carte;
    }

    @Override
    public CompteUtilisateurDto transformEntityToDTO(CompteUtilisateur element) {
        return CompteUtilisateurMapper.INSTANCE.toDto(element);
    }

    @Override
    public CompteUtilisateurDto findByUtilisateurId(String utilisateurId) {
        return CompteUtilisateurMapper.INSTANCE.toDto(compteUtilisateurRepository.findByKeycloakUserId(utilisateurId));
    }

    @Override
    public CompteUtilisateurDto findByLogin(String login) {
        return CompteUtilisateurMapper.INSTANCE.toDto(compteUtilisateurRepository.findByLogin(login));
    }
}
