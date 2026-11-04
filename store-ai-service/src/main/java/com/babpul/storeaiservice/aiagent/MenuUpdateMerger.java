package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.dto.MenuDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/** Gemini가 일부 필드만 보낸 메뉴 수정 요청을 현재 메뉴 값과 합친다. */
final class MenuUpdateMerger {

    private static final int MIN_PRICE = 100;
    private static final int MIN_PREP_TIME = 1;

    private MenuUpdateMerger() {
    }

    /** @throws IllegalArgumentException 바꿀 값이 없거나 값이 올바르지 않은 경우 */
    static Map<String, Object> merge(JsonNode args, MenuDto current) {
        boolean hasName = args.hasNonNull("name") && !args.get("name").asText().isBlank();
        boolean hasPrice = args.hasNonNull("price");
        boolean hasPrepTime = args.hasNonNull("prepTime");

        if (!hasName && !hasPrice && !hasPrepTime) {
            throw new IllegalArgumentException("수정할 값이 없습니다. 사장님에게 무엇을 바꿀지 물어본 뒤 다시 호출하세요.");
        }

        int price = current.getPrice();
        if (hasPrice) {
            price = args.get("price").asInt();
            if (price < MIN_PRICE) {
                throw new IllegalArgumentException("가격은 최소 100원 이상이어야 합니다.");
            }
        }

        int prepTime = current.getPrepTime();
        if (hasPrepTime) {
            prepTime = args.get("prepTime").asInt();
            if (prepTime < MIN_PREP_TIME) {
                throw new IllegalArgumentException("준비 시간은 최소 1분 이상이어야 합니다.");
            }
        }

        return Map.of(
            "name", hasName ? args.get("name").asText().trim() : current.getName(),
            "price", price,
            "prepTime", prepTime
        );
    }
}
