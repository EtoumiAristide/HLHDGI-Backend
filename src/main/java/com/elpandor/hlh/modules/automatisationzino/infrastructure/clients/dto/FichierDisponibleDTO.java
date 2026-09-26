package com.elpandor.hlh.modules.automatisationzino.infrastructure.clients.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichierDisponibleDTO {

    @JsonProperty("filename")
    private String nomFichier;

    @JsonProperty("filetype")
    private String typeFichier;

    @JsonProperty("fileSize")
    private String taille;

    // Méthode utilitaire pour obtenir la taille en Long
    public Long getTailleEnLong() {
        try {
            return Long.parseLong(taille);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}