package com.example.quietp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class NoiseRepository {

    public static final String PATH_NOISE = "noiseReports";

    private final DatabaseReference noiseRef;

    public NoiseRepository() {
        noiseRef = FirebaseDatabase.getInstance().getReference(PATH_NOISE);
    }

    // 신고 생성
    public void createReport(@NonNull String room,
                             @NonNull String noiseType,
                             @NonNull String memo,
                             @NonNull OnCompleteListener<Void> listener) {

        String key = noiseRef.push().getKey();
        if (key == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("신고 키 생성에 실패했습니다.")
            ));
            return;
        }

        long now = System.currentTimeMillis();
        NoiseReport model = new NoiseReport(key, room, now, noiseType, memo);

        noiseRef.child(key).setValue(model).addOnCompleteListener(listener);
    }

    // 최근 N개 신고(리스트용)
    public void observeRecentReports(int limit, @NonNull ValueEventListener listener) {
        Query q = noiseRef.orderByChild("timeMillis").limitToLast(limit);
        q.addValueEventListener(listener);
    }

    // 가장 최신 신고 1개(홈 카드용)
    public void loadLastReport(@NonNull ValueEventListener listener) {
        Query q = noiseRef.orderByChild("timeMillis").limitToLast(1);
        q.addListenerForSingleValueEvent(listener);
    }
}
