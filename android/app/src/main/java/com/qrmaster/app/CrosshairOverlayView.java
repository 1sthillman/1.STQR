package com.qrmaster.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.View;
import android.view.MotionEvent;
import android.util.Log;

/**
 * Crosshair overlay for precise point selection
 */
public class CrosshairOverlayView extends View {
    
    private static final String TAG = "CrosshairOverlay";
    private Paint crosshairPaint;
    private Paint circlePaint;
    private Paint textPaint;
    private float touchX = 0;
    private float touchY = 0;
    private boolean hasTouched = false;
    private OnPointSelectedListener listener;
    
    public interface OnPointSelectedListener {
        void onPointSelected(float x, float y);
    }
    
    public CrosshairOverlayView(Context context) {
        super(context);
        
        crosshairPaint = new Paint();
        crosshairPaint.setColor(Color.parseColor("#10B981"));
        crosshairPaint.setStrokeWidth(3);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setAntiAlias(true);
        
        circlePaint = new Paint();
        circlePaint.setColor(Color.parseColor("#8810B981"));
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(8, 0, 0, Color.BLACK);
    }
    
    public void setOnPointSelectedListener(OnPointSelectedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    touchX = event.getX();
                    touchY = event.getY();
                    hasTouched = true;
                    invalidate();
                    return true;
                    
                case MotionEvent.ACTION_UP:
                    if (listener != null && hasTouched) {
                        listener.onPointSelected(touchX, touchY);
                        Log.d(TAG, String.format("✅ Point selected: (%.0f, %.0f)", touchX, touchY));
                    }
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in touch event: " + e.getMessage());
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        try {
            if (!hasTouched) {
                // Center instructions
                float centerX = getWidth() / 2f;
                float centerY = getHeight() / 2f;
                
                canvas.drawText("DOKUN VE NOKTA EKLE", centerX, centerY, textPaint);
                canvas.drawText("↓", centerX, centerY + 80, textPaint);
                
            } else {
                // Draw crosshair
                
                // Vertical line
                canvas.drawLine(touchX, 0, touchX, getHeight(), crosshairPaint);
                
                // Horizontal line
                canvas.drawLine(0, touchY, getWidth(), touchY, crosshairPaint);
                
                // Center circle
                canvas.drawCircle(touchX, touchY, 50, circlePaint);
                canvas.drawCircle(touchX, touchY, 50, crosshairPaint);
                
                // Inner dot
                circlePaint.setColor(Color.parseColor("#DD10B981"));
                canvas.drawCircle(touchX, touchY, 20, circlePaint);
                circlePaint.setColor(Color.parseColor("#8810B981"));
                
                // Coordinates
                textPaint.setTextSize(32);
                String coords = String.format("(%.0f, %.0f)", touchX, touchY);
                canvas.drawText(coords, touchX, touchY - 80, textPaint);
                textPaint.setTextSize(48);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onDraw: " + e.getMessage());
        }
    }
}





























