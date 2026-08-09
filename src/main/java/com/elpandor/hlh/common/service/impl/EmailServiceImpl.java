package com.elpandor.hlh.common.service.impl;

import com.elpandor.hlh.common.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailSource;

    public void sendEmail(String to, String subject, String body) throws AddressException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailSource);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public boolean sendHtmlEmail(String to, String subject, String body) throws MessagingException {
        boolean ok = true;

        MimeMessage message = mailSender.createMimeMessage();

        message.setFrom(new InternetAddress(emailSource));
        message.setRecipients(MimeMessage.RecipientType.TO, to);
        message.setSubject(subject);

//        String htmlContent = "<h1>This is a test Spring Boot email</h1>" +
//                "<p>It can contain <strong>HTML</strong> content.</p>";
        message.setContent(body, "text/html; charset=utf-8");

        try {
            mailSender.send(message);
        } catch (Exception e) {

            ok = false;

            e.printStackTrace();

        }

        return ok;
    }

    public boolean sendEmailWithAttachment(String to, String subject, String body, String fileUrl) throws MessagingException, IOException {
        boolean ok = true;

        MimeMessage message = mailSender.createMimeMessage();
        message.setFrom(new InternetAddress(emailSource));
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        try {
            FileSystemResource file = new FileSystemResource(downloadFromURL(fileUrl, "filename.pdf"));
            helper.addAttachment(file.getFilename(), file);
        } catch (IOException e) {

            ok = false;

            e.printStackTrace();

        }

        mailSender.send(message);
        return ok;
    }

    private File downloadFromURL(String url, String resultFilename) throws IOException {
        InputStream in = new URL(url).openStream();
        Files.copy(in, Paths.get(resultFilename), StandardCopyOption.REPLACE_EXISTING);

        return ResourceUtils.getFile(resultFilename);
    }

    @Override
    public boolean sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String attachmentFilename) throws MessagingException {
        boolean ok = true;

        MimeMessage message = mailSender.createMimeMessage();
        message.setFrom(new InternetAddress(emailSource));
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        try {
            helper.addAttachment(attachmentFilename, new org.springframework.core.io.ByteArrayResource(attachment));
        } catch (Exception e) {
            ok = false;
            e.printStackTrace();
        }

        mailSender.send(message);
        return ok;
    }
}
