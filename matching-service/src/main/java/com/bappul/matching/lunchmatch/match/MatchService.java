package com.bappul.matching.lunchmatch.match;

import com.bappul.matching.chat.ChatService;
import com.bappul.matching.client.UserServiceClient;
import com.bappul.matching.client.dto.FcmRequestDto;
import com.bappul.matching.client.dto.UserInfoDto;
import com.bappul.matching.lunchmatch.agent.AgentHistoryService;
import com.bappul.matching.lunchmatch.timetable.TimetableService;
import com.bappul.matching.lunchmatch.timetable.UserFreeTime;
import com.bappul.matching.point.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    @Lazy
    @Autowired
    private MatchService self;

    private final MatchQueueRepository matchQueueRepository;
    private final MatchProposalRepository matchProposalRepository;
    private final TimetableService timetableService;
    private final MatchSseService matchSseService;
    private final ChatService chatService;
    private final AgentHistoryService agentHistoryService;
    private final UserServiceClient userServiceClient;
    private final PointService pointService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public List<UserFreeTime> getTodayFreeTimes(Long userId) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();
        return timetableService.getFreeTimesByDay(userId, today).stream()
                .filter(ft -> ft.getEndTime().isAfter(now))
                .map(ft -> {
                    if (ft.getStartTime().isBefore(now)) {
                        ft.setStartTime(now.withMinute(0).withSecond(0).plusHours(1));
                    }
                    return ft;
                })
                .filter(ft -> ft.getStartTime().isBefore(ft.getEndTime()))
                .toList();
    }

    @Transactional
    public MatchEnqueueResult enqueue(Long userId, UserInfoDto userInfo, List<MatchQueue.FreeTimeSlot> requestedSlots) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String userIdStr = String.valueOf(userId);

        if (!userInfo.isAvailable()) {
            throw new IllegalStateException("사용자 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
        if (userInfo.getPoints() < 100) {
            throw new IllegalStateException("포인트가 부족합니다. 매칭에는 100포인트 이상이 필요합니다.");
        }

        if (matchQueueRepository.existsById(userIdStr)) {
            return MatchEnqueueResult.alreadyWaiting();
        }

        MatchQueue myQueue = MatchQueue.builder()
                .id(userIdStr)
                .userId(userId)
                .dayOfWeek(today)
                .requestedSlots(requestedSlots)
                .status(MatchQueueStatus.WAITING)
                .ttl(300)
                .build();
        matchQueueRepository.save(myQueue);

        MatchProposal proposal = findMatch(myQueue);
        if (proposal != null) {
            return MatchEnqueueResult.matched(proposal.getId());
        }

        return MatchEnqueueResult.waiting();
    }

    private MatchProposal findMatch(MatchQueue myQueue) {
        for (MatchQueue other : matchQueueRepository.findAll()) {
            if (other == null) continue;
            if (other.getId().equals(myQueue.getId())) continue;
            if (other.getStatus() != MatchQueueStatus.WAITING) continue;
            if (other.getDayOfWeek() != myQueue.getDayOfWeek()) continue;

            MatchQueue.FreeTimeSlot overlap = findOverlap(myQueue.getRequestedSlots(), other.getRequestedSlots());
            if (overlap != null) {
                return createProposal(other, myQueue, overlap);
            }
        }
        return null;
    }

    private MatchQueue.FreeTimeSlot findOverlap(List<MatchQueue.FreeTimeSlot> slotsA,
                                                 List<MatchQueue.FreeTimeSlot> slotsB) {
        for (MatchQueue.FreeTimeSlot a : slotsA) {
            LocalTime aStart = LocalTime.parse(a.getStartTime(), TIME_FMT);
            LocalTime aEnd   = LocalTime.parse(a.getEndTime(), TIME_FMT);

            for (MatchQueue.FreeTimeSlot b : slotsB) {
                LocalTime bStart = LocalTime.parse(b.getStartTime(), TIME_FMT);
                LocalTime bEnd   = LocalTime.parse(b.getEndTime(), TIME_FMT);

                LocalTime overlapStart = aStart.isAfter(bStart) ? aStart : bStart;
                LocalTime overlapEnd   = aEnd.isBefore(bEnd) ? aEnd : bEnd;

                if (overlapStart.isBefore(overlapEnd) &&
                    overlapEnd.minusMinutes(60).compareTo(overlapStart) >= 0) {
                    return new MatchQueue.FreeTimeSlot(
                            overlapStart.format(TIME_FMT),
                            overlapEnd.format(TIME_FMT));
                }
            }
        }
        return null;
    }

    private MatchProposal createProposal(MatchQueue a, MatchQueue b, MatchQueue.FreeTimeSlot overlap) {
        MatchProposal proposal = MatchProposal.builder()
                .id(UUID.randomUUID().toString())
                .requesterId(a.getUserId())
                .receiverId(b.getUserId())
                .matchedDate(LocalDate.now().toString())
                .matchedStartTime(overlap.getStartTime())
                .matchedEndTime(overlap.getEndTime())
                .requesterAccepted(false)
                .receiverAccepted(false)
                .status(MatchProposalStatus.PENDING)
                .ttl(300)
                .build();

        matchProposalRepository.save(proposal);

        a.setStatus(MatchQueueStatus.MATCHED);
        a.setProposalId(proposal.getId());
        b.setStatus(MatchQueueStatus.MATCHED);
        b.setProposalId(proposal.getId());
        matchQueueRepository.save(a);
        matchQueueRepository.save(b);

        log.info("매칭 성공 - proposalId: {}", proposal.getId());

        String fcmBody = String.format("점심 가능 시간: %s ~ %s", overlap.getStartTime(), overlap.getEndTime());
        sendFcm(a.getUserId(), "혼밥 메이트를 찾았어요!", fcmBody);
        sendFcm(b.getUserId(), "혼밥 메이트를 찾았어요!", fcmBody);

        String sseData = String.format(
                "{\"proposalId\":\"%s\",\"matchedStartTime\":\"%s\",\"matchedEndTime\":\"%s\"}",
                proposal.getId(), overlap.getStartTime(), overlap.getEndTime());
        matchSseService.notify(a.getUserId(), "matched", sseData);
        matchSseService.notify(b.getUserId(), "matched", sseData);

        String notifyMsg = String.format(
                "매칭할 사람이 생겼습니다! 점심 가능 시간: %s ~ %s\n제안 ID: %s\n수락하시겠어요? (수락 / 거절)",
                overlap.getStartTime(), overlap.getEndTime(), proposal.getId());
        agentHistoryService.pushModelMessage(a.getUserId(), notifyMsg);
        agentHistoryService.pushModelMessage(b.getUserId(), notifyMsg);

        return proposal;
    }

    @Transactional
    public MatchProposalResult respond(Long userId, String proposalId, boolean accept) {
        MatchProposal proposal = matchProposalRepository.findById(proposalId).orElse(null);

        if (proposal == null) return MatchProposalResult.timeout();
        if (proposal.getStatus() != MatchProposalStatus.PENDING) {
            return MatchProposalResult.alreadyProcessed(proposal.getStatus());
        }

        boolean isRequester = userId.equals(proposal.getRequesterId());
        boolean isReceiver  = userId.equals(proposal.getReceiverId());
        if (!isRequester && !isReceiver) return MatchProposalResult.notParticipant();

        pointService.deductPoints(userId, 100, "매칭 응답");

        if (!accept) {
            proposal.setStatus(MatchProposalStatus.REJECTED);
            matchProposalRepository.save(proposal);
            matchQueueRepository.deleteById(String.valueOf(userId));

            Long victimId = isRequester ? proposal.getReceiverId() : proposal.getRequesterId();
            matchSseService.notify(victimId, "proposalRejected",
                    "{\"message\":\"상대방이 매칭을 거절했습니다. 다시 대기열에 등록합니다.\"}");
            agentHistoryService.pushModelMessage(victimId, "상대방이 매칭을 거절했습니다. 다시 대기 중으로 전환합니다.");

            MatchQueue victimQueue = matchQueueRepository.findById(String.valueOf(victimId)).orElse(null);
            if (victimQueue != null) {
                sendFcm(victimId, "매칭 거절", "상대방이 매칭을 거절했어요. 다시 대기 중으로 전환합니다.");
                UserInfoDto victimInfo = userServiceClient.getUserById(victimId);
                if (victimInfo.getPoints() >= 100) {
                    victimQueue.setStatus(MatchQueueStatus.WAITING);
                    victimQueue.setProposalId(null);
                    matchQueueRepository.save(victimQueue);
                    findMatch(victimQueue);
                } else {
                    matchQueueRepository.deleteById(String.valueOf(victimId));
                    matchSseService.notify(victimId, "queueRemoved",
                            "{\"message\":\"포인트가 부족하여 대기열에서 제외되었습니다.\"}");
                    agentHistoryService.pushModelMessage(victimId, "포인트가 부족하여 대기열에서 제외되었습니다.");
                }
            }

            return MatchProposalResult.rejected();
        }

        if (isRequester) proposal.setRequesterAccepted(true);
        if (isReceiver)  proposal.setReceiverAccepted(true);

        if (proposal.isRequesterAccepted() && proposal.isReceiverAccepted()) {
            proposal.setStatus(MatchProposalStatus.ACCEPTED);
            matchProposalRepository.save(proposal);
            matchQueueRepository.deleteById(String.valueOf(proposal.getRequesterId()));
            matchQueueRepository.deleteById(String.valueOf(proposal.getReceiverId()));

            var chatRoom = chatService.getOrCreateChatRoom(proposal.getRequesterId(), proposal.getReceiverId());

            String sseData = String.format("{\"chatRoomId\":%d}", chatRoom.getId());
            matchSseService.notify(proposal.getRequesterId(), "chatReady", sseData);
            matchSseService.notify(proposal.getReceiverId(), "chatReady", sseData);

            agentHistoryService.pushModelMessage(proposal.getRequesterId(), "매칭이 완료되었습니다! 채팅방을 확인해보세요!");
            agentHistoryService.pushModelMessage(proposal.getReceiverId(), "매칭이 완료되었습니다! 채팅방을 확인해보세요!");

            return MatchProposalResult.accepted(proposal);
        }

        matchProposalRepository.save(proposal);
        return MatchProposalResult.waitingOther();
    }

    public MatchProposal getProposal(String proposalId) {
        return matchProposalRepository.findById(proposalId).orElse(null);
    }

    @Scheduled(fixedDelay = 30000)
    public void checkExpiredProposals() {
        for (MatchQueue queue : matchQueueRepository.findAll()) {
            if (queue == null) continue;
            if (queue.getStatus() != MatchQueueStatus.MATCHED) continue;
            if (queue.getProposalId() == null) continue;

            boolean proposalExists = matchProposalRepository.findById(queue.getProposalId()).isPresent();
            if (!proposalExists) {
                try {
                    self.handleExpiredQueue(queue);
                } catch (Exception e) {
                    log.error("만료 큐 처리 중 오류 - userId={}", queue.getUserId(), e);
                }
            }
        }
    }

    @Transactional
    public void handleExpiredQueue(MatchQueue queue) {
        log.info("제안 만료 감지 - userId={}", queue.getUserId());
        matchSseService.notify(queue.getUserId(), "proposalExpired",
                "{\"message\":\"매칭 제안이 만료되었습니다. 다시 대기열에 등록합니다.\"}");
        agentHistoryService.pushModelMessage(queue.getUserId(), "매칭 제안 시간이 초과되었습니다. 다시 대기 중으로 전환합니다.");

        UserInfoDto userInfo = userServiceClient.getUserById(queue.getUserId());
        if (userInfo.getPoints() >= 100) {
            queue.setStatus(MatchQueueStatus.WAITING);
            queue.setProposalId(null);
            matchQueueRepository.save(queue);
            findMatch(queue);
        } else {
            matchQueueRepository.deleteById(queue.getId());
            matchSseService.notify(queue.getUserId(), "queueRemoved",
                    "{\"message\":\"포인트가 부족하여 대기열에서 제외되었습니다.\"}");
            agentHistoryService.pushModelMessage(queue.getUserId(), "포인트가 부족하여 대기열에서 제외되었습니다.");
        }
    }

    public void cancelQueue(Long userId) {
        matchQueueRepository.deleteById(String.valueOf(userId));
    }

    public MatchQueue getQueueStatus(Long userId) {
        MatchQueue queue = matchQueueRepository.findById(String.valueOf(userId)).orElse(null);
        if (queue == null) return null;

        if (queue.getStatus() == MatchQueueStatus.MATCHED && queue.getProposalId() != null) {
            boolean proposalExists = matchProposalRepository.findById(queue.getProposalId()).isPresent();
            if (!proposalExists) {
                queue.setStatus(MatchQueueStatus.WAITING);
                queue.setProposalId(null);
                matchQueueRepository.save(queue);
            }
        }

        return queue;
    }

    private void sendFcm(Long userId, String title, String body) {
        try {
            String token = userServiceClient.getUserById(userId).getFcmToken();
            if (token != null && !token.isBlank()) {
                userServiceClient.sendFcm(new FcmRequestDto(token, title, body));
            }
        } catch (Exception e) {
            log.warn("FCM 전송 실패 userId={}", userId);
        }
    }
}