package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.impl;

import com.elpandor.hlh.modules.automatisationzino.domain.repository.TicketVenteRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.TicketVenteZino;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity.TicketVenteEntity;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.jpa.TicketVenteJpaRepository;
import com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.mapper.TicketVenteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TicketVenteRepositoryImpl implements TicketVenteRepository {

    private final TicketVenteJpaRepository jpaRepository;
    private final TicketVenteMapper mapper;

    @Override
    public List<TicketVenteZino> saveAll(List<TicketVenteZino> tickets, String nomFichierSource) {
        List<TicketVenteEntity> entities = tickets.stream()
                .map(mapper::toEntity)
                .peek(entity -> entity.setNomFichierSource(nomFichierSource))
                .collect(Collectors.toList());

        List<TicketVenteEntity> saved = jpaRepository.saveAll(entities);

        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketVenteZino> findByNomFichierSource(String nomFichierSource) {
        return jpaRepository.findByNomFichierSource(nomFichierSource).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
