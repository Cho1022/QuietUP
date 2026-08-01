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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 중간 소음 알림 목록 + 작성 화면 이동
 */
public class AlertFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoiseAdapter adapter;

    // Firebase : /noiseReports
    private DatabaseReference noiseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_alert, container, false);

        noiseRef = FirebaseDatabase.getInstance()
                .getReference("noiseReports");

        // 툴바
        MaterialToolbar toolbar = v.findViewById(R.id.toolbarAlert);
        toolbar.setTitle("중간 소음 알림");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        // 리스트
        recyclerView = v.findViewById(R.id.recyclerViewNoise);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NoiseAdapter();
        recyclerView.setAdapter(adapter);

        // 글쓰기(신고) 버튼
        FloatingActionButton fab = v.findViewById(R.id.fabNoiseWrite);
        fab.setOnClickListener(view -> openNoiseWrite());

        // 목록 관찰
        observeNoiseList();

        return v;
    }

    // 소음 목록 리스너
    private void observeNoiseList() {
        // 시간 기준으로 정렬 (최근 순으로 보고 싶으면 코드에서 뒤집어도 됨)
        noiseRef.orderByChild("timeMillis")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<NoiseReport> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            NoiseReport r = child.getValue(NoiseReport.class);
                            if (r == null) continue;
                            if (r.id == null) {
                                r.id = child.getKey();
                            }
                            list.add(r);
                        }
                        adapter.setItems(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),
                                "알림 목록을 불러오지 못했습니다: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 신고 작성 화면으로 이동
    private void openNoiseWrite() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).replaceFragment(new NoiseWriteFragment());
        }
    }

    // 특정 신고 삭제
    private void deleteNoise(NoiseReport report) {
        if (report == null || report.id == null) return;

        noiseRef.child(report.id)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "알림이 삭제되었습니다.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "삭제 실패: " +
                                        (task.getException() != null
                                                ? task.getException().getMessage()
                                                : ""),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 날짜 포맷
    private static String formatTime(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    // ===========================
    //  RecyclerView Adapter
    // ===========================
    private class NoiseAdapter
            extends RecyclerView.Adapter<NoiseAdapter.NoiseViewHolder> {

        private final List<NoiseReport> items = new ArrayList<>();

        void setItems(List<NoiseReport> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public NoiseViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                  int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_noise_report, parent, false);
            return new NoiseViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull NoiseViewHolder holder,
                                     int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class NoiseViewHolder extends RecyclerView.ViewHolder {

            TextView textTitle;
            TextView textTime;
            TextView textMemo;
            View buttonDelete;   // 삭제 버튼(아이콘/텍스트뷰 등)

            NoiseViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textNoiseTitle);
                textTime  = itemView.findViewById(R.id.textNoiseTime);
                textMemo  = itemView.findViewById(R.id.textNoiseMemo);
                buttonDelete = itemView.findViewById(R.id.buttonNoiseDelete);
            }

            void bind(NoiseReport report) {
                // 🔹 여기서 NoiseReport 필드만 사용
                String title = report.room + " · " + report.noiseType;
                textTitle.setText(title);
                textTime.setText(formatTime(report.timeMillis));
                textMemo.setText(
                        report.memo != null ? report.memo : ""
                );

                buttonDelete.setOnClickListener(v -> deleteNoise(report));
            }
        }
    }
}
