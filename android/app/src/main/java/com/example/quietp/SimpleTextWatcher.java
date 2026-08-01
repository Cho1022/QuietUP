package com.example.quietp;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * afterTextChanged 에서 람다 하나만 실행하고 싶을 때 쓰는 간단 헬퍼
 */
public class SimpleTextWatcher implements TextWatcher {

    private final Runnable afterChanged;

    public SimpleTextWatcher(Runnable afterChanged) {
        this.afterChanged = afterChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (afterChanged != null) {
            afterChanged.run();
        }
    }
}
