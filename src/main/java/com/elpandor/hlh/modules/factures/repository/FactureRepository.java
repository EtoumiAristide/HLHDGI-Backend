package com.elpandor.hlh.modules.factures.repository;

import com.elpandor.hlh.modules.factures.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactureRepository extends JpaRepository<Facture, Integer> {
}
