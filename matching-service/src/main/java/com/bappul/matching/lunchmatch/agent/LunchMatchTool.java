package com.bappul.matching.lunchmatch.agent;

import com.bappul.matching.client.UserServiceClient;
import com.bappul.matching.client.dto.UserInfoDto;
import com.bappul.matching.lunchmatch.match.*;
import com.bappul.matching.lunchmatch.timetable.FreeTimeResponseDto;
import com.bappul.matching.lunchmatch.timetable.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LunchMatchTool {

    private final MatchService matchService;
    private final TimetableService timetableService;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    public String getTodayFreeTimes(Long userId) throws Exception {
        if (!timetableService.hasTimetable(userId)) {
            return "{\"status\": \"NO_TIMETABLE\", \"message\": \"아직 시간표가 등록되지 않았습니다.\"}";
        }

        List<FreeTimeResponseDto> freeTimes = matchService.getTodayFreeTimes(userId)
                .stream().map(FreeTimeResponseDto::new).toList();

        if (freeTimes.isEmpty()) {
            return "{\"status\": \"NO_FREE_TIME\", \"message\": \"오늘은 공강시간이 없네요.\"}";
        }

        return objectMapper.writeValueAsString(freeTimes);
    }

    public String enqueue(Long userId, List<MatchQueue.FreeTimeSlot> slots) throws Exception {
        UserInfoDto userInfo = userServiceClient.getUserById(userId);
        MatchEnqueueResult result = matchService.enqueue(userId, userInfo, slots);
        return objectMapper.writeValueAsString(result);
    }

    public String getQueueStatus(Long userId) throws Exception {
        MatchQueue queue = matchService.getQueueStatus(userId);
        if (queue == null) return "{\"message\": \"현재 매칭 대기 중이 아닙니다.\"}";
        return objectMapper.writeValueAsString(queue);
    }

    public String getProposal(String proposalId) throws Exception {
        MatchProposal proposal = matchService.getProposal(proposalId);
        if (proposal == null) return "{\"message\": \"매칭 제안이 만료되었거나 존재하지 않습니다.\"}";
        return objectMapper.writeValueAsString(new AnonymousProposalDto(proposal));
    }

    public String respond(Long userId, String proposalId, boolean accept) throws Exception {
        MatchProposalResult result = matchService.respond(userId, proposalId, accept);
        return objectMapper.writeValueAsString(result);
    }

    public String cancelQueue(Long userId) {
        matchService.cancelQueue(userId);
        return "{\"message\": \"매칭 대기를 취소했습니다.\"}";
    }

    record AnonymousProposalDto(String proposalId, String matchedDate,
                                String matchedStartTime, String matchedEndTime,
                                String status) {
        AnonymousProposalDto(MatchProposal p) {
            this(p.getId(), p.getMatchedDate(), p.getMatchedStartTime(), p.getMatchedEndTime(), p.getStatus().name());
        }
    }
}