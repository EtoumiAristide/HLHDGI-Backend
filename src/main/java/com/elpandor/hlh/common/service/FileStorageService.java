package com.elpandor.hlh.common.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    String storeFile(MultipartFile file, String organistionAbbrev);

    Resource loadFileAsResource(String fileName);

}
