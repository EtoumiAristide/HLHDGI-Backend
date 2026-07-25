package com.elpandor.hlh.modules.hlh.model.dto;

public record FneResponse(
        String ncc, String reference, String token, boolean warning,
        InvoiceDto invoice
) {}
