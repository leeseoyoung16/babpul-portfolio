package com.babpul.matching.point;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/points")
public class InternalPointController {

    private final PointService pointService;

    @PostMapping("/earn")
    public ResponseEntity<Void> earnPoints(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        int amount = Integer.parseInt(body.get("amount").toString());
        String reason = (String) body.getOrDefault("reason", "결제 완료");
        pointService.earnPoints(userId, amount, reason);
        return ResponseEntity.ok().build();
    }
}
