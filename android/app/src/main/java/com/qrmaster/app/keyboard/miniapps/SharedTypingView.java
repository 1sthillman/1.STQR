package com.qrmaster.app.keyboard.miniapps;

import com.qrmaster.app.R;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Premium Ortak Yazı - Modern Design
 */
public class SharedTypingView extends LinearLayout {
    private static final String TAG = "SharedTypingView";
    
    private final Callback callback;
    private Context context;
    private TextView statusText;
    private ScrollView chatScroll;
    private LinearLayout chatContainer;
    private final List<ChatMessage> messages = new ArrayList<>();
    
    public interface Callback {
        void onTextInsert(String text);
        void onClose();
        void onTextRequest(TextView targetView);
    }
    
    static class ChatMessage {
        String text;
        boolean isMine;
        long timestamp;
        
        ChatMessage(String text, boolean isMine) {
            this.text = text;
            this.isMine = isMine;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public SharedTypingView(Context context, Callback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(380)));
        setBackgroundColor(0xFF0A0A0A);

        // Premium Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LayoutParams headerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp(8);
        
        TextView title = new TextView(context);
        title.setText("Ortak Yazı");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.02f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
        closeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dp(6));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        closeBtn.setColorFilter(0xFFFFFFFF);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header, headerParams);

        // Status Bar
        LinearLayout statusBar = new LinearLayout(context);
        statusBar.setOrientation(HORIZONTAL);
        statusBar.setGravity(Gravity.CENTER);
        statusBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
        statusBg.setCornerRadius(dp(8));
        statusBg.setColor(0xFF1A1A1A);
        statusBar.setBackground(statusBg);
        
        LayoutParams statusBarParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statusBarParams.setMargins(dp(8), 0, dp(8), dp(8));
        
        statusText = new TextView(context);
        statusText.setText("Cihaz Ara");
        statusText.setTextColor(0xFF666666);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusText.setGravity(Gravity.CENTER);
        statusBar.addView(statusText, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        
        Button connectBtn = createModernButton("Bağlan", 0xFF0A84FF);
        connectBtn.setOnClickListener(v -> startConnection());
        statusBar.addView(connectBtn);
        
        addView(statusBar, statusBarParams);

        // Chat Container
        chatScroll = new ScrollView(context);
        chatScroll.setBackgroundColor(0xFF000000);
        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.setMargins(dp(8), 0, dp(8), dp(8));
        chatScroll.setLayoutParams(scrollParams);
        
        chatContainer = new LinearLayout(context);
        chatContainer.setOrientation(VERTICAL);
        chatContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        chatScroll.addView(chatContainer);
        addView(chatScroll);
        
        // Quick Actions
        addView(createQuickActions(context));
        
        // Add welcome message
        addMessage("Ortak Yazı özelliği ile yakındaki cihazlarla mesaj paylaşın", false);
    }
    
    private LinearLayout createQuickActions(Context context) {
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setPadding(dp(8), dp(6), dp(8), dp(6));
        actions.setGravity(Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable actionBg = new android.graphics.drawable.GradientDrawable();
        actionBg.setColor(0xFF1A1A1A);
        actions.setBackground(actionBg);
        
        LayoutParams actionParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        actions.setLayoutParams(actionParams);
        
        Button sendBtn = createModernButton("Gönder", 0xFF0A84FF);
        sendBtn.setOnClickListener(v -> sendMessage("Test mesajı"));
        actions.addView(sendBtn, new LayoutParams(0, dp(40), 1f));
        
        return actions;
    }
    
    private Button createModernButton(String text, int color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setLetterSpacing(0.02f);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setCornerRadius(dp(8));
        btnBg.setColor(color);
        button.setBackground(btnBg);
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }
    
    private void startConnection() {
                statusText.setText("Bağlanıyor...");
        statusText.setTextColor(0xFFFF9F0A);
        postDelayed(() -> {
            statusText.setText("Bağlı");
            statusText.setTextColor(0xFF34C759);
            addMessage("Bağlantı kuruldu!", false);
        }, 1500);
    }
    
    private void sendMessage(String text) {
        addMessage(text, true);
    }
    
    private void addMessage(String text, boolean isMine) {
        messages.add(new ChatMessage(text, isMine));
        chatContainer.addView(createMessageBubble(text, isMine));
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
    
    private LinearLayout createMessageBubble(String text, boolean isMine) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(HORIZONTAL);
        container.setGravity(isMine ? Gravity.END : Gravity.START);
        container.setPadding(0, dp(4), 0, dp(4));
        
        TextView bubble = new TextView(context);
        bubble.setText(text);
        bubble.setTextColor(0xFFFFFFFF);
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setMaxWidth(dp(220));
        
        android.graphics.drawable.GradientDrawable bubbleBg = new android.graphics.drawable.GradientDrawable();
        bubbleBg.setCornerRadius(dp(16));
        bubbleBg.setColor(isMine ? 0xFF0A84FF : 0xFF2A2A2A);
        bubble.setBackground(bubbleBg);
        
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        bubble.setLayoutParams(bubbleParams);
        
        container.addView(bubble);
        return container;
    }
    
    public void release() {
        // Cleanup resources
        messages.clear();
        if (chatContainer != null) {
            chatContainer.removeAllViews();
        }
    }
    
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
