package com.qrmaster.app.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qrmaster.app.R;

import java.util.List;

public class CandidateStripView extends LinearLayout {

    public interface OnSuggestionClickListener {
        void onSuggestionClicked(String suggestion);
    }

    private TextView[] suggestionViews = new TextView[5]; // 3'ten 5'e çıkarıldı
    private OnSuggestionClickListener listener;

    public CandidateStripView(Context context) {
        super(context);
        init(context);
    }

    public CandidateStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CandidateStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.keyboard_candidate_strip, this, true);
        suggestionViews[0] = findViewById(R.id.suggestion1);
        suggestionViews[1] = findViewById(R.id.suggestion2);
        suggestionViews[2] = findViewById(R.id.suggestion3);
        suggestionViews[3] = findViewById(R.id.suggestion4);
        suggestionViews[4] = findViewById(R.id.suggestion5);
        for (TextView textView : suggestionViews) {
            if (textView != null) { // Null check
                textView.setOnClickListener(v -> {
                    if (listener != null) {
                        String text = ((TextView) v).getText().toString();
                        if (!text.isEmpty()) {
                            listener.onSuggestionClicked(text);
                        }
                    }
                });
            }
        }
        hideAll();
    }

    public void setListener(OnSuggestionClickListener suggestionClickListener) {
        this.listener = suggestionClickListener;
    }

    public void setSuggestions(List<String> suggestions) {
        hideAll();
        if (suggestions == null) {
            return;
        }
        int count = Math.min(suggestions.size(), suggestionViews.length);
        for (int i = 0; i < count; i++) {
            TextView view = suggestionViews[i];
            view.setText(suggestions.get(i));
            view.setVisibility(View.VISIBLE);
        }
    }

    public void hideAll() {
        for (TextView view : suggestionViews) {
            view.setText("");
            view.setVisibility(View.INVISIBLE);
        }
    }
}

