package com.quietup.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.quietup.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderByIdAsc(Long chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
            Long chatRoomId,
            Long afterMessageId,
            Pageable pageable);

    Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);
}
