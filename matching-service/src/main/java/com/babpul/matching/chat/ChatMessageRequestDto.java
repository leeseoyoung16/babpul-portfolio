package com.babpul.matching.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequestDto {

    @NotNull
    private Long roomId;

    @NotBlank
    private String content;

    @NotNull
    private ChatMessageType type;
}