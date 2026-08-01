package com.example.quietp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentPostMainBinding;
import com.google.android.material.navigation.NavigationView;

public class PostMainFragment extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    FragmentPostMainBinding fragmentPostMainBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentPostMainBinding = FragmentPostMainBinding.inflate(inflater, container, false);

        setHasOptionsMenu(true);
        setToolbar();
        setDrawer();

        // 처음에는 전체 게시글 목록
        showPostList(Value.BOARD_TYPE_ALL);

        return fragmentPostMainBinding.getRoot();
    }

    // 툴바를 액션바로 세팅
    public void setToolbar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(fragmentPostMainBinding.toolbarPostMain);
            activity.getSupportActionBar().setTitle("게시판");
        }
    }

    // 드로어(왼쪽 메뉴) 세팅
    public void setDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(),
                fragmentPostMainBinding.drawerLayoutPostMain,
                fragmentPostMainBinding.toolbarPostMain,
                R.string.app_name,
                R.string.app_name
        );
        fragmentPostMainBinding.drawerLayoutPostMain.addDrawerListener(toggle);
        toggle.syncState();

        fragmentPostMainBinding.naviPostMain.setNavigationItemSelectedListener(this);
    }

    // 왼쪽 메뉴 아이템 클릭 시
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_post_all) {
            showPostList(Value.BOARD_TYPE_ALL);
        } else if (id == R.id.menu_post_free) {
            showPostList(Value.BOARD_TYPE_FREE);
        } else if (id == R.id.menu_post_notice) {
            showPostList(Value.BOARD_TYPE_NOTICE);
        }

        fragmentPostMainBinding.drawerLayoutPostMain.closeDrawer(GravityCompat.START);
        return true;
    }

    // 게시글 목록 프래그먼트 표시
    public void showPostList(int boardType) {
        PostListFragment postListFragment = new PostListFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("boardType", boardType);
        postListFragment.setArguments(bundle);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.postMainContainer, postListFragment)
                .commit();
    }
}
