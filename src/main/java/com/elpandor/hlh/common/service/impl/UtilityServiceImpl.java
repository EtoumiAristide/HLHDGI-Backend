package com.elpandor.hlh.common.service.impl;

import com.elpandor.hlh.common.service.EmailService;
import com.elpandor.hlh.common.service.UtiliityService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.service.CompteUtilisateurService;
import com.elpandor.hlh.modules.parametrage.sendmail.model.dto.SendMailDTO;
import com.elpandor.hlh.modules.parametrage.sendmail.service.SendMailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@Slf4j
public class UtilityServiceImpl implements UtiliityService {
    @Autowired
    private CompteUtilisateurService compteUtilisateurService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SendMailService sendMailService;

    @Value("${frontend.app.url}")
    private String frontendURL;

    @Async
    @Override
    public void sendMailToNewUser(CompteUtilisateurDto compteUtilisateurDto) throws ParseException {
        log.trace("Starting processing of sendMailToNewUser");
        String body = "<html><head></head><body style=\"font-size:16px\"><p style=\"color:orange; font-weight:bold\">Notification de création de votre compte</p>" +
                "<br><br>" +
                "Chers collaborateur, <br><br> " +
                "Un compte a été créé pour vous sur la plateforme d'interfaçage FNE.<br><br>" +
                "Nous vous invitons à vous connecter via le lien <a href=\"" + frontendURL + "\">" + frontendURL + "</a>.<br><br>" +
                "<div style=\"border-left-width:5px; border-left-style:solid; border-left-color:#08436D; padding-left:10px\">" +
                "Nom d'utilisateur ou login: <b>" + compteUtilisateurDto.getLogin() + "</b>" +
                "</div><br>" +
                "Votre mot de passe vous sera communiqué via un autre canal.<br>" +
                "Vous serez invité à changer votre mot de passe lors de votre première connexion.<br><br>" +
                "N’hésitez pas à nous contacter si vous rencontrez des difficultés d’accès à la plateforme.<br><br>" +
                "Bien à vous." +
                "<br><br><br><br><br>" +
                "</body>" +
                "</html>";

        SendMailDTO sendMailDTO = new SendMailDTO();
        sendMailDTO.setEmail(compteUtilisateurDto.getEmail());
        sendMailDTO.setDataToSend(body);

        Boolean statut = false;
        try {
            statut = emailService.sendHtmlEmail(compteUtilisateurDto.getEmail(), "Création de votre compte sur la plateforme Gestion Parc-Auto", body);

        } catch (MessagingException e) {
            sendMailDTO.setErreur(e.getLocalizedMessage());
            log.error(e.getMessage(), e);
            //e.printStackTrace();
        }
        sendMailDTO.setStatut(statut);

        sendMailService.create(sendMailDTO);//Log dans la BD pour les mails échoués
    }
}
