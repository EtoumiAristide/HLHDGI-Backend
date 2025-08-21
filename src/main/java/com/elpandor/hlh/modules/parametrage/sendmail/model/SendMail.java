package com.elpandor.hlh.modules.parametrage.sendmail.model;

import com.elpandor.hlh.common.entities.AuditModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendMail extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sendmailID;

    private String email;
    @Column(name = "data_to_send", columnDefinition = "TEXT DEFAULT NULL")
    private String dataToSend;
    @Column(name = "erreur", columnDefinition = "TEXT DEFAULT NULL")
    private String erreur;
    private boolean statut;

}
