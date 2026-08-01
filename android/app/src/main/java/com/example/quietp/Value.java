package com.example.quietp;

// ★ QuietUp 공용 상수 모음
public class Value {

    // ================================
    // 1) 게시판 타입 (정수로 쓰는 용도)
    //    PostListFragment 등에서 사용
    // ================================
    public static final int BOARD_TYPE_ALL    = 0;  // 전체
    public static final int BOARD_TYPE_NOTICE = 1;  // 공지
    public static final int BOARD_TYPE_FREE   = 2;  // 자유
    public static final int BOARD_TYPE_QNA    = 3;  // Q&A
    public static final int BOARD_TYPE_INFO   = 4;  // 정보, 기타

    // ================================
    // 2) (예전 샘플 코드용) 콘텐츠 타입
    //    혹시 사용하는 곳 있을까 봐 그대로 둠
    // ================================
    public static final int CONTENT_TYPE1 = 1;
    public static final int CONTENT_TYPE2 = 2;
    public static final int CONTENT_TYPE3 = 3;
    public static final int CONTENT_TYPE4 = 4;

    // ================================
    // 3) Firebase Realtime Database 경로
    //    UserInfoRepository, ContentRepository 에서 사용
    // ================================
    public static final String PATH_USERS = "users";
    public static final String PATH_POSTS = "posts";

    // ================================
    // 4) 게시판 카테고리 문자열
    //    ContentModel.category, ContentRepository.observePostList 에서 사용
    // ================================

    // 🔹 게시판 카테고리 (문자열로 관리)
    public static final String CATEGORY_ALL   = "ALL";     // 전체
    public static final String CATEGORY_FREE  = "FREE";    // 자유게시판
    public static final String CATEGORY_SECRET = "SECRET"; // 비밀게시판
    public static final String CATEGORY_GRAD  = "GRAD";    // 졸업생 게시판
    public static final String CATEGORY_CLUB  = "CLUB";    // 동아리 게시판

    // 화면에 보여줄 한글 이름
    public static String getCategoryLabel(String category) {
        switch (category) {
            case CATEGORY_FREE:   return "자유게시판";
            case CATEGORY_SECRET: return "비밀게시판";

            case CATEGORY_GRAD:   return "정보게시판";
            case CATEGORY_CLUB:   return "신세대게시판";

            case CATEGORY_ALL:
            default:              return "전체게시글";
        }
    }
//    public static final String CATEGORY_ALL    = "ALL";     // 전체
//    public static final String CATEGORY_NOTICE = "NOTICE";  // 공지
//    public static final String CATEGORY_FREE   = "FREE";    // 자유
//    public static final String CATEGORY_QNA    = "QNA";     // Q&A
//    public static final String CATEGORY_INFO   = "INFO";    // 기타/정보
}
