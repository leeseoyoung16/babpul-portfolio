package com.babpul.matching.lunchmatch.timetable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableOcrService {

    private final TimetableOcrEngine ocrEngine;

    public List<TimetableItemDto> processImage(MultipartFile image) {
        try {
            BufferedImage bufferedImage = loadImage(image);
            return ocrEngine.parseTimetable(bufferedImage);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private BufferedImage loadImage(MultipartFile file) throws Exception {
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.");
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;

        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB); // OCR 입력 표준화
        java.awt.Graphics2D g = rgb.createGraphics();
        g.setBackground(java.awt.Color.WHITE);
        g.clearRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rgb;
    }
}