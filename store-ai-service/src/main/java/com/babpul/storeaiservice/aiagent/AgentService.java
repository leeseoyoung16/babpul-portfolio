package com.babpul.storeaiservice.aiagent;

import com.babpul.storeaiservice.client.MenuClient;
import com.babpul.storeaiservice.client.StoreServiceClient;
import com.babpul.storeaiservice.client.dto.MenuDto;
import com.babpul.storeaiservice.client.dto.StoreNoticeDto;
import com.babpul.storeaiservice.client.dto.StoreScheduleDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final StoreServiceClient storeServiceClient;
    private final MenuClient menuClient;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, List<Map<String, Object>>> historyMap = new ConcurrentHashMap<>();

    public String chat(Long storeId, String userMessage, Long ownerId) {
        try {
            List<Map<String, Object>> contents = historyMap.computeIfAbsent(ownerId, k -> new ArrayList<>());
            contents.add(userContent(userMessage));
            return runAgentLoop(storeId, contents, ownerId);
        } catch (Exception e) {
            log.error("AgentService error", e);
            return "죄송해요, 처리 중 오류가 발생했어요: " + e.getMessage();
        }
    }

    public void clearHistory(Long userId) {
        historyMap.remove(userId);
    }

    private String runAgentLoop(Long storeId, List<Map<String, Object>> contents, Long ownerId) throws Exception {
        for (int turn = 0; turn < 5; turn++) {
            Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction(storeId),
                "tools",             List.of(Map.of("function_declarations", toolDeclarations())),
                "contents",          contents
            );

            String responseJson = geminiClient.send(requestBody);
            JsonNode root      = objectMapper.readTree(responseJson);
            JsonNode candidate = root.path("candidates").get(0).path("content");
            JsonNode parts     = candidate.path("parts");

            contents.add(Map.of("role", "model", "parts", objectMapper.convertValue(parts, List.class)));

            boolean hasFunctionCall = false;
            List<Map<String, Object>> toolResultParts = new ArrayList<>();

            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    hasFunctionCall = true;
                    JsonNode fc       = part.get("functionCall");
                    String   funcName = fc.get("name").asText();
                    JsonNode args     = fc.get("args");

                    log.info("Tool call: {}", funcName);
                    String toolResult;
                    try {
                        toolResult = executeTool(funcName, args, storeId, ownerId);
                    } catch (Exception e) {
                        toolResult = "{\"error\": \"" + e.getMessage() + "\"}";
                    }

                    toolResultParts.add(Map.of(
                        "functionResponse", Map.of(
                            "name", funcName,
                            "response", Map.of("result", toolResult)
                        )
                    ));
                }
            }

            if (!hasFunctionCall) return parts.get(0).path("text").asText("응답을 생성하지 못했어요.");
            contents.add(Map.of("role", "user", "parts", toolResultParts));
        }

        return "처리 한도를 초과했어요. 다시 시도해 주세요.";
    }

    private String executeTool(String funcName, JsonNode args, Long storeId, Long ownerId) throws Exception {
        return switch (funcName) {
            case "getMenus" -> objectMapper.writeValueAsString(menuClient.getMenusByStore(storeId));
            case "getMenu"  -> objectMapper.writeValueAsString(menuClient.getMenu(args.get("menuId").asLong()));
            case "createMenu" -> {
                Map<String, Object> body = Map.of(
                    "name", args.get("name").asText(),
                    "price", args.get("price").asInt(),
                    "prepTime", args.get("prepTime").asInt()
                );
                MenuDto menu = menuClient.createMenu(storeId, body, ownerId);
                yield objectMapper.writeValueAsString(menu) + "\n[NOTICE: 메뉴 사진은 앱에서 별도로 첨부할 수 있습니다.]";
            }
            case "updateMenu" -> {
                Long menuId = args.get("menuId").asLong();
                Map<String, Object> body = MenuUpdateMerger.merge(args, menuClient.getMenu(menuId));
                yield objectMapper.writeValueAsString(menuClient.updateMenu(menuId, body, ownerId));
            }
            case "disableMenu" -> {
                menuClient.disableMenu(args.get("menuId").asLong(), ownerId);
                yield "{\"result\": \"메뉴가 비활성화되었습니다.\"}";
            }
            case "setSoldOut" -> objectMapper.writeValueAsString(
                    menuClient.setSoldOut(args.get("menuId").asLong(), args.get("soldOut").asBoolean(), ownerId));
            case "getNotices" -> objectMapper.writeValueAsString(storeServiceClient.getNotices(storeId));
            case "getNotice"  -> {
                Long noticeId = args.get("noticeId").asLong();
                List<StoreNoticeDto> notices = storeServiceClient.getNotices(storeId);
                StoreNoticeDto found = notices.stream()
                        .filter(n -> n.getId().equals(noticeId)).findFirst().orElse(null);
                yield objectMapper.writeValueAsString(found);
            }
            case "createNotice" -> {
                Map<String, String> body = Map.of(
                    "title", args.get("title").asText(),
                    "content", args.get("content").asText(),
                    "scheduledAt", args.get("scheduledAt").asText(),
                    "expireAt", args.get("expireAt").asText()
                );
                yield objectMapper.writeValueAsString(storeServiceClient.createNotice(storeId, body, ownerId));
            }
            case "updateNotice" -> {
                Map<String, String> body = NoticeUpdateMerger.merge(args, storeServiceClient.getNotices(storeId));
                yield objectMapper.writeValueAsString(
                        storeServiceClient.updateNotice(args.get("noticeId").asLong(), body, ownerId));
            }
            case "deleteNotice" -> {
                storeServiceClient.deleteNotice(args.get("noticeId").asLong(), ownerId);
                yield "{\"result\": \"공지가 삭제되었습니다.\"}";
            }
            case "getSchedules" -> objectMapper.writeValueAsString(storeServiceClient.getSchedules(storeId));
            case "updateSchedule" -> {
                Map<String, Object> body = ScheduleUpdateMerger.merge(args, storeServiceClient.getSchedules(storeId));
                String dayOfWeek = (String) body.get("dayOfWeek");
                yield objectMapper.writeValueAsString(
                        storeServiceClient.updateSchedule(storeId, dayOfWeek, body, ownerId));
            }
            case "isStoreOpen" -> {
                Map<String, Boolean> result = storeServiceClient.isStoreOpen(storeId);
                yield "{\"open\": " + result.getOrDefault("open", false) + "}";
            }
            default -> "{\"error\": \"알 수 없는 함수: " + funcName + "\"}";
        };
    }

    private Map<String, Object> systemInstruction(Long storeId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String text = """
            당신은 가게 ID %d 번을 운영하는 사장님을 돕는 AI 어시스턴트입니다.
            현재 날짜/시간: %s
            1. 메뉴·공지·일정 관련 작업은 반드시 제공된 도구를 사용합니다.
            2. 모든 작업 시 storeId는 항상 %d 를 사용하며, 다른 storeId는 허용하지 않습니다.
            3. 날짜/시간은 ISO 형식(yyyy-MM-ddTHH:mm:ss)으로 전달합니다. 상대적 시간 표현("오늘", "지금", "내일" 등)은 현재 날짜/시간을 기준으로 계산합니다.
            4. 일반적인 질문에도 친절하게 답변합니다.
            5. 항상 한국어로 답변합니다.
            """.formatted(storeId, now, storeId);
        return Map.of("parts", List.of(Map.of("text", text)));
    }

    private List<Map<String, Object>> toolDeclarations() {
        return List.of(
            toolDef("getMenus", "가게의 전체 메뉴 목록을 조회합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),
            toolDef("getMenu", "단일 메뉴를 조회합니다.",
                Map.of("type", "object",
                       "properties", Map.of("menuId", prop("integer", "메뉴 ID")),
                       "required", List.of("menuId"))),
            toolDef("createMenu", "새 메뉴를 등록합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "name",     prop("string",  "메뉴 이름"),
                           "price",    prop("integer", "메뉴 가격 (원)"),
                           "prepTime", prop("integer", "준비 시간 (분)")),
                       "required", List.of("name", "price", "prepTime"))),
            toolDef("updateMenu", "기존 메뉴를 수정합니다. 바꿀 필드만 보내면 나머지는 기존 값을 유지합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "menuId",   prop("integer", "수정할 메뉴 ID"),
                           "name",     prop("string",  "새 메뉴 이름"),
                           "price",    prop("integer", "새 가격 (원)"),
                           "prepTime", prop("integer", "새 준비 시간 (분)")),
                       "required", List.of("menuId"))),
            toolDef("disableMenu", "메뉴를 비활성화합니다.",
                Map.of("type", "object",
                       "properties", Map.of("menuId", prop("integer", "비활성화할 메뉴 ID")),
                       "required", List.of("menuId"))),
            toolDef("setSoldOut", "메뉴의 품절 상태를 지정한 값으로 설정합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "menuId",  prop("integer", "메뉴 ID"),
                           "soldOut", prop("boolean", "품절 처리는 true, 판매 재개는 false")),
                       "required", List.of("menuId", "soldOut"))),
            toolDef("getNotices", "가게의 공지 목록을 조회합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),
            toolDef("getNotice", "단일 공지를 조회합니다.",
                Map.of("type", "object",
                       "properties", Map.of("noticeId", prop("integer", "공지 ID")),
                       "required", List.of("noticeId"))),
            toolDef("createNotice", "공지를 등록합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "title", prop("string", "공지 제목"),
                           "content", prop("string", "공지 내용"),
                           "scheduledAt", prop("string", "시작 시각 (yyyy-MM-ddTHH:mm:ss)"),
                           "expireAt",    prop("string", "종료 시각 (yyyy-MM-ddTHH:mm:ss)")),
                       "required", List.of("title", "content", "scheduledAt", "expireAt"))),
            toolDef("updateNotice", "공지를 수정합니다. 바꿀 필드만 보내면 나머지는 기존 값을 유지합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "noticeId", prop("integer", "수정할 공지 ID"),
                           "title", prop("string", "새 공지 제목"),
                           "content", prop("string", "새 공지 내용"),
                           "scheduledAt", prop("string", "새 시작 시각 (yyyy-MM-ddTHH:mm:ss)"),
                           "expireAt",    prop("string", "새 종료 시각 (yyyy-MM-ddTHH:mm:ss)")),
                       "required", List.of("noticeId"))),
            toolDef("deleteNotice", "공지를 삭제합니다.",
                Map.of("type", "object",
                       "properties", Map.of("noticeId", prop("integer", "삭제할 공지 ID")),
                       "required", List.of("noticeId"))),
            toolDef("getSchedules", "가게의 영업 일정 전체를 조회합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),
            toolDef("updateSchedule", "특정 요일의 영업 일정을 등록/수정합니다. 보내지 않은 필드는 기존 값을 유지합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "dayOfWeek", prop("string", "요일 (MONDAY~SUNDAY)"),
                           "openTime",  prop("string", "오픈 시각 (HH:mm)"),
                           "closeTime", prop("string", "마감 시각 (HH:mm)"),
                           "closed",    Map.of("type", "boolean", "description", "휴무 여부. 휴무일을 영업일로 바꿀 때는 false를 명시합니다.")),
                       "required", List.of("dayOfWeek"))),
            toolDef("isStoreOpen", "현재 가게가 영업 중인지 확인합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()))
        );
    }

    private Map<String, Object> toolDef(String name, String description, Map<String, Object> parameters) {
        return Map.of("name", name, "description", description, "parameters", parameters);
    }

    private Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private Map<String, Object> userContent(String text) {
        return Map.of("role", "user", "parts", List.of(Map.of("text", text)));
    }
}