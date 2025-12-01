package com.example.quietp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quietp.databinding.FragmentPostModifyBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

/**
 * 게시글 수정 화면
 */
public class PostModifyFragment extends Fragment {

    private FragmentPostModifyBinding binding;
    private ContentRepository contentRepository;

    private String postId;
    private String category;

    private ContentModel currentPost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPostModifyBinding.inflate(inflater, container, false);
        contentRepository = new ContentRepository();

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
            category = getArguments().getString("category", Value.CATEGORY_ALL);
        }

        setupToolbar();
        loadPost();

        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.toolbarPostModify.setTitle("글 수정");

        binding.toolbarPostModify.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbarPostModify.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        binding.toolbarPostModify.inflateMenu(R.menu.post_modify_main_menu);
        binding.toolbarPostModify.setOnMenuItemClickListener(this::onToolbarMenuClick);
    }

    private boolean onToolbarMenuClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_post_modify_done) {
            saveModifiedPost();
            return true;
        }
        return false;
    }

    private void loadPost() {
        if (postId == null) {
            Toast.makeText(getContext(), "잘못된 접근입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        contentRepository.loadPost(postId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ContentModel model = snapshot.getValue(ContentModel.class);
                if (model == null) {
                    Toast.makeText(getContext(), "글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).popBackStack();
                    }
                    return;
                }

                if (model.id == null) {
                    model.id = snapshot.getKey();
                }
                currentPost = model;

                binding.inputPostModifyTitle.setText(model.getTitle());
                binding.inputPostModifyContent.setText(model.getBody());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "불러오기 실패: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveModifiedPost() {
        if (currentPost == null) {
            Toast.makeText(getContext(), "수정할 글이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newTitle = binding.inputPostModifyTitle.getText().toString().trim();
        String newBody = binding.inputPostModifyContent.getText().toString().trim();

        if (newTitle.isEmpty() || newBody.isEmpty()) {
            Toast.makeText(getContext(), "제목과 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPost.title = newTitle;
        currentPost.body = newBody;

        contentRepository.updatePost(currentPost, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "수정되었습니다.", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).popBackStack();
                }
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "";
                Toast.makeText(getContext(),
                        "수정 실패: " + msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
