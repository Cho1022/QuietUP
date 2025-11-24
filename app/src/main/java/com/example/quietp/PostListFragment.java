package com.example.quietp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 게시글 목록 화면
 * - category 에 따라 글 목록을 Firebase 에서 불러와 리사이클러뷰에 표시
 * - 우측 아래 FAB 로 글 작성 화면으로 이동
 */
public class PostListFragment extends Fragment {

    private String category = Value.CATEGORY_ALL;

    private ContentRepository contentRepository;
    private RecyclerView recyclerView;
    private PostListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_post_list, container, false);

        // 어느 게시판인지 전달받기
        if (getArguments() != null) {
            category = getArguments().getString("category", Value.CATEGORY_ALL);
        }

        contentRepository = new ContentRepository();

        // 툴바 세팅
        MaterialToolbar toolbar = v.findViewById(R.id.toolbarPostList);
        toolbar.setTitle(Value.getCategoryLabel(category));
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        // 리사이클러뷰 세팅
        recyclerView = v.findViewById(R.id.recyclerViewPostList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostListAdapter();
        recyclerView.setAdapter(adapter);

        // 글쓰기 버튼(FAB)
        FloatingActionButton fab = v.findViewById(R.id.fabPostWrite);
        fab.setOnClickListener(view -> openWriteFragment());

        // 목록 불러오기
        loadPostList();

        return v;
    }

    private void openWriteFragment() {
        PostWriteFragment fragment = new PostWriteFragment();
        Bundle b = new Bundle();
        b.putString("category", category);
        fragment.setArguments(b);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).replaceFragment(fragment);
        }
    }

    private void loadPostList() {
        contentRepository.observePostList(category, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ContentModel> list = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    ContentModel m = child.getValue(ContentModel.class);
                    if (m == null) continue;

                    if (m.id == null) {
                        m.id = child.getKey();
                    }
                    list.add(m);
                }

                adapter.setItems(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "목록 불러오기 실패: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===========================
    //  RecyclerView Adapter
    // ===========================
    private class PostListAdapter extends RecyclerView.Adapter<PostListAdapter.PostViewHolder> {

        private final List<ContentModel> items = new ArrayList<>();

        void setItems(List<ContentModel> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_post_list, parent, false);
            return new PostViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            ContentModel item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {

            TextView textTitle;
            TextView textWriter;
            TextView textCategory;
            TextView textDate;

            public PostViewHolder(@NonNull View itemView) {
                super(itemView);

                // 🔹 row_post_list.xml 의 id 와 정확히 맞춤
                textTitle    = itemView.findViewById(R.id.textViewRowPostTitle);
                textWriter   = itemView.findViewById(R.id.textViewRowPostUserName);
                textCategory = itemView.findViewById(R.id.textViewRowPostBoardType);
                textDate     = itemView.findViewById(R.id.textViewRowPostDate);
            }

            void bind(ContentModel model) {
                // 제목, 작성자
                textTitle.setText(model.getTitle());
                textWriter.setText(model.getWriterName());

                // 카테고리 라벨 (자유게시판 / 비밀게시판 ...)
                textCategory.setText(Value.getCategoryLabel(model.getCategory()));

                // 날짜
                textDate.setText(formatDate(model.getCreatedAt()));

                // 한 줄 클릭하면 글 읽기 화면으로 이동
                itemView.setOnClickListener(v -> openReadFragment(model));
            }
        }
    }

    private void openReadFragment(ContentModel model) {
        PostReadFragment fragment = new PostReadFragment();
        Bundle b = new Bundle();
        b.putString("postId", model.id);
        b.putString("category", category);
        fragment.setArguments(b);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).replaceFragment(fragment);
        }
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
