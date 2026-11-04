package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.dto.StoreScheduleDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Gemini가 일부 필드만 보낸 일정 수정 요청을 해당 요일의 기존 일정과 합친다. */
final class ScheduleUpdateMerger {

    private static final String NO_HOURS = "00:00";

    private ScheduleUpdateMerger() {
    }

    /** @throws IllegalArgumentException 영업 시간을 알 수 없어 사장님에게 다시 물어봐야 하는 경우 */
    static Map<String, Object> merge(JsonNode args, List<StoreScheduleDto> schedules) {
        DayOfWeek day = DayOfWeek.valueOf(args.get("dayOfWeek").asText().trim().toUpperCase(Locale.ROOT));
        StoreScheduleDto current = schedules.stream()
                .filter(s -> s.getDayOfWeek() == day)
                .findFirst()
                .orElse(null);

        boolean closed = args.hasNonNull("closed")
                ? args.get("closed").asBoolean()
                : current != null && current.isClosed();

        boolean hasKnownHours = current != null && !current.getOpenTime().equals(current.getCloseTime());
        String openTime = text(args, "openTime", hasKnownHours ? current.getOpenTime() : null);
        String closeTime = text(args, "closeTime", hasKnownHours ? current.getCloseTime() : null);

        if (closed && openTime == null && closeTime == null) {
            openTime = NO_HOURS;
            closeTime = NO_HOURS;
        }
        if (openTime == null || closeTime == null) {
            throw new IllegalArgumentException(
                    "저장된 영업 시간이 없습니다. 사장님에게 오픈 시각과 마감 시각을 물어본 뒤 다시 호출하세요.");
        }
        if (!closed && openTime.equals(closeTime)) {
            throw new IllegalArgumentException(
                    "오픈 시각과 마감 시각이 같습니다. 사장님에게 영업 시간을 다시 확인한 뒤 호출하세요.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("dayOfWeek", day.name());
        body.put("openTime", openTime);
        body.put("closeTime", closeTime);
        body.put("closed", closed);
        return body;
    }

    private static String text(JsonNode args, String field, String fallback) {
        if (!args.hasNonNull(field) || args.get(field).asText().isBlank()) {
            return fallback;
        }
        return args.get(field).asText().trim();
    }
}
