package com.elpandor.hlh.modules.parametrage.organisations.repository;

import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointVenteRepository extends JpaRepository<PointVente, Integer> {
    List<PointVente> findByEtablissementId(Integer etablissementId);
    List<PointVente> findByEtablissementOrganisationId(Integer organisationId);
    PointVente findByNom(String nom);
}