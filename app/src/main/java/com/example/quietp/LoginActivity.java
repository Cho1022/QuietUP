package com.example.quietp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class LoginActivity extends AppCompatActivity {

    public static final String FRAG_LOGIN = "LoginFragment";
    public static final String FRAG_JOIN = "JoinFragment";
    public static final String FRAG_ADD_USER_INFO = "AddUserInfoFragment";

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 스플래시
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);   // activity_login.xml = FrameLayout(login_container)

        if (savedInstanceState == null) {
            // 앱 처음 들어올 때 로그인 프래그먼트 보여줌
            replaceFragment(FRAG_LOGIN, false);
        }
    }

    // 태그 기반 프래그먼트 교체
    public void replaceFragment(String tag, boolean addToBackStack) {
        Fragment newFragment = null;

        switch (tag) {
            case FRAG_LOGIN:
                newFragment = new LoginFragment();
                break;
            case FRAG_JOIN:
                newFragment = new JoinFragment();
                break;
            case FRAG_ADD_USER_INFO:
                newFragment = new AddUserInfoFragment();
                break;
        }

        if (newFragment == null) return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.replace(R.id.login_container, newFragment, tag);

        if (addToBackStack) {
            tx.addToBackStack(tag);
        }

        tx.commit();
        currentFragment = newFragment;
    }

    // 로그인 성공 시 메인으로 이동
    public void moveToMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // ★ 회원가입 + 추가정보 입력 완료 후
    //    “처음 로그인 화면”으로 완전히 되돌리는 전용 메서드
    public void showLoginFragmentClearBackStack() {
        FragmentManager fm = getSupportFragmentManager();

        // 백스택 전체 제거
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // 로그인 프래그먼트로 교체 (백스택에 안 올림)
        FragmentTransaction tx = fm.beginTransaction();
        tx.replace(R.id.login_container, new LoginFragment(), FRAG_LOGIN);
        tx.commit();

        currentFragment = fm.findFragmentByTag(FRAG_LOGIN);
    }
}
