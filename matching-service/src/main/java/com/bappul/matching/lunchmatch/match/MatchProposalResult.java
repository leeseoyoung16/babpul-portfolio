package com.bappul.matching.lunchmatch.match;

import lombok.Getter;

@Getter
public class MatchProposalResult {

    public enum Type { ACCEPTED, WAITING_OTHER, REJECTED, TIMEOUT, ALREADY_PROCESSED, NOT_PARTICIPANT }

    private final Type type;
    private final String message;
    private final MatchProposal proposal;

    private MatchProposalResult(Type type, String message, MatchProposal proposal) {
        this.type = type;
        this.message = message;
        this.proposal = proposal;
    }

    public static MatchProposalResult accepted(MatchProposal proposal) {
        return new MatchProposalResult(Type.ACCEPTED, "매칭이 완료되었습니다! 채팅을 시작해보세요.", proposal);
    }

    public static MatchProposalResult waitingOther() {
        return new MatchProposalResult(Type.WAITING_OTHER, "수락 완료! 상대방의 응답을 기다리는 중입니다.", null);
    }

    public static MatchProposalResult rejected() {
        return new MatchProposalResult(Type.REJECTED, "매칭을 거절했습니다.", null);
    }

    public static MatchProposalResult timeout() {
        return new MatchProposalResult(Type.TIMEOUT, "매칭 제안이 만료되었습니다. 다시 시도해주세요.", null);
    }

    public static MatchProposalResult alreadyProcessed(MatchProposalStatus status) {
        return new MatchProposalResult(Type.ALREADY_PROCESSED, "이미 처리된 제안입니다: " + status, null);
    }

    public static MatchProposalResult notParticipant() {
        return new MatchProposalResult(Type.NOT_PARTICIPANT, "해당 매칭의 참여자가 아닙니다.", null);
    }
}