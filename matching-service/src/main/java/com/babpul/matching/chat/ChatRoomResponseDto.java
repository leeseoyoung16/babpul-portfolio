package com.babpul.matching.chat;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatRoomResponseDto {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private LocalDateTime lastMessageAt;
    private int unreadCount;

    public ChatRoomResponseDto(ChatRoom room) {
        this.id = room.getId();
        this.user1Id = room.getUser1Id();
        this.user2Id = room.getUser2Id();
        this.lastMessageAt = room.getLastMessageAt();
        this.unreadCount = 0;
    }

    public ChatRoomResponseDto(ChatRoom room, Long requesterId) {
        this.id = room.getId();
        this.user1Id = room.getUser1Id();
        this.user2Id = room.getUser2Id();
        this.lastMessageAt = room.getLastMessageAt();
        this.unreadCount = room.getUser1Id().equals(requesterId) ? room.getUnreadCount1() : room.getUnreadCount2();
    }
}