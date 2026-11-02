package com.bappul.matching.matching.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * chat-service 에서 받아오는 채팅방 응답 DTO.
 * matching-service 는 id 필드만 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomResponse {
    private Long id;
}
