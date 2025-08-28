package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UplaodPaylaod {
    private Integer id;
    private MultipartFile file;
    private String typeFacture;
}
