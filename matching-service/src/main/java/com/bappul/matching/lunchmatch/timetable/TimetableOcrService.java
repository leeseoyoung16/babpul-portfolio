package com.bappul.matching.lunchmatch.timetable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableOcrService {

    private final TimetableOcrEngine ocrEngine;

    public List<TimetableItemDto> processImage(MultipartFile image) {
        try {
            BufferedImage bufferedImage = loadImage(image);
            return ocrEngine.parseTimetable(bufferedImage);
        } catch (Exception e) {
            throw new RuntimeException("OCR 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private BufferedImage loadImage(MultipartFile file) throws Exception {
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) throw new RuntimeException("지원하지 않는 이미지 형식입니다.");
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = rgb.createGraphics();
        g.setBackground(java.awt.Color.WHITE);
        g.clearRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rgb;
    }
}