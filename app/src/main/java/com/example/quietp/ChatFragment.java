package com.example.quietp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class ChatFragment extends Fragment {

    private ChatRepository chatRepository;
    private RecyclerView recyclerView;
    private RoomAdapter adapter;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRepository = new ChatRepository();

        MaterialToolbar toolbar = v.findViewById(R.id.toolbarChat);
        toolbar.setTitle("익명 채팅방");

        recyclerView = v.findViewById(R.id.recyclerViewChatRooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RoomAdapter();
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = v.findViewById(R.id.fabAddRoom);
        fab.setOnClickListener(view -> showCreateRoomDialog());

        observeRoomList();

        return v;
    }

    // 방 목록 실시간 구독
    private void observeRoomList() {
        chatRepository.observeRoomList(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatRoom> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatRoom room = child.getValue(ChatRoom.class);
                    if (room == null) continue;
                    if (room.id == null) room.id = child.getKey();
                    list.add(room);
                }
                adapter.setItems(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "채팅방 목록을 불러오지 못했습니다: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 방 만들기 / 입장 다이얼로그
    private void showCreateRoomDialog() {
        if (getContext() == null) return;

        final EditText input = new EditText(getContext());
        input.setHint("방 이름을 입력하세요 (예: 301동 채팅방)");

        new AlertDialog.Builder(getContext())
                .setTitle("채팅방 입장 / 개설")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("입장", (dialog, which) -> {
                    String roomName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(roomName)) {
                        Toast.makeText(getContext(),
                                "방 이름을 입력해주세요.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createRoomAndEnter(roomName);
                })
                .show();
    }

    // 방 생성 후 바로 입장
    private void createRoomAndEnter(String roomName) {
        chatRepository.createRoom(roomName, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(getContext(),
                        "방 생성 실패",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String roomId = task.getResult();
            openChatRoom(roomId, roomName);
        });
    }

    // 방 리스트 중 하나 클릭 시
    private void openChatRoom(String roomId, String roomName) {
        if (!(getActivity() instanceof MainActivity)) return;

        ChatRoomFragment fragment = new ChatRoomFragment();
        Bundle b = new Bundle();
        b.putString("roomId", roomId);
        b.putString("roomName", roomName);
        fragment.setArguments(b);

        ((MainActivity) getActivity()).replaceFragment(fragment);
    }

    private class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

        private final List<ChatRoom> items = new ArrayList<>();

        void setItems(List<ChatRoom> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_room, parent, false);
            return new RoomViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class RoomViewHolder extends RecyclerView.ViewHolder {

            TextView textName;
            TextView textLastMessage;
            View buttonDelete;   // 휴지통 버튼

            RoomViewHolder(@NonNull View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.textRoomName);
                textLastMessage = itemView.findViewById(R.id.textRoomLastMessage);
                buttonDelete = itemView.findViewById(R.id.buttonRoomDelete);
            }

            void bind(ChatRoom room) {
                textName.setText(room.name != null ? room.name : "이름 없는 방");

                String last;
                if (room.lastMessageTime > 0 &&
                        room.lastMessage != null &&
                        !room.lastMessage.isEmpty()) {
                    last = timeFormat.format(new Date(room.lastMessageTime))
                            + " · " + room.lastMessage;
                } else {
                    last = "아직 메시지가 없습니다.";
                }
                textLastMessage.setText(last);

                // 카드 클릭 → 방 입장
                itemView.setOnClickListener(v ->
                        openChatRoom(room.id, room.name != null ? room.name : "")
                );

                // 휴지통 클릭 → 방 + 메시지 삭제
                buttonDelete.setOnClickListener(v -> {
                    if (getContext() == null) return;

                    new AlertDialog.Builder(getContext())
                            .setTitle("채팅방 삭제")
                            .setMessage("방 \"" + room.name + "\" 을(를) 삭제할까요?\n" +
                                    "대화 내용도 함께 삭제됩니다.")
                            .setNegativeButton("취소", null)
                            .setPositiveButton("삭제", (dialog, which) -> {
                                chatRepository.deleteRoom(room.id, task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(),
                                                "채팅방이 삭제되었습니다.",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(),
                                                "삭제 실패: " +
                                                        task.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .show();
                });
            }
        }
    }

}
