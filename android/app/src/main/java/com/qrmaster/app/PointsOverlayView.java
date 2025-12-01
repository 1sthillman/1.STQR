package com.qrmaster.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.View;

import java.util.List;

/**
 * ✅ 100% GÜVENLİ OVERLAY
 * - Sadece görüntü gösterir
 * - Touch almaz - ASLA DONMAZ
 * - Taşımak için: GERİ → Yeniden ekle
 */
public class PointsOverlayView extends View {
    
    private List<AutoClickerService.ClickAction> actions;
    private Paint pointPaint;
    private Paint outerPaint;
    private Paint textPaint;
    private Paint linePaint;
    
    public PointsOverlayView(Context context, List<AutoClickerService.ClickAction> actions) {
        super(context);
        this.actions = actions;
        
        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#DD10B981"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
        
        outerPaint = new Paint();
        outerPaint.setColor(Color.parseColor("#8810B981"));
        outerPaint.setStyle(Paint.Style.FILL);
        outerPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(8, 0, 0, Color.BLACK);
        textPaint.setFakeBoldText(true);
        
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#DD06B6D4"));
        linePaint.setStrokeWidth(8);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        try {
            if (actions == null || actions.isEmpty()) {
                return;
            }
            
            int tapIndex = 1;
            
            for (int i = 0; i < actions.size(); i++) {
                AutoClickerService.ClickAction action = actions.get(i);
                
                switch (action.type) {
                    case TAP:
                        outerPaint.setColor(Color.parseColor("#8810B981"));
                        canvas.drawCircle(action.x, action.y, 60, outerPaint);
                        
                        pointPaint.setColor(Color.parseColor("#DD10B981"));
                        canvas.drawCircle(action.x, action.y, 45, pointPaint);
                        
                        canvas.drawText(String.valueOf(tapIndex), action.x, action.y + 15, textPaint);
                        
                        tapIndex++;
                        break;
                        
                    case DOUBLE_TAP:
                        outerPaint.setColor(Color.parseColor("#8814B8A6"));
                        canvas.drawCircle(action.x, action.y, 60, outerPaint);
                        canvas.drawCircle(action.x + 15, action.y + 15, 50, outerPaint);
                        
                        pointPaint.setColor(Color.parseColor("#DD14B8A6"));
                        canvas.drawCircle(action.x, action.y, 40, pointPaint);
                        canvas.drawCircle(action.x + 15, action.y + 15, 35, pointPaint);
                        
                        textPaint.setTextSize(32);
                        canvas.drawText("x2", action.x + 8, action.y + 8, textPaint);
                        textPaint.setTextSize(40);
                        
                        tapIndex++;
                        break;
                        
                    case LONG_PRESS:
                        outerPaint.setColor(Color.parseColor("#888B5CF6"));
                        canvas.drawCircle(action.x, action.y, 70, outerPaint);
                        canvas.drawCircle(action.x, action.y, 55, outerPaint);
                        
                        pointPaint.setColor(Color.parseColor("#DD8B5CF6"));
                        canvas.drawCircle(action.x, action.y, 45, pointPaint);
                        
                        canvas.drawText(String.valueOf(tapIndex), action.x, action.y + 15, textPaint);
                        
                        tapIndex++;
                        break;
                        
                    case SWIPE:
                        linePaint.setColor(Color.parseColor("#DD06B6D4"));
                        linePaint.setStrokeWidth(8);
                        
                        canvas.drawLine(action.x, action.y, action.x2, action.y2, linePaint);
                        
                        outerPaint.setColor(Color.parseColor("#8806B6D4"));
                        canvas.drawCircle(action.x, action.y, 40, outerPaint);
                        pointPaint.setColor(Color.parseColor("#DD06B6D4"));
                        canvas.drawCircle(action.x, action.y, 30, pointPaint);
                        
                        canvas.drawCircle(action.x2, action.y2, 40, outerPaint);
                        canvas.drawCircle(action.x2, action.y2, 30, pointPaint);
                        
                        double angle = Math.atan2(action.y2 - action.y, action.x2 - action.x);
                        float arrowSize = 50;
                        float x1 = action.x2 - arrowSize * (float) Math.cos(angle - Math.PI / 6);
                        float y1 = action.y2 - arrowSize * (float) Math.sin(angle - Math.PI / 6);
                        float x2 = action.x2 - arrowSize * (float) Math.cos(angle + Math.PI / 6);
                        float y2 = action.y2 - arrowSize * (float) Math.sin(angle + Math.PI / 6);
                        
                        canvas.drawLine(action.x2, action.y2, x1, y1, linePaint);
                        canvas.drawLine(action.x2, action.y2, x2, y2, linePaint);
                        break;
                        
                    case PINCH:
                        outerPaint.setColor(Color.parseColor("#88F59E0B"));
                        canvas.drawCircle(action.x, action.y, 50, outerPaint);
                        pointPaint.setColor(Color.parseColor("#DDF59E0B"));
                        canvas.drawCircle(action.x, action.y, 35, pointPaint);
                        
                        canvas.drawCircle(action.x - 80, action.y, 35, outerPaint);
                        canvas.drawCircle(action.x - 80, action.y, 25, pointPaint);
                        
                        canvas.drawCircle(action.x + 80, action.y, 35, outerPaint);
                        canvas.drawCircle(action.x + 80, action.y, 25, pointPaint);
                        
                        linePaint.setColor(Color.parseColor("#DDF59E0B"));
                        linePaint.setStrokeWidth(4);
                        
                        if (action.zoomIn) {
                            canvas.drawLine(action.x - 80, action.y, action.x - 40, action.y, linePaint);
                            canvas.drawLine(action.x + 80, action.y, action.x + 40, action.y, linePaint);
                        } else {
                            canvas.drawLine(action.x - 40, action.y, action.x - 80, action.y, linePaint);
                            canvas.drawLine(action.x + 40, action.y, action.x + 80, action.y, linePaint);
                        }
                        
                        linePaint.setStrokeWidth(8);
                        break;
                }
            }
            
        } catch (Exception e) {
            android.util.Log.e("PointsOverlayView", "Error in onDraw: " + e.getMessage());
        }
    }
}
