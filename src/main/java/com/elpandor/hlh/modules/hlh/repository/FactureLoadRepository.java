package com.elpandor.hlh.modules.hlh.repository;

import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.FactureLoad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FactureLoadRepository extends JpaRepository<FactureLoad, UUID> {
    Page<FactureLoad> findByPointVente_Etablissement_Organisation_RaisonSocialOrderByIdDesc(Pageable pageable, String entreprise);
}
