package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.impl;

import com.elpandor.hlh.modules.automatisationzino.domain.repository.RepartitionPaiementRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.RepartitionParModePaiement;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.RepartitionPaiementEntity;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa.RepartitionPaiementJpaRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper.RepartitionPaiementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RepartitionPaiementRepositoryImpl implements RepartitionPaiementRepository {

    private final RepartitionPaiementJpaRepository jpaRepository;
    private final RepartitionPaiementMapper mapper;

    @Override
    public List<RepartitionParModePaiement> saveAll(List<RepartitionParModePaiement> repartitions) {
        List<RepartitionPaiementEntity> entities = repartitions.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        List<RepartitionPaiementEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
