package com.elpandor.hlh.modules.parametrage.organisations.repository;

import com.elpandor.hlh.modules.parametrage.organisations.model.Etablissement;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtablissementRepository extends JpaRepository<Etablissement, Integer> {
    List<Etablissement> findByOrganisationId(Integer organisationId);
    Etablissement findByNomContainingIgnoreCase(String nom);
}