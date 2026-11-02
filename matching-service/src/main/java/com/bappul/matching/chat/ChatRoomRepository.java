package com.bappul.matching.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE (r.user1Id = :u1 AND r.user2Id = :u2) OR (r.user1Id = :u2 AND r.user2Id = :u1)")
    Optional<ChatRoom> findByUsers(Long u1, Long u2);

    @Query("SELECT r FROM ChatRoom r WHERE r.user1Id = :userId OR r.user2Id = :userId ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findByUserIdOrderByLastMessageAtDesc(Long userId);
}