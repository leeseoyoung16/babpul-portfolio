package com.bappul.matching.chat;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageResponseDto {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String content;
    private ChatMessageType type;
    private LocalDateTime sentAt;

    public ChatMessageResponseDto(ChatMessage message) {
        this.id       = message.getId();
        this.roomId   = message.getRoom().getId();
        this.senderId = message.getSenderId();
        this.content  = message.getContent();
        this.type     = message.getType();
        this.sentAt   = message.getSentAt();
    }
}