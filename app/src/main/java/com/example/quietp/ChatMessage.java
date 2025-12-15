package com.example.quietp;

public class ChatMessage {
    public String id;
    public String roomId;
    public String senderUid;
    public String nickname;
    public String message;
    public long timeMillis;

    public ChatMessage() {
        // Firebase 역직렬화용
    }

    public ChatMessage(String id,
                       String roomId,
                       String senderUid,
                       String nickname,
                       String message,
                       long timeMillis) {
        this.id = id;
        this.roomId = roomId;
        this.senderUid = senderUid;
        this.nickname = nickname;
        this.message = message;
        this.timeMillis = timeMillis;
    }
}
