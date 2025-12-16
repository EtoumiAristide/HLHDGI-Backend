package com.elpandor.hlh.modules.hlh.utils;

import java.awt.image.BufferedImage;

public interface IOCRService {
    String extraireTexteDepuisPDF(byte[] pdfBytes);
    String extraireTexteDepuisImage(BufferedImage image);
}
