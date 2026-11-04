package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Gemini가 일부 필드만 보낸 공지 수정 요청을 현재 공지 값과 합친다. */
final class NoticeUpdateMerger {

    private static final List<String> FIELDS = List.of("title", "content", "scheduledAt", "expireAt");

    private NoticeUpdateMerger() {
    }

    /** @throws IllegalArgumentException 공지를 찾을 수 없거나 바꿀 값이 없거나 값이 올바르지 않은 경우 */
    static Map<String, String> merge(JsonNode args, List<StoreNoticeDto> notices) {
        long noticeId = args.get("noticeId").asLong();
        StoreNoticeDto current = notices.stream()
                .filter(n -> n.getId() == noticeId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 가게에서 해당 공지를 찾을 수 없습니다."));

        if (FIELDS.stream().noneMatch(field -> hasText(args, field))) {
            throw new IllegalArgumentException("수정할 값이 없습니다. 사장님에게 무엇을 바꿀지 물어본 뒤 다시 호출하세요.");
        }

        LocalDateTime scheduledAt = hasText(args, "scheduledAt")
                ? LocalDateTime.parse(args.get("scheduledAt").asText().trim())
                : current.getScheduledAt();
        LocalDateTime expireAt = hasText(args, "expireAt")
                ? LocalDateTime.parse(args.get("expireAt").asText().trim())
                : current.getExpireAt();
        if (expireAt.isBefore(scheduledAt)) {
            throw new IllegalArgumentException("공지 종료 시각은 시작 시각 이후여야 합니다.");
        }

        return Map.of(
            "title", hasText(args, "title") ? args.get("title").asText().trim() : current.getTitle(),
            "content", hasText(args, "content") ? args.get("content").asText().trim() : current.getContent(),
            "scheduledAt", scheduledAt.toString(),
            "expireAt", expireAt.toString()
        );
    }

    private static boolean hasText(JsonNode args, String field) {
        return args.hasNonNull(field) && !args.get(field).asText().isBlank();
    }
}
