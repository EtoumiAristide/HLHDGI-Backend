package com.elpandor.hlh.common.service;

import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import org.springframework.scheduling.annotation.Async;

import java.text.ParseException;

public interface UtiliityService {
    @Async public void sendMailToNewUser(CompteUtilisateurDto compteUtilisateurDto) throws ParseException;
}
