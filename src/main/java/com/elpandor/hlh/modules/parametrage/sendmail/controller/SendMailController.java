package com.elpandor.hlh.modules.parametrage.sendmail.controller;

import com.elpandor.hlh.common.service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/sendmail")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@AllArgsConstructor
@Slf4j
public class SendMailController {
    @Autowired
    EmailService emailService;

    @PostMapping()
    public ResponseEntity<Map<String, Object>> create() {
        Map<String, Object> response = new HashMap<>();

        boolean success = false;
        try {
            String body = "<html><head></head><body style=\"font-size:16px\">Test Envoi Mail RSE Eranove" +
                    "<br><br>" +
                    "Salutation , Ceci est un test" +
                    "<br>" +
                    "Cordialement." +
                    "</body>" +
                    "</html>";
            emailService.sendHtmlEmail("aetoumi@gmail.com", "Test envoi mail", body);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
