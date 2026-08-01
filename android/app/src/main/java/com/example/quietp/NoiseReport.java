package com.example.quietp;

public class NoiseReport {
    public String id;
    public String room;       // 예: 302호
    public long   timeMillis; // 신고 시간
    public String noiseType;  // 예: 의자 끄는 소리
    public String memo;       // 메모

    public NoiseReport() {
        // Firebase 에서 사용하려면 기본 생성자 꼭 필요
    }

    public NoiseReport(String id, String room, long timeMillis,
                       String noiseType, String memo) {
        this.id = id;
        this.room = room;
        this.timeMillis = timeMillis;
        this.noiseType = noiseType;
        this.memo = memo;
    }
}
