package com.elpandor.hlh.modules.hlh.repository;

import com.elpandor.hlh.modules.hlh.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactureRepository extends JpaRepository<Facture, Integer> {
    Page<Facture> findByPointVente_Organisation_RaisonSocial(Pageable pageable, String entreprise);
}
