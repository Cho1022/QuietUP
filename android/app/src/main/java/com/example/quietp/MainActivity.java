package com.example.quietp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ⭐ 시스템 스플래시
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // 스플래시 아이콘 애니메이션
        splashScreen.setOnExitAnimationListener(provider -> {
            View iconView = provider.getIconView();

            PropertyValuesHolder scaleX =
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 0f);
            PropertyValuesHolder scaleY =
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 0f);
            PropertyValuesHolder alpha =
                    PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 1f, 0f);

            ObjectAnimator animator =
                    ObjectAnimator.ofPropertyValuesHolder(iconView, scaleX, scaleY, alpha);

            animator.setInterpolator(new AnticipateInterpolator());
            animator.setDuration(700);

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    provider.remove();
                }
            });

            animator.start();
        });

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        // 첫 화면: 홈 탭
        if (savedInstanceState == null) {
            replaceRootFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                replaceRootFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_board) {
                replaceRootFragment(new BoardFragment());
                return true;
            } else if (id == R.id.nav_alert) {
                replaceRootFragment(new AlertFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                replaceRootFragment(new ChatFragment());
                return true;
            }
            return false;
        });
    }

    /**
     * 🔹 하단 탭(Home / Board / Alert / Chat) 전환용
     *    → 백스택에 안 쌓음
     */
    public void replaceRootFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_main, fragment)
                .commit();
    }

    /**
     * 🔹 게시글 목록 → 읽기, 읽기 → 수정 이런 “상세 화면으로 이동”용
     *    → 백스택에 쌓아서 뒤로가기 가능
     *
     *    예) ((MainActivity)getActivity()).replaceFragment(new PostReadFragment());
     */
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_main, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * 🔹 Fragment 쪽에서 뒤로 가고 싶을 때 사용
     *    예) ((MainActivity)getActivity()).popBackStack();
     */
    public void popBackStack() {
        getSupportFragmentManager().popBackStack();
    }
}
