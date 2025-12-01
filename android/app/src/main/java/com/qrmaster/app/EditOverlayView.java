package com.qrmaster.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.View;
import android.view.MotionEvent;
import android.util.Log;

import java.util.List;

/**
 * ✅ EDIT OVERLAY - SADECE DÜZENLE MODUNDA
 * - Noktaları sürükle
 * - Normal modda yok
 */
public class EditOverlayView extends View {
    
    private static final String TAG = "EditOverlayView";
    private List<AutoClickerService.ClickAction> actions;
    
    private int draggedIndex = -1;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;
    private static final float TOUCH_RADIUS = 120f;
    
    private Paint overlayPaint;
    private Paint textPaint;
    
    public interface OnPointMovedListener {
        void onPointMoved(int index, float x, float y);
    }
    
    public interface OnAutoCloseListener {
        void onAutoClose();
    }
    
    private OnPointMovedListener listener;
    private OnAutoCloseListener onAutoCloseListener;
    
    public EditOverlayView(Context context, List<AutoClickerService.ClickAction> actions) {
        super(context);
        this.actions = actions;
        
        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#22FF0000"));
        overlayPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(8, 0, 0, Color.BLACK);
        textPaint.setFakeBoldText(true);
    }
    
    public void setOnPointMovedListener(OnPointMovedListener listener) {
        this.listener = listener;
    }
    
    public void setOnAutoCloseListener(OnAutoCloseListener listener) {
        this.onAutoCloseListener = listener;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw red tint to indicate edit mode
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
        
        // Draw instruction text
        canvas.drawText("✏ NOKTLARI SÜRÜKLEY İN", getWidth() / 2f, 100, textPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    draggedIndex = findTouchedAction(x, y);
                    if (draggedIndex >= 0) {
                        AutoClickerService.ClickAction action = actions.get(draggedIndex);
                        dragOffsetX = x - action.x;
                        dragOffsetY = y - action.y;
                        
                        // Vibrate
                        android.os.Vibrator vibrator = (android.os.Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                        if (vibrator != null) {
                            vibrator.vibrate(30);
                        }
                        
                        Log.d(TAG, "✅ Started dragging #" + draggedIndex);
                        invalidate();
                        return true;
                    }
                    return true; // Consume all touches
                    
                case MotionEvent.ACTION_MOVE:
                    if (draggedIndex >= 0) {
                        AutoClickerService.ClickAction action = actions.get(draggedIndex);
                        float newX = x - dragOffsetX;
                        float newY = y - dragOffsetY;
                        
                        if (action.type == AutoClickerService.ActionType.SWIPE) {
                            float deltaX = newX - action.x;
                            float deltaY = newY - action.y;
                            action.x = newX;
                            action.y = newY;
                            action.x2 += deltaX;
                            action.y2 += deltaY;
                        } else {
                            action.x = newX;
                            action.y = newY;
                        }
                        
                        if (listener != null) {
                            listener.onPointMoved(draggedIndex, action.x, action.y);
                        }
                        
                        invalidate();
                        return true;
                    }
                    return true; // Consume all touches
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (draggedIndex >= 0) {
                        Log.d(TAG, "✅ Finished dragging #" + draggedIndex);
                        draggedIndex = -1;
                        invalidate();
                        
                        // AUTO CLOSE EDIT MODE after dragging
                        postDelayed(() -> {
                            if (onAutoCloseListener != null) {
                                onAutoCloseListener.onAutoClose();
                            }
                        }, 300); // Small delay for smooth UX
                        
                        return true;
                    }
                    return true; // Consume all touches
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in touch: " + e.getMessage());
        }
        return true; // Always consume
    }
    
    private int findTouchedAction(float x, float y) {
        try {
            if (actions == null || actions.isEmpty()) return -1;
            
            for (int i = 0; i < actions.size(); i++) {
                AutoClickerService.ClickAction action = actions.get(i);
                
                float dx = x - action.x;
                float dy = y - action.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                
                if (dist < TOUCH_RADIUS) {
                    return i;
                }
            }
            
            return -1;
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding action: " + e.getMessage());
            return -1;
        }
    }
}


