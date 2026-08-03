package com.quietup.chat.entity;

import java.time.LocalDateTime;

import com.quietup.residence.entity.Residence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(
                name = "idx_chat_messages_room_id",
                columnList = "chat_room_id, id"))
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_residence_id", nullable = false)
    private Residence senderResidence;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(
            ChatRoom chatRoom,
            Residence senderResidence,
            String content,
            LocalDateTime createdAt) {
        if (!chatRoom.isParticipant(senderResidence.getId())) {
            throw new IllegalArgumentException("채팅 참여자만 메시지를 보낼 수 있습니다.");
        }
        this.chatRoom = chatRoom;
        this.senderResidence = senderResidence;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Residence getSenderResidence() {
        return senderResidence;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
