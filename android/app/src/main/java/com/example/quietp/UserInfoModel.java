package com.example.quietp;

import java.util.List;

// 회원 추가 정보 모델
public class UserInfoModel {

    public String uid;          // Firebase uid
    public String email;        // 이메일
    public String nickname;     // 닉네임
    public int age;             // 나이

    // 🔹 대표 아파트 이름 (홈 화면 상단에 표시용)
    public String aptName;

    // 🔹 관심 아파트 리스트 (체크박스에서 선택한 전체 목록)
    public List<String> aptList;

    // Firebase용 기본 생성자
    public UserInfoModel() {}

    public UserInfoModel(String uid,
                         String email,
                         String nickname,
                         int age,
                         String aptName,
                         List<String> aptList) {

        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.age = age;
        this.aptName = aptName;
        this.aptList = aptList;
    }
}
