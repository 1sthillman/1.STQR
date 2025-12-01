package com.qrmaster.app.keyboard.mouse;

import android.view.MotionEvent;
import android.util.Log;
import java.util.*;

/**
 * Windows 11 Precision Touchpad Gesture Algılama
 * 1-4 parmak hareketlerini algılar ve simüle eder
 */
public class GestureDetector {
    private static final String TAG = "GestureDetector";
    
    // Gesture tanıma parametreleri
    private static final float SWIPE_THRESHOLD = 100f;
    private static final float VELOCITY_THRESHOLD = 1000f;
    private static final int TAP_TIMEOUT = 200;
    
    private GestureCallback callback;
    private int pointerCount = 0;
    private float initialX, initialY;
    private float previousX, previousY;
    private long gestureStartTime;
    private float initialDistance;
    private boolean isScrolling = false;
    
    public interface GestureCallback {
        void onMouseMove(float deltaX, float deltaY);
        void onSingleTap();
        void onDoubleTap();
        void onTwoFingerTap(); // Sağ tık
        void onScroll(float deltaY);
        void onThreeFingerSwipeUp(); // Task View
        void onThreeFingerSwipeDown(); // Show Desktop
        void onThreeFingerSwipeLeft(); // Alt+Tab Previous
        void onThreeFingerSwipeRight(); // Alt+Tab Next
        void onThreeFingerTap(); // Search/Cortana
        void onFourFingerTap(); // Action Center
        void onFourFingerSwipeLeft(); // Virtual Desktop Left
        void onFourFingerSwipeRight(); // Virtual Desktop Right
        void onPinchZoom(float scale); // Zoom in/out
    }
    
    public GestureDetector(GestureCallback callback) {
        this.callback = callback;
    }
    
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handlePointerDown(event);
                break;
                
            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handlePointerUp(event);
                break;
                
            case MotionEvent.ACTION_CANCEL:
                reset();
                break;
        }
        
        return true;
    }
    
    private void handlePointerDown(MotionEvent event) {
        pointerCount = event.getPointerCount();
        gestureStartTime = System.currentTimeMillis();
        
        initialX = event.getX(0);
        initialY = event.getY(0);
        previousX = initialX;
        previousY = initialY;
        
        if (pointerCount == 2) {
            initialDistance = calculateDistance(event);
        }
        
        Log.d(TAG, "Pointer down: " + pointerCount + " parmak");
    }
    
    private void handleMove(MotionEvent event) {
        if (pointerCount == 0) return;
        
        float currentX = event.getX(0);
        float currentY = event.getY(0);
        
        float deltaX = currentX - previousX;
        float deltaY = currentY - previousY;
        
        previousX = currentX;
        previousY = currentY;
        
        switch (pointerCount) {
            case 1:
                // Tek parmak: Mouse hareketi
                if (Math.abs(deltaX) > 0.5f || Math.abs(deltaY) > 0.5f) {
                    callback.onMouseMove(deltaX, deltaY);
                }
                break;
                
            case 2:
                // İki parmak: Scroll veya Pinch Zoom
                if (event.getPointerCount() >= 2) {
                    float currentDistance = calculateDistance(event);
                    
                    if (Math.abs(currentDistance - initialDistance) > 50) {
                        // Pinch zoom
                        float scale = currentDistance / initialDistance;
                        callback.onPinchZoom(scale);
                    } else {
                        // Scroll
                        isScrolling = true;
                        callback.onScroll(-deltaY);
                    }
                }
                break;
                
            case 3:
            case 4:
                // 3-4 parmak hareketleri handlePointerUp'ta işlenecek
                break;
        }
    }
    
    private void handlePointerUp(MotionEvent event) {
        long duration = System.currentTimeMillis() - gestureStartTime;
        float totalDeltaX = event.getX(0) - initialX;
        float totalDeltaY = event.getY(0) - initialY;
        float totalDistance = (float) Math.sqrt(totalDeltaX * totalDeltaX + totalDeltaY * totalDeltaY);
        
        // Tap mı yoksa swipe mi?
        boolean isTap = duration < TAP_TIMEOUT && totalDistance < 50;
        boolean isSwipe = totalDistance > SWIPE_THRESHOLD;
        
        switch (pointerCount) {
            case 1:
                // Tek parmak tap: Sol tık
                if (isTap) {
                    callback.onSingleTap();
                }
                break;
                
            case 2:
                // İki parmak tap: Sağ tık
                if (isTap && !isScrolling) {
                    callback.onTwoFingerTap();
                }
                break;
                
            case 3:
                // Üç parmak hareketleri
                if (isTap) {
                    callback.onThreeFingerTap();
                } else if (isSwipe) {
                    detectThreeFingerSwipe(totalDeltaX, totalDeltaY);
                }
                break;
                
            case 4:
                // Dört parmak hareketleri
                if (isTap) {
                    callback.onFourFingerTap();
                } else if (isSwipe) {
                    detectFourFingerSwipe(totalDeltaX, totalDeltaY);
                }
                break;
        }
        
        if (event.getPointerCount() == 1) {
            reset();
        } else {
            pointerCount = event.getPointerCount() - 1;
        }
    }
    
    private void detectThreeFingerSwipe(float deltaX, float deltaY) {
        if (Math.abs(deltaY) > Math.abs(deltaX)) {
            // Dikey hareket
            if (deltaY < -SWIPE_THRESHOLD) {
                // Yukarı: Task View (Win + Tab)
                callback.onThreeFingerSwipeUp();
                Log.d(TAG, "✋ 3 parmak yukarı: Task View");
            } else if (deltaY > SWIPE_THRESHOLD) {
                // Aşağı: Show Desktop (Win + D)
                callback.onThreeFingerSwipeDown();
                Log.d(TAG, "✋ 3 parmak aşağı: Show Desktop");
            }
        } else {
            // Yatay hareket
            if (deltaX < -SWIPE_THRESHOLD) {
                // Sola: Alt+Tab Previous
                callback.onThreeFingerSwipeLeft();
                Log.d(TAG, "✋ 3 parmak sola: Alt+Tab Geri");
            } else if (deltaX > SWIPE_THRESHOLD) {
                // Sağa: Alt+Tab Next
                callback.onThreeFingerSwipeRight();
                Log.d(TAG, "✋ 3 parmak sağa: Alt+Tab İleri");
            }
        }
    }
    
    private void detectFourFingerSwipe(float deltaX, float deltaY) {
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Yatay hareket: Virtual Desktop geçişi
            if (deltaX < -SWIPE_THRESHOLD) {
                callback.onFourFingerSwipeLeft();
                Log.d(TAG, "✋ 4 parmak sola: Virtual Desktop Önceki");
            } else if (deltaX > SWIPE_THRESHOLD) {
                callback.onFourFingerSwipeRight();
                Log.d(TAG, "✋ 4 parmak sağa: Virtual Desktop Sonraki");
            }
        }
    }
    
    private float calculateDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0;
        
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    
    private void reset() {
        pointerCount = 0;
        isScrolling = false;
        initialDistance = 0;
    }
}







