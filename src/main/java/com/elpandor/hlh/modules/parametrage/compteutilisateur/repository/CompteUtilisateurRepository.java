package com.elpandor.hlh.modules.parametrage.compteutilisateur.repository;

import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompteUtilisateurRepository extends JpaRepository<CompteUtilisateur, Integer> {
    CompteUtilisateur findByKeycloakUserId(String utilisateurId);
    CompteUtilisateur findByLogin(String login);
}
