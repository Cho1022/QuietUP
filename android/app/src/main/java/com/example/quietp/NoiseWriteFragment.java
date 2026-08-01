package com.example.quietp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class NoiseWriteFragment extends Fragment {

    private NoiseRepository noiseRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_noise_write, container, false);

        noiseRepository = new NoiseRepository();

        MaterialToolbar toolbar = v.findViewById(R.id.toolbarNoiseWrite);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).popBackStack();
            }
        });

        EditText inputRoom = v.findViewById(R.id.inputNoiseRoom);
        EditText inputType = v.findViewById(R.id.inputNoiseType);
        EditText inputMemo = v.findViewById(R.id.inputNoiseMemo);
        Button buttonSubmit = v.findViewById(R.id.buttonNoiseSubmit);

        buttonSubmit.setOnClickListener(view -> {
            String room = inputRoom.getText().toString().trim();
            String type = inputType.getText().toString().trim();
            String memo = inputMemo.getText().toString().trim();

            if (TextUtils.isEmpty(room) || TextUtils.isEmpty(type)) {
                Toast.makeText(getContext(), "호수와 소음 종류를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            buttonSubmit.setEnabled(false);

            noiseRepository.createReport(room, type, memo, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    buttonSubmit.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "신고가 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).popBackStack();
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "등록 실패: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        return v;
    }
}
