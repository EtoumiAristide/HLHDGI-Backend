package com.elpandor.hlh.modules.automatisationzino.application.usecases;

import com.elpandor.hlh.modules.automatisationzino.application.dto.ResultatEnvoiFNE;
import com.elpandor.hlh.modules.automatisationzino.domain.model.HistoriqueEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.model.StatutEnvoi;
import com.elpandor.hlh.modules.automatisationzino.domain.repository.HistoriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriserEnvoiUseCase {

    private final HistoriqueRepository historiqueRepository;

    public HistoriqueEnvoi executer(String nomFichier, ResultatEnvoiFNE reponse, Integer tentative, Long tempsExecutionMs, String codeProduit) {
        log.info("Historisation de l'envoi pour le fichier: {}", nomFichier);

        StatutEnvoi statut = reponse.isSucces() ? StatutEnvoi.SUCCES : StatutEnvoi.ECHEC;

        HistoriqueEnvoi historique = HistoriqueEnvoi.builder()
                .nomFichier(nomFichier)
                .statut(statut)
                .codeErreur(reponse.getCodeErreur())
                .messageErreur(reponse.getMessage())
                .dateEnvoi(LocalDateTime.now())
                .tentative(tentative)
                .reponseApi(reponse.getDetails())
                .tempsExecutionMs(tempsExecutionMs)
                .codeProduitPrincipal(codeProduit)
                .build();

        HistoriqueEnvoi saved = historiqueRepository.save(historique);
        log.info("Historique sauvegardé avec l'ID: {}", saved.getId());

        return saved;
    }
}