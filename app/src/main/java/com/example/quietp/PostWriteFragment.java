package com.example.quietp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentPostWriteBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 글 작성 화면
 * - 제목 / 내용 입력해서 Firebase Realtime DB 에 저장
 */
public class PostWriteFragment extends Fragment {

    private FragmentPostWriteBinding binding;
    private ContentRepository contentRepository;

    // 어떤 게시판인지 (자유 / 비밀 / 졸업 / 새내기 / 전체)
    private String category = Value.CATEGORY_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPostWriteBinding.inflate(inflater, container, false);
        contentRepository = new ContentRepository();

        // 어느 게시판에서 왔는지 인자 받기 (없으면 전체)
        if (getArguments() != null) {
            category = getArguments().getString("category", Value.CATEGORY_ALL);
        }

        setupToolbar();

        return binding.getRoot();
    }

    private void setupToolbar() {
        // 제목: "자유게시판 글 작성" 이런 느낌으로 표시
        String title = Value.getCategoryLabel(category) + " 글 작성";
        binding.toolbarPostWrite.setTitle(title);

        // 뒤로가기
        binding.toolbarPostWrite.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbarPostWrite.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        // 상단 메뉴(완료 버튼) 붙이기
        binding.toolbarPostWrite.inflateMenu(R.menu.post_write_main_menu);
        binding.toolbarPostWrite.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_post_write_done) {
                savePost();
                return true;
            }
            return false;
        });
    }

    /**
     * 실제 글 저장 로직
     */
    private void savePost() {
        String title = binding.inputPostWriteTitle.getText().toString().trim();
        String body  = binding.inputPostWriteContent.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(body)) {
            Toast.makeText(getContext(), "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 작성자 이름(지금은 이메일, 없으면 "익명")
        String writerName = "익명";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            writerName = user.getEmail();
        }

        // 🔹 ContentModel 인스턴스를 기본 생성자로 만들고 필드 직접 채우기
        ContentModel model = new ContentModel();
        model.id         = null;            // 키는 Repository 에서 push().getKey() 로 채움
        model.title      = title;
        model.body       = body;
        model.imageUrl   = null;            // 지금은 이미지 사용 안 함
        model.writerUid  = (user != null) ? user.getUid() : null;
        model.writerName = writerName;
        model.category   = category;
        model.createdAt  = 0;               // Repository 에서 현재 시간으로 채움
        model.updatedAt  = 0;

        // 버튼 중복 클릭 방지
        binding.toolbarPostWrite.getMenu().findItem(R.id.menu_post_write_done).setEnabled(false);

        contentRepository.createPost(model, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // 다시 버튼 활성화
                binding.toolbarPostWrite.getMenu().findItem(R.id.menu_post_write_done).setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    // 저장 성공하면 뒤로(목록)으로
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).popBackStack();
                    }
                } else {
                    String msg = (task.getException() != null)
                            ? task.getException().getMessage()
                            : "알 수 없는 오류";
                    Toast.makeText(getContext(), "등록 실패: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
