package com.example.quietp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

/**
 * 게시글(포스트) 관련 Firebase Realtime Database 접근 모음
 *  - 글 작성 / 수정 / 삭제
 *  - 글 목록 조회
 *  - 단일 글 조회
 *  - 인기 글 조회 (조회수 기반)
 */
public class ContentRepository {

    private final FirebaseAuth auth;
    private final DatabaseReference postsRef;

    public ContentRepository() {
        auth = FirebaseAuth.getInstance();
        postsRef = FirebaseDatabase.getInstance().getReference(Value.PATH_POSTS);
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // -------------------------------------------------------
    // 1. 글 작성 (카테고리 + 제목 + 내용만 받아서 생성)
    // -------------------------------------------------------
    public void createPost(@NonNull String category,
                           @NonNull String title,
                           @NonNull String body,
                           @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("로그인 상태가 아닙니다.")
            ));
            return;
        }

        String key = postsRef.push().getKey();
        if (key == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("게시글 키 생성에 실패했습니다.")
            ));
            return;
        }

        long now = System.currentTimeMillis();
        String writerName = user.getEmail();   // 닉네임 대신 이메일 사용(임시)

        ContentModel model = new ContentModel(
                key,            // id
                title,          // title
                body,           // body
                null,           // imageUrl (지금은 안 씀)
                user.getUid(),  // writerUid
                writerName,     // writerName
                category,       // category
                now,            // createdAt
                now,            // updatedAt
                0L              // viewCount (조회수)
        );

        postsRef.child(key)
                .setValue(model)
                .addOnCompleteListener(listener);
    }

    // 모델 통째로 받아서 생성하고 싶을 때
    public void createPost(@NonNull ContentModel model,
                           @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("로그인 상태가 아닙니다.")
            ));
            return;
        }

        String key = model.id != null ? model.id : postsRef.push().getKey();
        if (key == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("게시글 키 생성에 실패했습니다.")
            ));
            return;
        }

        long now = System.currentTimeMillis();

        model.id = key;
        model.writerUid = user.getUid();
        if (model.createdAt == 0) {
            model.createdAt = now;
        }
        model.updatedAt = now;

        postsRef.child(key)
                .setValue(model)
                .addOnCompleteListener(listener);
    }

    // -------------------------------------------------------
    // 2. 글 수정
    // -------------------------------------------------------
    public void updatePost(@NonNull ContentModel model,
                           @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || model.id == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("잘못된 상태입니다.")
            ));
            return;
        }

        long now = System.currentTimeMillis();
        model.updatedAt = now;

        postsRef.child(model.id)
                .setValue(model)
                .addOnCompleteListener(listener);
    }

    // -------------------------------------------------------
    // 3. 글 삭제 (작성자만 가능)
    // -------------------------------------------------------
    public void deletePost(@NonNull String postId,
                           @NonNull String writerUid,
                           @NonNull OnCompleteListener<Void> listener) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(
                    new IllegalStateException("로그인 상태가 아닙니다.")
            ));
            return;
        }

        if (!writerUid.equals(user.getUid())) {
            listener.onComplete(Tasks.forException(
                    new IllegalAccessException("삭제 권한이 없습니다.")
            ));
            return;
        }

        postsRef.child(postId)
                .removeValue()
                .addOnCompleteListener(listener);
    }

    // -------------------------------------------------------
    // 4. 글 목록 조회 (카테고리별 / 전체)
    // -------------------------------------------------------
    public void observePostList(@NonNull String category,
                                @NonNull ValueEventListener listener) {

        Query q;
        if (Value.CATEGORY_ALL.equals(category)) {
            q = postsRef.orderByChild("createdAt");
        } else {
            q = postsRef.orderByChild("category").equalTo(category);
        }

        q.addValueEventListener(listener);
    }

    // -------------------------------------------------------
    // 5. 단일 글 조회 (PostRead / PostModify 에서 사용)
    // -------------------------------------------------------
    public void loadPost(@NonNull String postId,
                         @NonNull ValueEventListener listener) {
        postsRef.child(postId)
                .addListenerForSingleValueEvent(listener);
    }

    // -------------------------------------------------------
    // 6. 인기글 조회 (조회수 기준 상위 N개)
    // -------------------------------------------------------
    public void loadTopPosts(int limit,
                             @NonNull ValueEventListener listener) {

        Query q = postsRef.orderByChild("viewCount").limitToLast(limit);
        q.addListenerForSingleValueEvent(listener);
    }

    // -------------------------------------------------------
    // 7. 조회수 1 증가 (글 읽기 들어갈 때 호출)
    // -------------------------------------------------------
    public void increaseViewCount(@NonNull String postId) {
        postsRef.child(postId).child("viewCount")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long value = currentData.getValue(Long.class);
                        if (value == null) value = 0L;
                        currentData.setValue(value + 1);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error,
                                           boolean committed,
                                           @Nullable DataSnapshot currentData) {
                        // 필요하면 여기서 Log 찍어도 되고, 지금처럼 비워 둬도 됨
                    }
                });
    }

    // -------------------------------------------------------
    // 8. 카테고리별 최신 글 1개
    //    - createdAt 기준으로 최근 50개 정도 가져와서
    //      클라이언트에서 category로 한 번 더 필터링
    // -------------------------------------------------------
    public void loadLatestPostForCategory(@NonNull String category,
                                          @NonNull ValueEventListener listener) {
        // 전체 글 중 최근 것들만 가져온 뒤, 코드에서 카테고리 필터
        Query q = postsRef.orderByChild("createdAt").limitToLast(50);
        q.addListenerForSingleValueEvent(listener);
    }

}
