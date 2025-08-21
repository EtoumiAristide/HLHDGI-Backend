package com.elpandor.hlh.modules.hlh.factures.utils;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponseData {
    private int code;
    private String message;
    private String detail;
    private List<String> erreurs;
    private LocalDateTime timestamp;
}
