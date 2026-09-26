package com.elpandor.hlh.modules.automatisationzino.rest;

import com.elpandor.hlh.modules.automatisationzino.application.dto.RapportExtractionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnvoyerRapportMailRequest {

    @NotNull
    @Valid
    private RapportExtractionRequest request;

    @NotBlank
    @Email
    private String destinataire;

    /** "excel" | "pdf" | "word" */
    @NotBlank
    private String format;
}
