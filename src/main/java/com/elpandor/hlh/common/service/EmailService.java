package com.elpandor.hlh.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;

import java.io.IOException;

public interface EmailService {
    public void sendEmail(String to, String subject, String body) throws AddressException;

    public boolean sendHtmlEmail(String to, String subject, String body) throws MessagingException;

    public boolean sendEmailWithAttachment(String to, String subject, String body, String fileUrl) throws MessagingException, IOException;
}
