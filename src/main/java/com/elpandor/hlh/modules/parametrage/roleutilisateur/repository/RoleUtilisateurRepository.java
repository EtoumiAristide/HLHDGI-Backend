package com.elpandor.hlh.modules.parametrage.roleutilisateur.repository;

import com.elpandor.hlh.modules.parametrage.roleutilisateur.model.RoleUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleUtilisateurRepository extends JpaRepository<RoleUtilisateur, Integer> {
    public List<RoleUtilisateur> findAllByRole_Id(int missionId);
    public List<RoleUtilisateur> findAllByCompteUtilisateur_Id(int directionId);
}
