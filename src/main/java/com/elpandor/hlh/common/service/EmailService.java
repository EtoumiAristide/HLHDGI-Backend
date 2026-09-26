package com.elpandor.hlh.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;

import java.io.IOException;

public interface EmailService {
    public void sendEmail(String to, String subject, String body) throws AddressException;

    public boolean sendHtmlEmail(String to, String subject, String body) throws MessagingException;

    public boolean sendEmailWithAttachment(String to, String subject, String body, String fileUrl) throws MessagingException, IOException;

    /**
     * Envoie un email avec une pièce jointe fournie directement en mémoire (pas de téléchargement
     * intermédiaire depuis une URL). Utile pour joindre un document généré à la volée (rapport, export...).
     */
    public boolean sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String attachmentFilename) throws MessagingException;
}
