package com.elpandor.hlh.modules.parametrage.sendmail.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendMailDTO {
    private Long sendmailID;
    private String email;
    private String dataToSend;
    private String erreur;
    private boolean statut;
}
