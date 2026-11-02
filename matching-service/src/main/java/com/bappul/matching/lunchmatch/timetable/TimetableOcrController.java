package com.bappul.matching.lunchmatch.timetable;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ocr")
public class TimetableOcrController {

    private final TimetableOcrService timetableOcrService;

    @PostMapping(value = "/timetable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TimetableItemDto>> parseTimetable(
            @RequestParam("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new RuntimeException("이미지 파일이 필요합니다.");
        }
        return ResponseEntity.ok(timetableOcrService.processImage(image));
    }
}