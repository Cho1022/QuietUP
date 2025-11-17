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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ContentRepository contentRepository;
    private NoiseRepository noiseRepository;
    private UserInfoRepository userInfoRepository;

    private ContentModel topHotPost;                 // 인기글 1등
    private final List<ContentModel> hotPostList = new ArrayList<>(); // 순위용(필요시)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        contentRepository = new ContentRepository();
        noiseRepository   = new NoiseRepository();
        userInfoRepository = new UserInfoRepository();

        // ===== 상단 아파트 이름 =====
        TextView tvAptName = v.findViewById(R.id.textHomeApartmentName);
        loadApartmentName(tvAptName);

        // ===== 즐겨찾는 게시판 텍스트뷰 =====
        TextView tvFree   = v.findViewById(R.id.textHomeFavoriteBoard1);
        TextView tvSecret = v.findViewById(R.id.textHomeFavoriteBoard2);
        TextView tvGrad   = v.findViewById(R.id.textHomeFavoriteBoard3);
        TextView tvClub   = v.findViewById(R.id.textHomeFavoriteBoard4);
        TextView tvMore   = v.findViewById(R.id.textHomeFavoriteMore);

        // 각 게시판 화면으로 이동
        tvFree.setOnClickListener(view -> openBoard(Value.CATEGORY_FREE));
        tvSecret.setOnClickListener(view -> openBoard(Value.CATEGORY_SECRET));
        tvGrad.setOnClickListener(view -> openBoard(Value.CATEGORY_GRAD));
        tvClub.setOnClickListener(view -> openBoard(Value.CATEGORY_CLUB));
        tvMore.setOnClickListener(view -> openBoard(Value.CATEGORY_ALL));

        // "게시판명 · 최신글제목" 으로 한 줄 구성
        loadFavoriteBoardLine(Value.CATEGORY_FREE,   tvFree);
        loadFavoriteBoardLine(Value.CATEGORY_SECRET, tvSecret);
        loadFavoriteBoardLine(Value.CATEGORY_GRAD,   tvGrad);
        loadFavoriteBoardLine(Value.CATEGORY_CLUB,   tvClub);

        // ===== 층간 소음 알림 카드 =====
        View cardNoise = v.findViewById(R.id.cardNoiseAlert);
        TextView tvNoiseBody = v.findViewById(R.id.textHomeAlertBody);

        cardNoise.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).replaceFragment(new AlertFragment());
            }
        });

        // 최근 신고 1건만 카드에 표시
        loadLastNoise(tvNoiseBody);

        // ===== 실시간 인기 글 카드 =====
        TextView tvHotBody = v.findViewById(R.id.textHomeHotBody);

        // 텍스트 영역 클릭 시 1등 인기글로 이동
        tvHotBody.setOnClickListener(view -> {
            if (topHotPost != null) {
                openPost(topHotPost);
            } else {
                Toast.makeText(getContext(), "인기글이 아직 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 조회수 기준 상위 인기글 불러와서 1~3등 표시
        loadHotPosts(tvHotBody);

        return v;
    }

    // 내 아파트 이름 불러오기
    private void loadApartmentName(TextView targetView) {
        userInfoRepository.loadMyUserInfo(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserInfoModel info = snapshot.getValue(UserInfoModel.class);

                if (info != null && info.aptName != null && !info.aptName.isEmpty()) {
                    // 예: "e편한세상", "롯데캐슬 2단지"
                    targetView.setText(info.aptName);
                } else {
                    // 저장된 아파트가 없으면 기본 문구
                    targetView.setText("내 아파트");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 실패해도 그냥 원래 텍스트 유지
            }
        });
    }

    // 즐겨찾는 게시판 줄에 최신글 제목 붙이기
    private void loadFavoriteBoardLine(String category, TextView targetView) {
        contentRepository.observePostList(category, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ContentModel latest = null;

                for (DataSnapshot child : snapshot.getChildren()) {
                    ContentModel m = child.getValue(ContentModel.class);
                    if (m == null) continue;
                    if (m.id == null) m.id = child.getKey();

                    // createdAt 이 가장 큰(최근) 글만 채택
                    if (latest == null || m.createdAt > latest.createdAt) {
                        latest = m;
                    }
                }

                String boardLabel = Value.getCategoryLabel(category);

                if (latest == null) {
                    targetView.setText(boardLabel + "  ·  최신글 없음");
                } else {
                    String title = (latest.title != null && !latest.title.isEmpty())
                            ? latest.title
                            : "제목 없음";
                    targetView.setText(boardLabel + "  ·  " + title);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 실패해도 기본 텍스트 유지
            }
        });
    }

    // 최근 층간 소음 신고 1건을 홈 카드에 표시
    private void loadLastNoise(TextView tvNoiseBody) {
        noiseRepository.loadLastReport(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                NoiseReport report = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    report = child.getValue(NoiseReport.class);
                    if (report != null && report.id == null) {
                        report.id = child.getKey();
                    }
                }

                if (report == null) {
                    tvNoiseBody.setText("최근 알림이 없습니다.");
                    return;
                }

                String room = report.room != null ? report.room : "";
                String type = report.noiseType != null ? report.noiseType : "";
                String time = formatTime(report.timeMillis);

                tvNoiseBody.setText(room + " · " + type + " · " + time);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvNoiseBody.setText("알림을 불러오지 못했습니다.");
            }
        });
    }

    // 조회수 기준 인기글 상위 N개
    private void loadHotPosts(TextView tvHotBody) {
        contentRepository.loadTopPosts(10, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ContentModel> list = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    ContentModel m = child.getValue(ContentModel.class);
                    if (m == null) continue;
                    if (m.id == null) m.id = child.getKey();
                    list.add(m);
                }

                if (list.isEmpty()) {
                    tvHotBody.setText("아직 인기글이 없습니다.");
                    topHotPost = null;
                    hotPostList.clear();
                    return;
                }

                // 조회수 내림차순
                Collections.sort(list, new Comparator<ContentModel>() {
                    @Override
                    public int compare(ContentModel o1, ContentModel o2) {
                        return Long.compare(o2.viewCount, o1.viewCount);
                    }
                });

                hotPostList.clear();
                hotPostList.addAll(list);
                topHotPost = list.get(0);

                StringBuilder sb = new StringBuilder();
                int max = Math.min(3, list.size());
                for (int i = 0; i < max; i++) {
                    ContentModel m = list.get(i);
                    String categoryLabel = Value.getCategoryLabel(m.category);
                    String title = (m.title != null && !m.title.isEmpty())
                            ? m.title
                            : "제목 없음";

                    // 1줄 : 순위 + 카테고리 + 제목
                    sb.append(i + 1)
                            .append("등 [")
                            .append(categoryLabel)
                            .append("] ")
                            .append(title)
                            .append("\n");

                    // 2줄 : 조회수
                    sb.append("조회수 ")
                            .append(m.viewCount)
                            .append("회");

                    if (i < max - 1) sb.append("\n\n"); // 글들 사이 한 줄 띄우기
                }

                tvHotBody.setText(sb.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvHotBody.setText("인기글을 불러오지 못했습니다.");
            }
        });
    }

    private void openBoard(String category) {
        if (getActivity() instanceof MainActivity) {
            PostListFragment fragment = new PostListFragment();
            Bundle b = new Bundle();
            b.putString("category", category);
            fragment.setArguments(b);

            ((MainActivity) getActivity()).replaceFragment(fragment);
        }
    }

    private void openPost(ContentModel model) {
        if (getActivity() instanceof MainActivity) {
            PostReadFragment fragment = new PostReadFragment();
            Bundle b = new Bundle();
            b.putString("postId", model.id);
            b.putString("category", model.category != null ? model.category : Value.CATEGORY_ALL);
            fragment.setArguments(b);

            ((MainActivity) getActivity()).replaceFragment(fragment);
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
