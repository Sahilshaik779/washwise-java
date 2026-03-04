package com.washwise.backend.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class QrCodeUtils {

    public String generateQr(String data, String filename) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // Generate a 300x300 QR Code
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);
            
            // Save it to a "qr_codes" folder in the root directory
            Path path = FileSystems.getDefault().getPath("qr_codes", filename);
            
            // Create the directory if it doesn't exist yet
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}