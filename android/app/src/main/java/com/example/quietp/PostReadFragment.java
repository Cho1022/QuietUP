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

import com.example.quietp.databinding.FragmentPostReadBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 게시글 읽기 화면
 */
public class PostReadFragment extends Fragment {

    private FragmentPostReadBinding binding;
    private ContentRepository contentRepository;

    private String postId;
    private String category;

    private ContentModel currentPost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPostReadBinding.inflate(inflater, container, false);
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
        binding.toolbarPostRead.setTitle("게시글 보기");

        binding.toolbarPostRead.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbarPostRead.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        binding.toolbarPostRead.inflateMenu(R.menu.post_read_main_menu);
        binding.toolbarPostRead.setOnMenuItemClickListener(this::onToolbarMenuClick);
    }

    private boolean onToolbarMenuClick(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuItemPostReadModify) {
            if (postId == null || currentPost == null) return true;

            PostModifyFragment fragment = new PostModifyFragment();
            Bundle b = new Bundle();
            b.putString("postId", postId);
            b.putString("category", category);
            fragment.setArguments(b);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).replaceFragment(fragment);
            }
            return true;

        } else if (id == R.id.menuItemPostReadDelete) {
            deletePost();
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

                binding.inputPostReadType.setText(
                        Value.getCategoryLabel(model.category));
                binding.inputPostReadSubject.setText(model.getTitle());
                binding.inputPostReadNickname.setText(model.getWriterName());
                binding.inputPostReadDate.setText(formatDate(model.createdAt));
                binding.inputPostReadText.setText(model.getBody());

                // 조회수 증가
                if (model.id != null) {
                    contentRepository.increaseViewCount(model.id);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "불러오기 실패: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deletePost() {
        if (postId == null || currentPost == null) {
            Toast.makeText(getContext(), "삭제할 글이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        contentRepository.deletePost(postId, currentPost.writerUid,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).popBackStack();
                            }
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage() : "";
                            Toast.makeText(getContext(),
                                    "삭제 실패: " + msg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
