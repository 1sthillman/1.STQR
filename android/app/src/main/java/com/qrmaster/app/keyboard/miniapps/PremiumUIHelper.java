package com.qrmaster.app.keyboard.miniapps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

/**
 * 🎨 Premium UI Helper
 * 
 * Tüm mini apps için tutarlı, modern, şık UI bileşenleri
 */
public class PremiumUIHelper {
    
    // Premium Color Palette
    public static class Colors {
        // iOS-inspired colors
        public static final int BLUE = 0xFF007AFF;
        public static final int GREEN = 0xFF34C759;
        public static final int INDIGO = 0xFF5856D6;
        public static final int ORANGE = 0xFFFF9500;
        public static final int PINK = 0xFFFF2D55;
        public static final int PURPLE = 0xFFAF52DE;
        public static final int RED = 0xFFFF3B30;
        public static final int TEAL = 0xFF5AC8FA;
        public static final int YELLOW = 0xFFFFCC00;
        
        // Backgrounds
        public static final int BG_PRIMARY = 0xFF000000;
        public static final int BG_SECONDARY = 0xFF1C1C1E;
        public static final int BG_TERTIARY = 0xFF2C2C2E;
        public static final int BG_QUATERNARY = 0xFF3A3A3C;
        
        // Text
        public static final int TEXT_PRIMARY = 0xFFFFFFFF;
        public static final int TEXT_SECONDARY = 0xFF8E8E93;
        public static final int TEXT_TERTIARY = 0xFF636366;
        
        // Accents
        public static final int ACCENT_SUCCESS = 0xFF34C759;
        public static final int ACCENT_WARNING = 0xFFFF9500;
        public static final int ACCENT_ERROR = 0xFFFF3B30;
        public static final int ACCENT_INFO = 0xFF007AFF;
    }
    
    /**
     * Premium gradient button with glass morphism effect
     */
    public static Button createPremiumButton(Context context, String text, int startColor, int endColor) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Colors.TEXT_PRIMARY);
        btn.setTextSize(15);
        btn.setAllCaps(false);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 14));
        
        // Gradient background
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{startColor, endColor}
        );
        gradient.setCornerRadius(dp(context, 12));
        gradient.setStroke(dp(context, 1), addAlpha(Colors.TEXT_PRIMARY, 0.1f));
        
        // Ripple effect
        GradientDrawable rippleMask = new GradientDrawable();
        rippleMask.setCornerRadius(dp(context, 12));
        rippleMask.setColor(Color.WHITE);
        
        RippleDrawable ripple = new RippleDrawable(
            android.content.res.ColorStateList.valueOf(addAlpha(Color.WHITE, 0.2f)),
            gradient,
            rippleMask
        );
        
        btn.setBackground(ripple);
        btn.setElevation(dp(context, 8));
        btn.setTranslationZ(dp(context, 4));
        btn.setStateListAnimator(null); // Disable default animator
        
        // Custom press animation
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
        
        return btn;
    }
    
    /**
     * Solid color premium button
     */
    public static Button createSolidButton(Context context, String text, int color) {
        return createPremiumButton(context, text, color, darken(color, 0.1f));
    }
    
    /**
     * Outlined button (ghost style)
     */
    public static Button createOutlinedButton(Context context, String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(color);
        btn.setTextSize(15);
        btn.setAllCaps(false);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 14));
        
        GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(dp(context, 12));
        outline.setStroke(dp(context, 2), color);
        outline.setColor(Color.TRANSPARENT);
        
        // Ripple effect
        GradientDrawable rippleMask = new GradientDrawable();
        rippleMask.setCornerRadius(dp(context, 12));
        rippleMask.setColor(Color.WHITE);
        
        RippleDrawable ripple = new RippleDrawable(
            android.content.res.ColorStateList.valueOf(addAlpha(color, 0.2f)),
            outline,
            rippleMask
        );
        
        btn.setBackground(ripple);
        
        return btn;
    }
    
    /**
     * Icon button (circular)
     */
    public static Button createIconButton(Context context, String icon, int color) {
        Button btn = new Button(context);
        btn.setText(icon);
        btn.setTextColor(Colors.TEXT_PRIMARY);
        btn.setTextSize(24);
        btn.setPadding(0, 0, 0, 0);
        
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        circle.setStroke(dp(context, 1), addAlpha(Colors.TEXT_PRIMARY, 0.1f));
        
        btn.setBackground(circle);
        btn.setElevation(dp(context, 6));
        
        return btn;
    }
    
    /**
     * Premium card container
     */
    public static GradientDrawable createCardBackground(Context context) {
        GradientDrawable card = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Colors.BG_TERTIARY, Colors.BG_SECONDARY}
        );
        card.setCornerRadius(dp(context, 20));
        card.setStroke(dp(context, 1), addAlpha(Colors.TEXT_PRIMARY, 0.08f));
        return card;
    }
    
    /**
     * Glass morphism container
     */
    public static GradientDrawable createGlassBackground(Context context, int tintColor) {
        GradientDrawable glass = new GradientDrawable();
        glass.setColor(addAlpha(tintColor, 0.15f));
        glass.setCornerRadius(dp(context, 16));
        glass.setStroke(dp(context, 1), addAlpha(Colors.TEXT_PRIMARY, 0.1f));
        return glass;
    }
    
    /**
     * Status indicator (dot)
     */
    public static GradientDrawable createStatusDot(Context context, int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        return dot;
    }
    
    /**
     * Premium text with gradient (requires TextPaint)
     */
    public static void applyGradientText(TextView textView, int startColor, int endColor) {
        textView.post(() -> {
            float width = textView.getPaint().measureText(textView.getText().toString());
            Shader shader = new LinearGradient(
                0, 0, width, 0,
                startColor, endColor,
                Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
    }
    
    // Helper methods
    
    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
    
    private static int addAlpha(int color, float alpha) {
        int alphaInt = Math.round(alpha * 255);
        return (alphaInt << 24) | (color & 0x00FFFFFF);
    }
    
    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(r, g, b);
    }
    
    private static int lighten(int color, float factor) {
        int r = (int) (Color.red(color) + (255 - Color.red(color)) * factor);
        int g = (int) (Color.green(color) + (255 - Color.green(color)) * factor);
        int b = (int) (Color.blue(color) + (255 - Color.blue(color)) * factor);
        return Color.rgb(r, g, b);
    }
}








