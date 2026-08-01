// app/src/main/java/com/example/quietp/Alert.java

package com.example.quietp;

// 🔔 층간소음 알림 데이터 모델 (간단 버전)
public class Alert {

    // Firebase 에서 쓰기 좋은 public 필드들
    public String id;          // 알림 id (push key 등)
    public String senderUid;   // 보낸 사람 uid
    public String receiverUid; // 받는 사람 uid (또는 호수, 동/호 등으로 나중에 바꿔도 됨)
    public long timestamp;     // 보낸 시간 (System.currentTimeMillis)
    public String status;      // "sent", "read" 등 상태 표시용

    // Firebase용 기본 생성자 (필수)
    public Alert() { }

    // AlertFragment 에서 쓰는 생성자
    public Alert(String id,
                 String senderUid,
                 String receiverUid,
                 long timestamp,
                 String status) {
        this.id = id;
        this.senderUid = senderUid;
        this.receiverUid = receiverUid;
        this.timestamp = timestamp;
        this.status = status;
    }

}
