package com.elpandor.hlh.common.utils;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.NonNull;

import java.io.*;
import java.util.Arrays;

public class InputStreamMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public InputStreamMultipartFile(InputStream inputStream, String name,
                                    String originalFilename, String contentType) throws IOException {
        this.content = inputStream!=null ? inputStream.readAllBytes(): new byte[0]; // lu une seule fois à la construction
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }
}
