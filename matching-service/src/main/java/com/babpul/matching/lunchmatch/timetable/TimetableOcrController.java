package com.babpul.matching.lunchmatch.timetable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
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
            log.warn("시간표 OCR 요청에 이미지 파일이 없음");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 필요합니다.");
        }
        return ResponseEntity.ok(timetableOcrService.processImage(image));
    }
}