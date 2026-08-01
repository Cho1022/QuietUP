package com.example.quietp;

/**
 * Firebase Realtime Database 에 저장되는 게시글 모델
 *  /posts/{postId}
 */
public class ContentModel {

    // DB key
    public String id;

    // 내용 관련
    public String title;
    public String body;
    public String imageUrl;

    // 작성자 정보
    public String writerUid;
    public String writerName;

    // 게시판 정보
    public String category;   // Value.CATEGORY_XXX

    // 시간 정보
    public long createdAt;
    public long updatedAt;

    // 조회수 (인기글용)
    public long viewCount;

    // Firebase 기본 생성자
    public ContentModel() {
    }

    public ContentModel(String id,
                        String title,
                        String body,
                        String imageUrl,
                        String writerUid,
                        String writerName,
                        String category,
                        long createdAt,
                        long updatedAt,
                        long viewCount) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.imageUrl = imageUrl;
        this.writerUid = writerUid;
        this.writerName = writerName;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.viewCount = viewCount;
    }

    // 편의용 getter
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public String getBody() {
        return body != null ? body : "";
    }

    public String getWriterUid() {
        return writerUid;
    }

    public String getWriterName() {
        return writerName != null ? writerName : "";
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getViewCount() {
        return viewCount;
    }
}
