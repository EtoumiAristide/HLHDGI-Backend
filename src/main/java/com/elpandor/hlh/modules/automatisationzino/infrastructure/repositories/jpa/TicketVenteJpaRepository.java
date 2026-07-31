package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.TicketVenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketVenteJpaRepository extends JpaRepository<TicketVenteEntity, Long> {

    List<TicketVenteEntity> findByNomFichierSource(String nomFichierSource);

    List<TicketVenteEntity> findByModePaiement(String modePaiement);
}
