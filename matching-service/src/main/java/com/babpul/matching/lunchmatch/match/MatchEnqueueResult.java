package com.babpul.matching.lunchmatch.match;

import lombok.Getter;

@Getter
public class MatchEnqueueResult {

    public enum Type { WAITING, MATCHED, ALREADY_WAITING }

    private final Type type;
    private final String proposalId;
    private final String message;

    private MatchEnqueueResult(Type type, String proposalId, String message) {
        this.type = type;
        this.proposalId = proposalId;
        this.message = message;
    }

    public static MatchEnqueueResult waiting() {
        return new MatchEnqueueResult(Type.WAITING, null, "매칭 대기 중입니다. 최대 5분 내에 매칭됩니다.");
    }

    public static MatchEnqueueResult matched(String proposalId) {
        return new MatchEnqueueResult(Type.MATCHED, proposalId, "매칭 상대를 찾았습니다! 30초 내에 수락해주세요.");
    }

    public static MatchEnqueueResult alreadyWaiting() {
        return new MatchEnqueueResult(Type.ALREADY_WAITING, null, "이미 매칭 대기 중입니다.");
    }
}