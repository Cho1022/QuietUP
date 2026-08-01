package com.example.quietp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentJoinBinding;
import com.google.firebase.auth.FirebaseAuth;

public class JoinFragment extends Fragment {

    private FragmentJoinBinding binding;
    private LoginActivity loginActivity;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentJoinBinding.inflate(inflater, container, false);
        loginActivity = (LoginActivity) getActivity();
        auth = FirebaseAuth.getInstance();

        setupToolbar();
        setupButton();

        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.toolbarJoin.setTitle("회원가입");
        binding.toolbarJoin.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbarJoin.setNavigationOnClickListener(v -> {
            // 뒤로가기
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupButton() {
        binding.buttonJoinNext.setOnClickListener(v -> doJoin());
    }

    private void doJoin() {
        String email = binding.inputJoinUserId.getText().toString().trim();
        String pw = binding.inputJoinUserPw.getText().toString().trim();
        String pw2 = binding.inputJoinUserPw2.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pw) || TextUtils.isEmpty(pw2)) {
            Toast.makeText(getContext(), "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pw.equals(pw2)) {
            Toast.makeText(getContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.buttonJoinNext.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(task -> {
                    binding.buttonJoinNext.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "회원가입 성공, 추가 정보 입력으로 이동", Toast.LENGTH_SHORT).show();
                        if (loginActivity != null) {
                            loginActivity.replaceFragment(LoginActivity.FRAG_ADD_USER_INFO, true);
                        }
                    } else {
                        Toast.makeText(getContext(), "회원가입 실패: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
