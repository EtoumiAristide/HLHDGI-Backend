package com.elpandor.hlh.modules.hlh.factures.repository;

import com.elpandor.hlh.modules.hlh.factures.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactureRepository extends JpaRepository<Facture, Integer> {
}
