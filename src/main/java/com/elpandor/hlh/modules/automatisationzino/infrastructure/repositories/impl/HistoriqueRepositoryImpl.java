package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.impl;

import com.elpandor.hlh.modules.automatisationzino.domain.model.HistoriqueEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.model.StatutEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.HistoriqueRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.HistoriqueEnvoiEntity;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa.HistoriqueJpaRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper.HistoriqueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HistoriqueRepositoryImpl implements HistoriqueRepository {

    private final HistoriqueJpaRepository jpaRepository;
    private final HistoriqueMapper mapper;

    @Override
    public HistoriqueEnvoi save(HistoriqueEnvoi historique) {
        HistoriqueEnvoiEntity entity = mapper.toEntity(historique);
        HistoriqueEnvoiEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<HistoriqueEnvoi> findByStatut(StatutEnvoi statut) {
        return jpaRepository.findByStatut(statut.name()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<HistoriqueEnvoi> findByNomFichier(String nomFichier) {
        return jpaRepository.findByNomFichier(nomFichier).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public HistoriqueEnvoi findLatestByNomFichier(String nomFichier) {
        HistoriqueEnvoiEntity entity = jpaRepository.findLatestByNomFichier(nomFichier);
        return entity != null ? mapper.toDomain(entity) : null;
    }

    @Override
    public List<HistoriqueEnvoi> findRecentFailures(int limit) {
        return jpaRepository.findRecentFailures(limit).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}