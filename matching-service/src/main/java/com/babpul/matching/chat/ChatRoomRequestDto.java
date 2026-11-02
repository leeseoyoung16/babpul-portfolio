package com.babpul.matching.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomRequestDto {
    @NotNull
    private Long opponentUserId;
}