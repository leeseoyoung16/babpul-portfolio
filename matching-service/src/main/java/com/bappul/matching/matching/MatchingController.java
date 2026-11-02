package com.bappul.matching.matching;

import com.bappul.matching.matching.client.UserServiceClient;
import com.bappul.matching.matching.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/match")
public class MatchingController {

    private final MatchingService matchService;
    private final ObjectMapper om;
    private final UserServiceClient userServiceClient;

    public MatchingController(
            MatchingService matchService,
            ObjectMapper om,
            UserServiceClient userServiceClient
    ) {
        this.matchService = matchService;
        this.om = om;
        this.userServiceClient = userServiceClient;
    }

    @PostMapping("/recommend")
    public ResponseEntity<AiMatchResponse> recommend(
            @RequestBody UserDto user,
            @RequestParam(defaultValue = "1") int topK,
            Principal principal
    ) throws Exception {

        // 1. 현재 사용자 내부 ID (JWT는 Gateway에서 검증, principal에서 externalId 추출)
        String currentUserIdString = principal.getName();
        Long currentUserId = userServiceClient.findInternalIdByExternalId(currentUserIdString);
        UserInfoDto userInfo = userServiceClient.getUserInfo(currentUserId);

        // 2. 성별 변환
        String originalGender = Optional.ofNullable(userInfo.getGender()).orElse("").toUpperCase();
        String finalGender = switch (originalGender) {
            case "MALE" -> "M";
            case "FEMALE" -> "F";
            default -> originalGender;
        };

        // 3. 외부 AI ID = "U" + 내부 ID
        String requesterAiId = "U" + currentUserIdString;

        // 4. AI 서버에 보낼 UserDto 구성
        UserDto aiReadyUser = new UserDto(
                requesterAiId,
                user.start_time(),
                user.end_time(),
                user.menu_pref(),
                user.duration_min(),
                String.valueOf(userInfo.getGrade()),
                userInfo.getMajor(),
                finalGender,
                user.wait_min(),
                user.venue()
        );

        // 5. 즉시 실행 + 대기열 등록 + AI 추천
        RecommendResponse res = matchService.runAiRecommendation(aiReadyUser, topK);

        // 6. 추천 결과 읽기
        List<RecommendMatchDto> matches = res.top_k();

        if (matches != null && !matches.isEmpty()) {
            RecommendMatchDto bestMatch = matches.get(0);
            if (bestMatch != null && bestMatch.user_id() != null) {
                String matchedExternal = bestMatch.user_id();
                String matchedTrimmed = matchedExternal.startsWith("U")
                        ? matchedExternal.substring(1) : matchedExternal;

                Long matchedInternalId = userServiceClient.findInternalIdByExternalId(matchedTrimmed);

                if (matchedInternalId != null && !matchedInternalId.equals(currentUserId)) {
                    ChatRoomResponse chatRoom = matchService.processSuccessfulMatch(
                            currentUserId, matchedInternalId, requesterAiId, matchedExternal);

                    return ResponseEntity.ok(new AiMatchResponse(true, chatRoom.getId()));
                }
            }
        }

        return ResponseEntity.ok(new AiMatchResponse(false, null));
    }
}
