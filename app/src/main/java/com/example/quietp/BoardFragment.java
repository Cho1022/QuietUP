// app/src/main/java/com/example/quietp/BoardFragment.java
package com.example.quietp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BoardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_board, container, false);

        v.findViewById(R.id.rowBoardFree2).setOnClickListener(view -> openBoard(Value.CATEGORY_FREE));
        v.findViewById(R.id.rowBoardSecret2).setOnClickListener(view -> openBoard(Value.CATEGORY_SECRET));
        v.findViewById(R.id.rowBoardGrad2).setOnClickListener(view -> openBoard(Value.CATEGORY_GRAD));
        v.findViewById(R.id.rowBoardClub2).setOnClickListener(view -> openBoard(Value.CATEGORY_CLUB));

        return v;
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
}
