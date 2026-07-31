package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.impl;

import com.elpandor.hlh.modules.automatisationzino.domain.model.FichierSource;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.FichierSourceRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.FichierSourceEntity;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa.FichierSourceJpaRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper.FichierSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FichierSourceRepositoryImpl implements FichierSourceRepository {

    private final FichierSourceJpaRepository jpaRepository;
    private final FichierSourceMapper mapper;

    @Override
    public List<FichierSource> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FichierSource> findByStatut(String statut) {
        return jpaRepository.findByStatut(statut).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FichierSource> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FichierSource> findByNomFichier(String nomFichier) {
        return jpaRepository.findByNomFichier(nomFichier)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByNomFichier(String nomFichier) {
        return jpaRepository.existsByNomFichier(nomFichier);
    }

    @Override
    public FichierSource save(FichierSource fichierSource) {
        FichierSourceEntity entity = mapper.toEntity(fichierSource);
        FichierSourceEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void updateStatut(Long id, String statut, String messageErreur) {
        jpaRepository.updateStatut(id, statut, messageErreur);
    }

    @Override
    public void incrementerTentative(Long id) {
        jpaRepository.incrementerTentative(id);
    }

    @Override
    public void sauvegarderDonneesExtraites(Long id, String jsonData) {
        jpaRepository.sauvegarderDonneesExtraites(id, jsonData);
    }
}