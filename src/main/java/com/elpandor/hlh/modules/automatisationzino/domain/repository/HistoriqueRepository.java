package com.elpandor.hlh.modules.automatisationzino.domain.repository;

import com.elpandor.hlh.modules.automatisationzino.domain.model.HistoriqueEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.model.StatutEnvoi;
import java.util.List;

public interface HistoriqueRepository {
    HistoriqueEnvoi save(HistoriqueEnvoi historique);
    List<HistoriqueEnvoi> findByStatut(StatutEnvoi statut);
    List<HistoriqueEnvoi> findByNomFichier(String nomFichier);
    HistoriqueEnvoi findLatestByNomFichier(String nomFichier);
    List<HistoriqueEnvoi> findRecentFailures(int limit);
}