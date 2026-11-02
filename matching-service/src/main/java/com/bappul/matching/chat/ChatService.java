package com.bappul.matching.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatRoomResponseDto getOrCreateChatRoom(Long userAId, Long userBId) {
        Long u1Id = userAId < userBId ? userAId : userBId;
        Long u2Id = userAId < userBId ? userBId : userAId;

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByUsers(u1Id, u2Id);

        ChatRoom chatRoom;
        if (existingRoom.isPresent()) {
            chatRoom = existingRoom.get();
        } else {
            try {
                ChatRoom newRoom = new ChatRoom();
                newRoom.setUser1Id(u1Id);
                newRoom.setUser2Id(u2Id);
                chatRoom = chatRoomRepository.saveAndFlush(newRoom);
            } catch (DataIntegrityViolationException e) {
                chatRoom = chatRoomRepository.findByUsers(u1Id, u2Id)
                        .orElseThrow(() -> new RuntimeException("채팅방 조회에 실패했습니다."));
            }
        }

        return new ChatRoomResponseDto(chatRoom);
    }

    @Transactional
    public ChatMessage saveMessage(Long roomId, Long senderId, String content, ChatMessageType type) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        boolean isParticipant = room.getUser1Id().equals(senderId) || room.getUser2Id().equals(senderId);
        if (!isParticipant) throw new RuntimeException("해당 채팅방의 참여자가 아닙니다.");

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setType(type);

        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage saveAndPublishMessage(Long senderId, Long chatRoomId, String content, ChatMessageType type) {
        ChatMessage saved = saveMessage(chatRoomId, senderId, content, type);

        ChatRoom room = saved.getRoom();
        if (room.getUser1Id().equals(senderId)) {
            room.setUnreadCount2(room.getUnreadCount2() + 1);
        } else {
            room.setUnreadCount1(room.getUnreadCount1() + 1);
        }
        chatRoomRepository.save(room);

        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoomId, new ChatMessageResponseDto(saved));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponseDto> getChatRoomsForUser(Long userId) {
        return chatRoomRepository.findByUserIdOrderByLastMessageAtDesc(userId)
                .stream()
                .map(room -> new ChatRoomResponseDto(room, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ChatMessageResponseDto> getMessagesByRoomId(Long roomId, Long requesterId, int limit) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        boolean isParticipant = room.getUser1Id().equals(requesterId) || room.getUser2Id().equals(requesterId);
        if (!isParticipant) throw new RuntimeException("해당 채팅방의 참여자가 아닙니다.");

        if (room.getUser1Id().equals(requesterId)) room.setUnreadCount1(0);
        else room.setUnreadCount2(0);
        chatRoomRepository.save(room);

        return chatMessageRepository.findByRoomOrderBySentAtAsc(room)
                .stream().limit(limit).map(ChatMessageResponseDto::new).collect(Collectors.toList());
    }
}