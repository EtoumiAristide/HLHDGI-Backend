package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.RepartitionPaiementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepartitionPaiementJpaRepository extends JpaRepository<RepartitionPaiementEntity, Long> {

    List<RepartitionPaiementEntity> findByModePaiement(String modePaiement);
}
