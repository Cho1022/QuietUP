package com.example.quietp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentAddUserInfoBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AddUserInfoFragment extends Fragment {

    private FragmentAddUserInfoBinding binding;
    private LoginActivity loginActivity;
    private UserInfoRepository userRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentAddUserInfoBinding.inflate(inflater, container, false);
        loginActivity = (LoginActivity) getActivity();
        userRepo = new UserInfoRepository();

        setupToolbar();
        setupCheckBoxAll();
        setupButton();

        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.toolbarAddUserInfo.setTitle("추가 정보 입력");
        binding.toolbarAddUserInfo.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbarAddUserInfo.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    // 아파트 선택 전체 체크
    private void setupCheckBoxAll() {
        CheckBox[] aptChecks = {
                binding.checkBoxAddUserInfo1,
                binding.checkBoxAddUserInfo2,
                binding.checkBoxAddUserInfo3,
                binding.checkBoxAddUserInfo4,
                binding.checkBoxAddUserInfo5,
                binding.checkBoxAddUserInfo6
        };

        binding.checkBoxAddUserInfoAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox cb : aptChecks) {
                cb.setChecked(isChecked);
            }
        });
    }

    private void setupButton() {
        binding.buttonAddUserInfoSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String nickname = binding.inputAddUserInfoNickname.getText().toString().trim();
        String ageStr   = binding.inputAddUserInfoAge.getText().toString().trim();

        if (TextUtils.isEmpty(nickname) || TextUtils.isEmpty(ageStr)) {
            Toast.makeText(getContext(), "닉네임과 나이를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "나이는 숫자로 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 체크된 아파트들 전부 모으기
        List<String> aptList = new ArrayList<>();
        if (binding.checkBoxAddUserInfo1.isChecked())
            aptList.add(binding.checkBoxAddUserInfo1.getText().toString());
        if (binding.checkBoxAddUserInfo2.isChecked())
            aptList.add(binding.checkBoxAddUserInfo2.getText().toString());
        if (binding.checkBoxAddUserInfo3.isChecked())
            aptList.add(binding.checkBoxAddUserInfo3.getText().toString());
        if (binding.checkBoxAddUserInfo4.isChecked())
            aptList.add(binding.checkBoxAddUserInfo4.getText().toString());
        if (binding.checkBoxAddUserInfo5.isChecked())
            aptList.add(binding.checkBoxAddUserInfo5.getText().toString());
        if (binding.checkBoxAddUserInfo6.isChecked())
            aptList.add(binding.checkBoxAddUserInfo6.getText().toString());

        // 대표 아파트: 선택된 것들 중 첫 번째(없으면 “아파트 미선택”)
        String aptName = aptList.isEmpty()
                ? "아파트 미선택"
                : aptList.get(0);

        // ★ UserInfoModel 생성 (현재 클래스 정의에 맞춰 이미 사용 중이던 그대로 유지)
        UserInfoModel model =
                new UserInfoModel(null, null, nickname, age, aptName, aptList);

        binding.buttonAddUserInfoSubmit.setEnabled(false);

        userRepo.saveMyUserInfo(model, task -> {
            binding.buttonAddUserInfoSubmit.setEnabled(true);

            if (task.isSuccessful()) {
                Toast.makeText(
                        getContext(),
                        "회원 정보 저장 완료. 다시 로그인 해주세요.",
                        Toast.LENGTH_SHORT
                ).show();

                // 1) 방금 가입한 계정은 로그아웃
                FirebaseAuth.getInstance().signOut();

                // 2) 로그인 화면으로 돌아가기 (LoginActivity 안에서 프래그먼트 교체)
                if (loginActivity == null && getActivity() instanceof LoginActivity) {
                    loginActivity = (LoginActivity) getActivity();
                }
                if (loginActivity != null) {
                    // 백스택에 쌓지 않고 로그인 화면으로
                    loginActivity.replaceFragment(LoginActivity.FRAG_LOGIN, false);
                }

            } else {
                Toast.makeText(
                        getContext(),
                        "저장 실패: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
