package com.example.quietp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ChatRepository {

    private final FirebaseAuth auth;
    private final DatabaseReference roomsRef;     // /chatRooms
    private final DatabaseReference messagesRef;  // /chatMessages

    public ChatRepository() {
        auth = FirebaseAuth.getInstance();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        roomsRef = db.getReference("chatRooms");
        messagesRef = db.getReference("chatMessages");
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // -----------------------------
    // 1. 방 목록 구독
    // -----------------------------
    public void observeRoomList(@NonNull ValueEventListener listener) {
        Query q = roomsRef.orderByChild("createdAt");
        q.addValueEventListener(listener);
    }

    // -----------------------------
    // 2. 방 생성 (단순히 새로 추가)
    // -----------------------------
    public void createRoom(@NonNull String name,
                           @NonNull OnCompleteListener<String> listener) {

        String key = roomsRef.push().getKey();
        if (key == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("방 키 생성 실패")
            ));
            return;
        }

        long now = System.currentTimeMillis();
        ChatRoom room = new ChatRoom(
                key,
                name,
                now,
                "",   // lastMessage
                0L    // lastMessageTime
        );

        roomsRef.child(key)
                .setValue(room)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("방 생성 실패");
                    }
                    return key;  // 성공 시 방 ID 반환
                })
                .addOnCompleteListener(listener);
    }


    // -----------------------------
    // 3. 특정 방 메시지 스트림
    // -----------------------------
    public void observeMessages(@NonNull String roomId,
                                @NonNull ValueEventListener listener) {

        messagesRef.child(roomId)
                .orderByChild("timeMillis")
                .addValueEventListener(listener);
    }

    // -----------------------------
    // 4. 메시지 전송
    // -----------------------------
    public void sendMessage(@NonNull String roomId,
                            @NonNull String nickname,
                            @NonNull String message,
                            @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("로그인 상태가 아닙니다.")
            ));
            return;
        }

        String key = messagesRef.child(roomId).push().getKey();
        if (key == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("메시지 키 생성 실패")
            ));
            return;
        }

        long now = System.currentTimeMillis();

        ChatMessage model = new ChatMessage(
                key,
                roomId,
                user.getUid(),
                nickname,
                message,
                now
        );

        messagesRef.child(roomId).child(key)
                .setValue(model)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 방의 마지막 메시지 정보 갱신
                        roomsRef.child(roomId).child("lastMessage").setValue(message);
                        roomsRef.child(roomId).child("lastMessageTime").setValue(now);
                    }
                    listener.onComplete(task);
                });
    }
    // 채팅방 삭제 (방 정보 + 메시지 전부 삭제)
    public void deleteRoom(@NonNull String roomId,
                           @NonNull OnCompleteListener<Void> listener) {

        if (roomId.trim().isEmpty()) {
            listener.onComplete(
                    Tasks.forException(new IllegalArgumentException("roomId 가 비어 있습니다."))
            );
            return;
        }

        // roomsRef, messagesRef 는 기존에 선언돼 있는 거 그대로 사용
        roomsRef.child(roomId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messagesRef.child(roomId).removeValue();
                    }
                    listener.onComplete(task);
                });
    }

}

