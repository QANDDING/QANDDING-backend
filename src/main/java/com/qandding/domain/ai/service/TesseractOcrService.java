package com.qandding.domain.ai.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Service
public class TesseractOcrService {

    private final String tessdataPath;

    public TesseractOcrService(@Value("${tesseract.data-path}") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public String extractTextFromImage(MultipartFile imageFile) throws IOException, TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("kor+eng"); // 한글과 영어 동시 인식

        try (InputStream inputStream = imageFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Could not read image file");
            }
            return tesseract.doOCR(bufferedImage);
        }
    }
}
