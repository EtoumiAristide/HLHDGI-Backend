package com.elpandor.hlh.modules.automatisationzino.domain.repository;

import com.elpandor.hlh.modules.automatisationzino.infrastructure.parser.RepartitionParModePaiement;

import java.util.List;

/**
 * Port de persistance de la répartition des ventes par mode de paiement.
 */
public interface RepartitionPaiementRepository {

    List<RepartitionParModePaiement> saveAll(List<RepartitionParModePaiement> repartitions);
}
