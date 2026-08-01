package com.example.quietp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

// 사용자 추가정보(닉네임, 나이, 취미 등)를 저장/읽어오는 저장소
public class UserInfoRepository {

    private final FirebaseAuth auth;
    private final DatabaseReference userInfoRef;

    public UserInfoRepository() {
        auth = FirebaseAuth.getInstance();
        // Firebase Realtime Database 의 경로 (원하는 이름으로 바꿔도 됨)
        userInfoRef = FirebaseDatabase.getInstance().getReference("user_info");
        // 만약 예전에 Value.PATH_USER_INFO 이런 상수 썼다면:
        // userInfoRef = FirebaseDatabase.getInstance().getReference(Value.PATH_USER_INFO);
    }

    // 현재 로그인한 유저
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * 내 사용자 정보 저장
     */
    public void saveMyUserInfo(@NonNull UserInfoModel model,
                               @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // ★ 여기서 Tasks.forException 사용
            listener.onComplete(
                    Tasks.forException(
                            new IllegalStateException("not logged in")
                    )
            );
            return;
        }

        // uid / email 채워 넣기
        model.uid = user.getUid();
        if (model.email == null || model.email.isEmpty()) {
            model.email = user.getEmail();
        }

        // /user_info/{uid} 에 저장
        userInfoRef.child(model.uid)
                .setValue(model)
                .addOnCompleteListener(listener);
    }

    /**
     * 내 사용자 정보 한 번 읽기
     */
    public void loadMyUserInfo(@NonNull ValueEventListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // 로그인 안 된 상태면 DB 요청 안 하고, 에러를 listener 에 직접 전달하고 싶다면
            // 별도 콜백 방식으로 구성해야 하는데,
            // 지금 구조에서는 보통 Fragment 쪽에서 user == null인지 체크 후 이 메서드를 부르는 게 깔끔함.
            return;
        }

        userInfoRef.child(user.getUid())
                .addListenerForSingleValueEvent(listener);
    }
}
