package com.bappul.matching.chat;

import com.bappul.matching.config.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/chat/room")
    public ResponseEntity<ChatRoomResponseDto> createOrGetChatRoom(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid ChatRoomRequestDto requestDto
    ) {
        return ResponseEntity.ok(chatService.getOrCreateChatRoom(authUser.getUserId(), requestDto.getOpponentUserId()));
    }

    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomResponseDto>> getChatRoomList(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(chatService.getChatRoomsForUser(authUser.getUserId()));
    }

    @GetMapping("/api/chat/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getPreviousMessages(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(chatService.getMessagesByRoomId(roomId, authUser.getUserId(), 100));
    }
}