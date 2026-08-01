package com.example.quietp;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomFragment extends Fragment {

    private static final String ARG_ROOM_ID   = "roomId";
    private static final String ARG_ROOM_NAME = "roomName";

    private String roomId;
    private String roomName;

    private ChatRepository chatRepository;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText inputMessage;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_chat_room, container, false);

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
            roomName = getArguments().getString(ARG_ROOM_NAME, "");
        }

        chatRepository = new ChatRepository();

        // 상단 툴바
        MaterialToolbar toolbar = v.findViewById(R.id.toolbarChatRoom);
        toolbar.setTitle("익명 채팅 - " + roomName);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        // 메시지 리스트
        recyclerView = v.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);

        // 입력 + 전송 버튼
        inputMessage = v.findViewById(R.id.inputChatMessage);
        Button buttonSend = v.findViewById(R.id.buttonChatSend);
        buttonSend.setOnClickListener(view -> sendMessage());

        // 빈 공간 터치 시 키보드 내리기
        setupHideKeyboard(v);

        // 메시지 스트림 구독
        observeMessages();

        return v;
    }

    // 리스트·배경 터치 시 키보드 숨기기
    private void setupHideKeyboard(View root) {
        View.OnTouchListener hideListener = (view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard(view);
            }
            return false;
        };

        recyclerView.setOnTouchListener(hideListener);
        root.setOnTouchListener(hideListener);
    }

    private void hideKeyboard(View view) {
        if (getContext() == null) return;
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // ---------------- 메시지 실시간 구독 ----------------
    private void observeMessages() {
        if (roomId == null) return;

        chatRepository.observeMessages(roomId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage msg = child.getValue(ChatMessage.class);
                    if (msg == null) continue;
                    if (msg.id == null) msg.id = child.getKey();
                    list.add(msg);
                }
                adapter.setItems(list);
                recyclerView.scrollToPosition(Math.max(list.size() - 1, 0));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "메시지를 불러오지 못했습니다: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- 메시지 전송 ----------------
    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String nickname = "익명";

        chatRepository.sendMessage(roomId, nickname, text, task -> {
            if (task.isSuccessful()) {
                inputMessage.setText("");
            } else {
                Toast.makeText(getContext(),
                        "메시지 전송 실패",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- RecyclerView Adapter ----------------
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private final List<ChatMessage> items = new ArrayList<>();

        void setItems(List<ChatMessage> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_chat_message, parent, false);
            return new MessageViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {

            TextView textNickname;
            TextView textBody;
            TextView textTime;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                // ★ XML 의 id 와 반드시 일치해야 함
                textNickname = itemView.findViewById(R.id.textMessageNickname);
                textBody     = itemView.findViewById(R.id.textMessageBody);
                textTime     = itemView.findViewById(R.id.textMessageTime);
            }

            void bind(ChatMessage msg) {
                String nick = (msg.nickname != null && !msg.nickname.isEmpty())
                        ? msg.nickname
                        : "익명";

                textNickname.setText(nick);
                textBody.setText(msg.message != null ? msg.message : "");
                textTime.setText(timeFormat.format(new Date(msg.timeMillis)));
            }
        }
    }
}
