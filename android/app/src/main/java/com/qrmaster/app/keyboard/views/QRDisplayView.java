package com.qrmaster.app.keyboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔑 QR Kod Gösterici
 * Public key'i QR kod olarak gösterir
 */
public class QRDisplayView extends LinearLayout {
    private ImageView qrImageView;
    private TextView instructionText;
    private Button closeBtn;
    
    private Callback callback;
    
    public interface Callback {
        void onClose();
    }
    
    public QRDisplayView(Context context) {
        super(context);
        init(context);
    }
    
    public QRDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(VERTICAL);
        setBackgroundColor(0xE0000000); // Semi-transparent
        setGravity(Gravity.CENTER);
        setPadding(dp(24), dp(24), dp(24), dp(24));
        
        // Container
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        container.setBackgroundColor(0xFF1C1C1E);
        container.setPadding(dp(20), dp(20), dp(20), dp(20));
        container.setGravity(Gravity.CENTER);
        
        android.graphics.drawable.GradientDrawable containerBg = 
            new android.graphics.drawable.GradientDrawable();
        containerBg.setColor(0xFF1C1C1E);
        containerBg.setCornerRadius(dp(16));
        containerBg.setStroke(dp(2), 0xFF9B51E0);
        container.setBackground(containerBg);
        
        // Title
        TextView title = new TextView(context);
        title.setText("🔑 Anahtarınızı Paylaşın");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        container.addView(title);
        
        // QR Image
        qrImageView = new ImageView(context);
        qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams qrParams = new LayoutParams(dp(280), dp(280));
        qrParams.topMargin = dp(16);
        qrParams.bottomMargin = dp(16);
        qrImageView.setLayoutParams(qrParams);
        container.addView(qrImageView);
        
        // Instructions
        instructionText = new TextView(context);
        instructionText.setText("Karşı tarafın 📷 Tara butonuna\nbasarak bu QR kodu okutmasını isteyin.\n\n⏱️ 30 saniye içinde geçerli");
        instructionText.setTextColor(0xFFBBBBBB);
        instructionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        instructionText.setGravity(Gravity.CENTER);
        container.addView(instructionText);
        
        // Close button
        closeBtn = new Button(context);
        closeBtn.setText("Kapat");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        closeBtn.setPadding(dp(32), dp(12), dp(32), dp(12));
        
        android.graphics.drawable.GradientDrawable closeBg = 
            new android.graphics.drawable.GradientDrawable();
        closeBg.setColor(0xFF9B51E0);
        closeBg.setCornerRadius(dp(10));
        closeBtn.setBackground(closeBg);
        
        closeBtn.setOnClickListener(v -> {
            if (callback != null) callback.onClose();
        });
        
        LayoutParams closeBtnParams = new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        );
        closeBtnParams.topMargin = dp(20);
        closeBtn.setLayoutParams(closeBtnParams);
        container.addView(closeBtn);
        
        addView(container);
        
        // Auto-close after 30 seconds
        postDelayed(() -> {
            if (callback != null) callback.onClose();
        }, 30000);
    }
    
    /**
     * QR kod göster
     */
    public void showQRCode(String data) {
        try {
            Bitmap qrBitmap = generateQRCode(data, 800, 800);
            qrImageView.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            android.util.Log.e("QRDisplayView", "QR generation error", e);
        }
    }
    
    /**
     * QR kod oluştur (ZXing)
     */
    private Bitmap generateQRCode(String data, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        
        return bitmap;
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            getContext().getResources().getDisplayMetrics()
        );
    }
}






