package com.example.quietp;

public class ChatRoom {
    public String id;
    public String name;          // 방 이름 (예: 101동 채팅방)
    public long createdAt;       // 방 생성 시간
    public String lastMessage;   // 마지막 메시지 요약
    public long lastMessageTime; // 마지막 메시지 시간

    public ChatRoom() {
        // Firebase 역직렬화를 위한 기본 생성자
    }

    public ChatRoom(String id, String name,
                    long createdAt,
                    String lastMessage,
                    long lastMessageTime) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }
}
