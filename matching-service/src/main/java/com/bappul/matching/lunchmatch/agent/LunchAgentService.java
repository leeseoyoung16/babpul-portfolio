package com.bappul.matching.lunchmatch.agent;

import com.bappul.matching.lunchmatch.match.MatchQueue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LunchAgentService {

    private final LunchMatchTool lunchMatchTool;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final AgentHistoryService agentHistoryService;

    public String chat(String userMessage, Long userId) {
        agentHistoryService.markLoopStart(userId);
        try {
            List<Map<String, Object>> contents = agentHistoryService.getOrCreate(userId);
            contents.add(userContent(userMessage));
            return runAgentLoop(contents, userId);
        } catch (Exception e) {
            log.error("LunchAgentService error", e);
            return "오류가 발생했어요: " + e.getMessage();
        } finally {
            agentHistoryService.markLoopEnd(userId);
        }
    }

    public void clearHistory(Long userId) {
        agentHistoryService.clear(userId);
    }

    private String runAgentLoop(List<Map<String, Object>> contents, Long userId) throws Exception {
        for (int turn = 0; turn < 5; turn++) {
            Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction(),
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

                    log.info("LunchTool call: {}", funcName);

                    String toolResult;
                    try {
                        toolResult = executeTool(funcName, args, userId);
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

            if (!hasFunctionCall) {
                return parts.get(0).path("text").asText("응답을 생성하지 못했어요.");
            }

            contents.add(Map.of("role", "user", "parts", toolResultParts));
        }

        return "처리 한도를 초과했어요. 다시 시도해 주세요.";
    }

    private String executeTool(String funcName, JsonNode args, Long userId) throws Exception {
        return switch (funcName) {
            case "getTodayFreeTimes" -> lunchMatchTool.getTodayFreeTimes(userId);
            case "enqueue" -> {
                List<MatchQueue.FreeTimeSlot> slots = new ArrayList<>();
                for (JsonNode slot : args.get("slots")) {
                    slots.add(new MatchQueue.FreeTimeSlot(
                            slot.get("startTime").asText(),
                            slot.get("endTime").asText()));
                }
                yield lunchMatchTool.enqueue(userId, slots);
            }
            case "getQueueStatus" -> lunchMatchTool.getQueueStatus(userId);
            case "getProposal"    -> lunchMatchTool.getProposal(args.get("proposalId").asText());
            case "respond"        -> lunchMatchTool.respond(userId,
                                        args.get("proposalId").asText(),
                                        args.get("accept").asBoolean());
            case "cancelQueue"    -> lunchMatchTool.cancelQueue(userId);
            default               -> "{\"error\": \"알 수 없는 함수: " + funcName + "\"}";
        };
    }

    private Map<String, Object> systemInstruction() {
        String text = """
            당신은 대학교 혼밥 매칭 서비스 AI 어시스턴트입니다.
            1. 사용자가 혼밥 메이트를 구하고 싶다고 하면 먼저 getTodayFreeTimes로 오늘 공강시간을 확인하세요.
            2. 공강시간을 안내하고 원하는 시간대를 물어보세요.
            3. 시간대가 정해지면 enqueue로 매칭 대기열에 투입하세요.
            4. 매칭 제안이 오면 익명으로 가능한 시간만 보여주고 수락 여부를 물어보세요.
            5. 수락/거절 시 제안 ID를 히스토리에서 직접 사용하세요. 사용자에게 제안 ID를 묻거나 노출하지 마세요.
            6. 매칭 상대의 개인정보는 절대 노출하지 마세요.
            7. 항상 친근하고 밝은 톤으로 한국어로 답변합니다.
            8. 매칭 관련 이외의 질문은 거절하세요.
            """;
        return Map.of("parts", List.of(Map.of("text", text)));
    }

    private List<Map<String, Object>> toolDeclarations() {
        return List.of(
            toolDef("getTodayFreeTimes", "오늘 나의 공강시간 목록을 조회합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),

            toolDef("enqueue", "선택한 공강시간으로 혼밥 매칭 대기열에 투입합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "slots", Map.of(
                               "type", "array",
                               "description", "매칭 희망 시간대 목록",
                               "items", Map.of(
                                   "type", "object",
                                   "properties", Map.of(
                                       "startTime", prop("string", "시작 시각 HH:mm"),
                                       "endTime",   prop("string", "종료 시각 HH:mm"))))),
                       "required", List.of("slots"))),

            toolDef("getQueueStatus", "현재 매칭 대기 상태를 조회합니다.",
                Map.of("type", "object", "properties", Map.of(), "required", List.of())),

            toolDef("getProposal", "매칭 제안 내용을 조회합니다. (익명)",
                Map.of("type", "object",
                       "properties", Map.of("proposalId", prop("string", "제안 ID")),
                       "required", List.of("proposalId"))),

            toolDef("respond", "매칭 제안을 수락하거나 거절합니다.",
                Map.of("type", "object",
                       "properties", Map.of(
                           "proposalId", prop("string", "제안 ID"),
                           "accept",     Map.of("type", "boolean", "description", "수락 여부")),
                       "required", List.of("proposalId", "accept"))),

            toolDef("cancelQueue", "매칭 대기를 취소합니다.",
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