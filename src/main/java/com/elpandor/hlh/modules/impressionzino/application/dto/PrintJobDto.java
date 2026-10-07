package com.elpandor.hlh.modules.impressionzino.application.dto;

import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintJobEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Job d'impression tel que livré à l'agent desktop (le payload est le JSON du ticket, non échappé).
 */
public record PrintJobDto(
        String jobId,
        String numeroFacture,
        String typeDocument,
        int tentative,
        String dateCreation,
        JsonNode payload) {

    public static PrintJobDto from(PrintJobEntity job, ObjectMapper mapper) {
        JsonNode payload;
        try {
            payload = mapper.readTree(job.getPayloadJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload illisible pour le job d'impression " + job.getId(), e);
        }
        return new PrintJobDto(
                job.getId().toString(),
                job.getNumeroFacture(),
                job.getTypeDocument(),
                job.getTentatives(),
                job.getDateCreation() != null ? job.getDateCreation().toString() : null,
                payload);
    }
}
