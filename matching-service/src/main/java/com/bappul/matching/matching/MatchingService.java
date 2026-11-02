package com.bappul.matching.matching;

import com.bappul.matching.matching.client.AiClient;
import com.bappul.matching.matching.client.ChatServiceClient;
import com.bappul.matching.matching.client.UserServiceClient;
import com.bappul.matching.matching.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class MatchingService {

    private final AttemptRepository attempts;
    private final AiClient ai;
    private final ObjectMapper om;
    private final ChatServiceClient chatServiceClient;
    private final UserServiceClient userServiceClient;

    public MatchingService(
            AttemptRepository attempts,
            AiClient ai,
            ObjectMapper om,
            ChatServiceClient chatServiceClient,
            UserServiceClient userServiceClient
    ) {
        this.attempts = attempts;
        this.ai = ai;
        this.om = om;
        this.chatServiceClient = chatServiceClient;
        this.userServiceClient = userServiceClient;
    }

    // ⭐ AI 추천 + 즉시 매칭
    @Transactional
    public RecommendResponse runAiRecommendation(UserDto u, int topK) {
        try {
            String originalGender = Optional.ofNullable(u.gender()).orElse("").toUpperCase();
            String aiGender = switch (originalGender) {
                case "MALE" -> "M";
                case "FEMALE" -> "F";
                default -> originalGender;
            };

            UserDto dbReadyUser = new UserDto(
                    u.user_id(), u.start_time(), u.end_time(), u.menu_pref(),
                    u.duration_min(), u.grade(), u.department(), aiGender,
                    u.wait_min(), u.venue()
            );

            var prefs = om.createObjectNode();
            prefs.put("dept", dbReadyUser.department());
            prefs.put("grade", dbReadyUser.grade());
            prefs.put("gender", dbReadyUser.gender());
            prefs.put("wait_min", dbReadyUser.wait_min());
            prefs.put("duration_min", dbReadyUser.duration_min());
            if (dbReadyUser.venue() != null) prefs.put("venue", dbReadyUser.venue());
            if (dbReadyUser.menu_pref() != null) {
                var arr = om.createArrayNode();
                dbReadyUser.menu_pref().forEach(arr::add);
                prefs.set("menu_pref", arr);
            }

            enqueue(dbReadyUser, om.writeValueAsString(prefs), 5);

            RecommendResponse res = recommend(dbReadyUser, topK);
            List<RecommendMatchDto> topKMatches = res.top_k();

            if (topKMatches != null && !topKMatches.isEmpty()) {
                RecommendMatchDto bestMatch = topKMatches.get(0);
                if (bestMatch != null && bestMatch.user_id() != null) {
                    String matchedExternalIdWithUPrefix = bestMatch.user_id();
                    String matchedExternalIdString = matchedExternalIdWithUPrefix.startsWith("U")
                            ? matchedExternalIdWithUPrefix.substring(1)
                            : matchedExternalIdWithUPrefix;

                    String requesterExternalId = dbReadyUser.user_id();
                    String requesterIdWithoutU = requesterExternalId.startsWith("U")
                            ? requesterExternalId.substring(1)
                            : requesterExternalId;

                    Long requesterInternalId = userServiceClient.findInternalIdByExternalId(requesterIdWithoutU);
                    Long matchedInternalId = userServiceClient.findInternalIdByExternalId(matchedExternalIdString);

                    if (matchedInternalId != null && requesterInternalId != null
                            && !requesterInternalId.equals(matchedInternalId)) {
                        processSuccessfulMatch(
                                requesterInternalId, matchedInternalId,
                                requesterExternalId, matchedExternalIdWithUPrefix
                        );
                        System.out.println("[IMMEDIATE MATCH SUCCESS] " + requesterExternalId
                                + " matched with " + matchedExternalIdWithUPrefix);
                    }
                }
            }

            return res;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AI 추천 과정에서 오류가 발생했습니다.");
        }
    }

    // ⭐ 대기열 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Attempt enqueue(UserDto u, String prefsJson, int ttlMinutes) {
        Attempt a = new Attempt();
        a.setUserId(u.user_id());
        a.setPrefsJson(prefsJson);

        try {
            if (u.start_time() == null || u.end_time() == null)
                throw new IllegalArgumentException("start_time / end_time 누락");
            a.setWindowStart(LocalTime.parse(u.start_time()));
            a.setWindowEnd(LocalTime.parse(u.end_time()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("시간 파싱 오류: " + e.getMessage());
        }

        a.setStatus(Attempt.Status.PENDING);
        a.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return attempts.save(a);
    }

    // ⭐ AI 서버 호출
    public RecommendResponse recommend(UserDto u, int topK) {
        ObjectNode userNode = om.valueToTree(u);
        var req = om.createObjectNode();
        req.set("new_user", userNode);
        req.put("top_k", topK);
        return ai.recommendJsonNode(req);
    }

    // ⭐ Attempt → UserDto 변환
    private UserDto reconstructUserDto(Attempt attempt) throws Exception {
        ObjectNode prefsNode = (ObjectNode) om.readTree(attempt.getPrefsJson());

        List<String> menuPrefList = null;
        if (prefsNode.has("menu_pref") && prefsNode.get("menu_pref").isArray()) {
            menuPrefList = om.convertValue(
                    prefsNode.get("menu_pref"),
                    om.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        }

        return new UserDto(
                attempt.getUserId(),
                attempt.getWindowStart().toString(),
                attempt.getWindowEnd().toString(),
                menuPrefList,
                prefsNode.has("duration_min") ? prefsNode.get("duration_min").asInt() : 30,
                prefsNode.get("grade").asText(),
                prefsNode.get("dept").asText(),
                prefsNode.get("gender").asText(),
                prefsNode.has("wait_min") ? prefsNode.get("wait_min").asDouble() : 5.0,
                prefsNode.has("venue") ? prefsNode.get("venue").asText() : null
        );
    }

    // ⭐ 스케줄러 매칭
    public void findAndMatchPendingUsers() {
        LocalDateTime now = LocalDateTime.now();
        List<Attempt> active = attempts.findByStatusAndExpiresAtAfter(Attempt.Status.PENDING, now);

        if (active.isEmpty()) return;

        for (Attempt attempt : active) {
            try {
                if (attempt.getStatus() != Attempt.Status.PENDING) continue;

                UserDto requesterDto = reconstructUserDto(attempt);
                RecommendResponse res = recommend(requesterDto, 1);
                List<RecommendMatchDto> topK = res.top_k();

                if (topK == null || topK.isEmpty()) continue;

                RecommendMatchDto bestMatch = topK.get(0);
                if (bestMatch == null || bestMatch.user_id() == null) continue;

                String matchedExternal = bestMatch.user_id();
                String matchedExternalTrimmed = matchedExternal.startsWith("U")
                        ? matchedExternal.substring(1) : matchedExternal;

                String requesterExternal = requesterDto.user_id();
                String requesterTrimmed = requesterExternal.startsWith("U")
                        ? requesterExternal.substring(1) : requesterExternal;

                Long requesterId = userServiceClient.findInternalIdByExternalId(requesterTrimmed);
                Long matchedId = userServiceClient.findInternalIdByExternalId(matchedExternalTrimmed);

                if (matchedId != null && requesterId != null && !requesterId.equals(matchedId)) {
                    processSuccessfulMatch(requesterId, matchedId, requesterExternal, matchedExternal);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<Attempt> findLatestPendingAttempt(String userId) {
        return attempts.findTop1ByUserIdAndStatusOrderByCreatedAtDesc(userId, Attempt.Status.PENDING);
    }

    // ⭐ 매칭 완료 처리
    public ChatRoomResponse processSuccessfulMatch(Long userAId, Long userBId, String aiIdA, String aiIdB) {
        ChatRoomResponse chatRoom = chatServiceClient.getOrCreateChatRoom(userAId, userBId);

        markUserAsMatched(aiIdA);
        markUserAsMatched(aiIdB);

        System.out.println("[MATCHING SUCCESS] ChatRoom=" + chatRoom.getId()
                + ", Users=" + userAId + ", " + userBId);

        return chatRoom;
    }

    // ⭐ Attempt 상태 MATCHED로 변경
    @Transactional
    public void markUserAsMatched(String userId) {
        Optional<Attempt> latest = attempts.findTop1ByUserIdAndStatusOrderByCreatedAtDesc(
                userId, Attempt.Status.PENDING);

        if (latest.isPresent()) {
            Attempt attempt = latest.get();
            attempt.setStatus(Attempt.Status.MATCHED);
            attempts.save(attempt);
            System.out.println("[MATCHING] marked attempt " + attempt.getId() + " (user=" + userId + ") MATCHED");
        } else {
            System.err.println("[MATCHING ERROR] No PENDING attempt for " + userId);
        }
    }

    // ⭐ 만료 시도 정리
    @Transactional
    public void cleanupExpiredAttempts() {
        LocalDateTime now = LocalDateTime.now();
        List<Attempt> expired = attempts.findByStatusAndExpiresAtBefore(Attempt.Status.PENDING, now);

        if (!expired.isEmpty()) {
            expired.forEach(a -> a.setStatus(Attempt.Status.EXPIRED));
            attempts.saveAll(expired);
            attempts.flush();
            System.out.println("[CLEANUP] expired " + expired.size() + " attempts.");
        }
    }
}
