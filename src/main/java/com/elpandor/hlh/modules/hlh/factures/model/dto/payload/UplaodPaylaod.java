package com.elpandor.hlh.modules.hlh.factures.model.dto.payload;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UplaodPaylaod {
    private Integer id;
    private MultipartFile file;
    private String typeFacture;
}
