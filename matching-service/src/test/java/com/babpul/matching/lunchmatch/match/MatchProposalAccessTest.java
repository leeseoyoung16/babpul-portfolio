package com.babpul.matching.lunchmatch.match;

import com.babpul.matching.client.UserServiceClient;
import com.babpul.matching.config.AuthenticatedUser;
import com.babpul.matching.lunchmatch.agent.AgentHistoryService;
import com.babpul.matching.lunchmatch.agent.LunchMatchTool;
import com.babpul.matching.lunchmatch.timetable.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchProposalAccessTest {

    private static final long REQUESTER = 1L;
    private static final long RECEIVER = 2L;
    private static final long OUTSIDER = 3L;
    private static final String PROPOSAL_ID = "proposal-1";
    private static final String MISSING_ID = "missing";

    private final MatchProposalRepository proposalRepository = mock(MatchProposalRepository.class);
    private final MatchSseService sseService = mock(MatchSseService.class);
    private final MatchService matchService = new MatchService(
            mock(MatchQueueRepository.class), proposalRepository, mock(TimetableService.class), sseService,
            mock(com.babpul.matching.chat.ChatService.class), mock(AgentHistoryService.class),
            mock(UserServiceClient.class), mock(com.babpul.matching.point.PointService.class));

    private final MatchProposal proposal = MatchProposal.builder()
            .id(PROPOSAL_ID)
            .requesterId(REQUESTER)
            .receiverId(RECEIVER)
            .matchedDate("2026-11-04")
            .matchedStartTime("12:00")
            .matchedEndTime("13:00")
            .status(MatchProposalStatus.PENDING)
            .build();

    @BeforeEach
    void setUp() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.findById(MISSING_ID)).thenReturn(Optional.empty());
    }

    // ── 조회 ──

    @ParameterizedTest
    @ValueSource(longs = {REQUESTER, RECEIVER})
    void participantCanReadProposal(long userId) {
        assertThat(matchService.getProposal(userId, PROPOSAL_ID)).isSameAs(proposal);
    }

    @Test
    void outsiderReadLooksLikeExpiredProposal() {
        assertThat(matchService.getProposal(OUTSIDER, PROPOSAL_ID)).isNull();
        assertThat(matchService.getProposal(OUTSIDER, MISSING_ID)).isNull();
    }

    @Test
    void restEndpointUsesLoggedInUser() {
        MatchController controller = new MatchController(matchService, sseService, mock(UserServiceClient.class));

        assertThat(controller.getProposal(PROPOSAL_ID, user(RECEIVER))).isSameAs(proposal);
        assertThat(controller.getProposal(PROPOSAL_ID, user(OUTSIDER))).isNull();
    }

    @Test
    void agentToolUsesCurrentUser() throws Exception {
        LunchMatchTool tool = new LunchMatchTool(
                matchService, mock(TimetableService.class), mock(UserServiceClient.class), new ObjectMapper());

        assertThat(tool.getProposal(REQUESTER, PROPOSAL_ID))
                .contains(PROPOSAL_ID, "12:00", "PENDING")
                .doesNotContain("requesterId", "receiverId");
        assertThat(tool.getProposal(OUTSIDER, PROPOSAL_ID))
                .contains("만료되었거나 존재하지 않습니다")
                .doesNotContain(PROPOSAL_ID, "12:00");
    }

    // ── 응답 ──

    @Test
    void outsiderCannotRespondToPendingProposal() {
        MatchProposalResult result = matchService.respond(OUTSIDER, PROPOSAL_ID, false);

        assertThat(result.getType()).isEqualTo(MatchProposalResult.Type.NOT_PARTICIPANT);
        assertThat(proposal.getStatus()).isEqualTo(MatchProposalStatus.PENDING);
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void outsiderCannotLearnStatusOfProcessedProposal() {
        proposal.setStatus(MatchProposalStatus.REJECTED);

        MatchProposalResult result = matchService.respond(OUTSIDER, PROPOSAL_ID, true);

        assertThat(result.getType()).isEqualTo(MatchProposalResult.Type.NOT_PARTICIPANT);
        assertThat(result.getMessage()).doesNotContain("REJECTED");
    }

    @Test
    void participantStillSeesAlreadyProcessed() {
        proposal.setStatus(MatchProposalStatus.REJECTED);

        MatchProposalResult result = matchService.respond(RECEIVER, PROPOSAL_ID, true);

        assertThat(result.getType()).isEqualTo(MatchProposalResult.Type.ALREADY_PROCESSED);
    }

    @Test
    void participantCanAcceptPendingProposal() {
        MatchProposalResult result = matchService.respond(REQUESTER, PROPOSAL_ID, true);

        assertThat(result.getType()).isEqualTo(MatchProposalResult.Type.WAITING_OTHER);
        assertThat(proposal.isRequesterAccepted()).isTrue();
        verify(proposalRepository).save(proposal);
    }

    @Test
    void missingProposalStillTimesOut() {
        assertThat(matchService.respond(REQUESTER, MISSING_ID, true).getType())
                .isEqualTo(MatchProposalResult.Type.TIMEOUT);
    }

    private AuthenticatedUser user(long userId) {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getUserId()).thenReturn(userId);
        return user;
    }
}
