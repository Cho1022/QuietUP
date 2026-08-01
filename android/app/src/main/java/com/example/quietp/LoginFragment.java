package com.example.quietp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth auth;
    private LoginActivity loginActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        loginActivity = (LoginActivity) getActivity();
        auth = FirebaseAuth.getInstance();

        setupTextWatcher();
        setupButtons();

        // 빈 공간 터치 시 키보드 내리기
        binding.getRoot().setOnClickListener(v -> hideKeyboard());

        // 처음 진입 시 버튼 상태 한 번 업데이트
        updateLoginButtonState();

        return binding.getRoot();
    }

    // 이메일 / 비밀번호 입력 감시해서 버튼 상태 변경
    private void setupTextWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }

            @Override public void afterTextChanged(Editable s) {}
        };

        binding.inputLoginUserId.addTextChangedListener(watcher);
        binding.inputLoginUserPw.addTextChangedListener(watcher);
    }

    // 둘 다 1글자 이상이면 활성(파랑), 아니면 비활성(회색)
    private void updateLoginButtonState() {
        String email = binding.inputLoginUserId.getText().toString().trim();
        String pw    = binding.inputLoginUserPw.getText().toString().trim();

        boolean enabled = !email.isEmpty() && !pw.isEmpty();
        binding.buttonLogin.setEnabled(enabled);

        int color = enabled
                ? ContextCompat.getColor(requireContext(), R.color.login_enabled_blue)
                : ContextCompat.getColor(requireContext(), R.color.login_disabled_gray);

        binding.buttonLogin.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void setupButtons() {
        // 로그인
        binding.buttonLogin.setOnClickListener(v -> doLogin());

        // 회원가입 화면으로 이동
        binding.buttonLoginJoin.setOnClickListener(v -> {
            if (loginActivity != null) {
                loginActivity.replaceFragment(LoginActivity.FRAG_JOIN, true);
            }
        });
    }

    private void doLogin() {
        String email = binding.inputLoginUserId.getText().toString().trim();
        String pw    = binding.inputLoginUserPw.getText().toString().trim();

        if (email.isEmpty() || pw.isEmpty()) {
            Toast.makeText(requireContext(), "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 중복 클릭 방지
        binding.buttonLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener(task -> {
                    binding.buttonLogin.setEnabled(true);
                    updateLoginButtonState();

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (loginActivity != null) {
                            loginActivity.moveToMainAndFinish();
                        }
                    } else {
                        String msg = "로그인 실패";
                        if (task.getException() != null) {
                            msg += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(
                                requireContext(),
                                msg,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // 화면 빈 곳 터치했을 때 키보드 숨기기
    private void hideKeyboard() {
        if (getActivity() == null) return;

        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        View view = getActivity().getCurrentFocus();
        if (view == null) {
            view = new View(getActivity());
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
