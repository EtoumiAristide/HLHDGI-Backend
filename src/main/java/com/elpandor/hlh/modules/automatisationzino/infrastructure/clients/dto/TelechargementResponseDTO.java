package com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelechargementResponseDTO {
    private String nomFichier;
    private String url;
    private Long taille;
    private String status;
    private String message;
}