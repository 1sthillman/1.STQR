package com.qrmaster.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ AUTO CLICKER SERVICE - TAM ÖZELLİKLİ VERSİYON
 * 
 * ÖZELLİKLER:
 * - Floating kontrol paneli (sürüklenebilir)
 * - Çoklu tıklama noktaları (10'a kadar)
 * - Noktalar başlangıçta görünür
 * - Crosshair ile hassas nokta seçimi
 * - Swipe (kaydırma) desteği
 * - Scroll desteği
 * - Pinch-to-zoom desteği
 * - Panel küçültme/büyütme
 * - Ayarlanabilir hız ve tekrar sayısı
 * - Türkçe arayüz
 */
public class AutoClickerService extends Service {
    
    private static final String TAG = "AutoClickerService";
    private static final String CHANNEL_ID = "auto_clicker_channel";
    private static final int NOTIFICATION_ID = 5001;
    
    private WindowManager windowManager;
    private View floatingView;
    private View minimizedView;
    private View pointsOverlay;
    private View crosshairOverlay;
    
    private Handler handler;
    private Runnable clickRunnable;
    
    // Click points & gestures
    private List<ClickAction> actions = new ArrayList<>();
    private int currentActionIndex = 0;
    
    // Undo/Redo history
    private List<List<ClickAction>> history = new ArrayList<>();
    private int historyIndex = -1;
    private static final int MAX_HISTORY = 50;
    
    // Edit mode
    private boolean isEditMode = false;
    private View editOverlay = null;
    
    // Settings
    private boolean isRunning = false;
    private boolean pointsVisible = true; // ✅ Başlangıçta görünür
    private boolean isMinimized = false;
    private int clickInterval = 500;
    private int repeatCount = -1;
    private int currentRepeat = 0;
    
    // Power button listener
    private BroadcastReceiver screenReceiver;
    private long lastPowerPress = 0;
    private static final long DOUBLE_PRESS_INTERVAL = 500; // 500ms for double press
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀🚀🚀 AUTO CLICKER SERVICE CREATED 🚀🚀🚀");
        
        try {
            Log.d(TAG, "Step 1: Creating handler...");
            handler = new Handler(Looper.getMainLooper());
            
            Log.d(TAG, "Step 2: Getting window manager...");
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            if (windowManager == null) {
                Log.e(TAG, "❌ WindowManager is NULL!");
                return;
            }
            
            Log.d(TAG, "Step 3: Creating notification channel...");
            createNotificationChannel();
            
            Log.d(TAG, "Step 4: Starting foreground...");
            startForeground(NOTIFICATION_ID, createNotification("⚡ Otomatik Tıklayıcı Hazır"));
            
            Log.d(TAG, "Step 5: Creating floating control panel...");
            createFloatingControlPanel();
            
            Log.d(TAG, "Step 6: Creating points overlay...");
            createPointsOverlay();
            
            Log.d(TAG, "Step 7: Registering screen receiver...");
            registerScreenReceiver();
            
            Log.d(TAG, "✅✅✅ SERVICE FULLY INITIALIZED! ✅✅✅");
            
            // Show success toast
            handler.post(() -> {
                android.widget.Toast.makeText(this, 
                    "⚡ Auto Clicker Başlatıldı!", 
                    android.widget.Toast.LENGTH_LONG).show();
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌❌❌ CRITICAL ERROR in onCreate: " + e.getMessage());
            e.printStackTrace();
            
            // Show error toast
            if (handler != null) {
                handler.post(() -> {
                    android.widget.Toast.makeText(this, 
                        "❌ Hata: " + e.getMessage(), 
                        android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }
    }
    
    private void registerScreenReceiver() {
        try {
            screenReceiver = new BroadcastReceiver() {
                private int pressCount = 0;
                private long lastPress = 0;
                
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        long now = System.currentTimeMillis();
                        
                        // Check if within 800ms of last press
                           if (now - lastPress < 500) {
                               pressCount++;
                               
                               if (pressCount >= 2) {
                                   // DOUBLE PRESS DETECTED!
                                   Log.d(TAG, "🔴🔴 POWER BUTTON x2 DETECTED!");
                                   
                                   if (isRunning) {
                                       stopClicking();
                                       Log.d(TAG, "🔴 AUTO CLICKER STOPPED!");
                                       
                                       // Show toast
                                       handler.post(() -> {
                                           android.widget.Toast.makeText(AutoClickerService.this, 
                                               "⏸ DURDURULDU (Güç x2)", 
                                               android.widget.Toast.LENGTH_LONG).show();
                                       });
                                   }
                                   
                                   // Update notification
                                   try {
                                       NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                       if (nm != null) {
                                           Notification notification = createNotification("⏸ DURDURULDU (Güç x2)");
                                           nm.notify(NOTIFICATION_ID, notification);
                                       }
                                   } catch (Exception e) {
                                       Log.e(TAG, "Error updating notification: " + e.getMessage());
                                   }
                                   
                                   pressCount = 0;
                               }
                           } else {
                               pressCount = 1;
                           }

                           lastPress = now;
                           
                           Log.d(TAG, "🔌 POWER pressed - count: " + pressCount);
                    } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                        Log.d(TAG, "💡 Screen ON");
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(screenReceiver, filter);
            
            Log.d(TAG, "✅✅ Power button receiver ACTIVE - Press power button 2x to stop!");
            
        } catch (Exception e) {
            Log.e(TAG, "Error registering screen receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "✅ Auto Clicker Service STARTED");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 Auto Clicker Service DESTROYED");
        
        try {
            stopClicking();
            removeAllOverlays();
            
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // ══════════════════════════════════════════════════════════════
    // NOTIFICATION
    // ══════════════════════════════════════════════════════════════
    
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Otomatik Tıklayıcı",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Otomatik tıklayıcı servisi bildirimi");
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage());
        }
    }
    
    private Notification createNotification(String text) {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Otomatik Tıklayıcı")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pendingIntent)
                .build();
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage());
            return null;
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // FLOATING PANEL
    // ══════════════════════════════════════════════════════════════
    
    private void createFloatingControlPanel() {
        try {
            Log.d(TAG, "📱 Creating floating panel...");
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 50;
            params.y = 200;
            
            Log.d(TAG, "📱 Inflating SIMPLE layout...");
            floatingView = LayoutInflater.from(this).inflate(R.layout.auto_clicker_simple_panel, null);
            Log.d(TAG, "✅ Layout inflated!");
            
            if (windowManager != null && floatingView != null) {
                windowManager.addView(floatingView, params);
                setupFloatingUI(params);
                Log.d(TAG, "✅ Floating control panel created");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating floating panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupFloatingUI(WindowManager.LayoutParams params) {
        try {
            if (floatingView == null) return;
            
            // Drag handle
            View dragHandle = floatingView.findViewById(R.id.drag_handle);
            if (dragHandle != null) {
                dragHandle.setOnTouchListener(new View.OnTouchListener() {
                    private int initialX, initialY;
                    private float initialTouchX, initialTouchY;
                    
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    initialX = params.x;
                                    initialY = params.y;
                                    initialTouchX = event.getRawX();
                                    initialTouchY = event.getRawY();
                                    return true;
                                    
                                case MotionEvent.ACTION_MOVE:
                                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                    if (windowManager != null && floatingView != null) {
                                        windowManager.updateViewLayout(floatingView, params);
                                    }
                                    return true;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in drag: " + e.getMessage());
                        }
                        return false;
                    }
                });
            }
            
            // Start/Stop buttons
            Button btnStart = floatingView.findViewById(R.id.btn_start);
            if (btnStart != null) {
                btnStart.setOnClickListener(v -> startClicking());
            }
            
            Button btnStop = floatingView.findViewById(R.id.btn_stop);
            if (btnStop != null) {
                btnStop.setOnClickListener(v -> stopClicking());
            }
            
            // Add point with crosshair
            Button btnAddPoint = floatingView.findViewById(R.id.btn_add_point);
            if (btnAddPoint != null) {
                btnAddPoint.setOnClickListener(v -> showCrosshairForPoint());
            }
            
            // Edit points button
            Button btnEditPoints = floatingView.findViewById(R.id.btn_edit_points);
            if (btnEditPoints != null) {
                btnEditPoints.setOnClickListener(v -> toggleEditMode(btnEditPoints));
            }
            
            // Undo button
            Button btnUndo = floatingView.findViewById(R.id.btn_undo);
            if (btnUndo != null) {
                btnUndo.setOnClickListener(v -> undo());
            }
            
            // Redo button
            Button btnRedo = floatingView.findViewById(R.id.btn_redo);
            if (btnRedo != null) {
                btnRedo.setOnClickListener(v -> redo());
            }
            
            // Clear points
            Button btnClearPoints = floatingView.findViewById(R.id.btn_clear_points);
            if (btnClearPoints != null) {
                btnClearPoints.setOnClickListener(v -> clearActions());
            }
            
            // Toggle points visibility
            Button btnTogglePoints = floatingView.findViewById(R.id.btn_toggle_points);
            if (btnTogglePoints != null) {
                // Set initial text based on visibility
                btnTogglePoints.setText(pointsVisible ? "GİZLE" : "GÖSTER");
                btnTogglePoints.setOnClickListener(v -> togglePointsVisibility(btnTogglePoints));
            }
            
            // Add Swipe
            Button btnAddSwipe = floatingView.findViewById(R.id.btn_add_swipe);
            if (btnAddSwipe != null) {
                btnAddSwipe.setOnClickListener(v -> showCrosshairForSwipe());
            }
            
            // Add Double Tap
            Button btnAddDoubleTap = floatingView.findViewById(R.id.btn_add_double_tap);
            if (btnAddDoubleTap != null) {
                btnAddDoubleTap.setOnClickListener(v -> showCrosshairForDoubleTap());
            }
            
            // Add Long Press
            Button btnAddLongPress = floatingView.findViewById(R.id.btn_add_long_press);
            if (btnAddLongPress != null) {
                btnAddLongPress.setOnClickListener(v -> showCrosshairForLongPress());
            }
            
            // Add Pinch
            Button btnAddPinch = floatingView.findViewById(R.id.btn_add_pinch);
            if (btnAddPinch != null) {
                btnAddPinch.setOnClickListener(v -> showCrosshairForPinch());
            }
            
            // Interval seekbar
            SeekBar seekInterval = floatingView.findViewById(R.id.seekbar_interval);
            TextView txtInterval = floatingView.findViewById(R.id.tv_interval);
            if (seekInterval != null && txtInterval != null) {
                seekInterval.setProgress(clickInterval - 500);
                txtInterval.setText(clickInterval + "ms");
                
                seekInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        clickInterval = progress + 500; // 500ms to 10000ms
                        if (txtInterval != null) {
                            txtInterval.setText(clickInterval + "ms");
                        }
                    }
                    
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
            
            // Repeat seekbar
            SeekBar seekRepeat = floatingView.findViewById(R.id.seekbar_repeat);
            TextView txtRepeat = floatingView.findViewById(R.id.tv_repeat);
            if (seekRepeat != null && txtRepeat != null) {
                seekRepeat.setProgress(0);
                txtRepeat.setText("Sonsuz");
                
                seekRepeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        repeatCount = progress == 0 ? -1 : progress;
                        if (txtRepeat != null) {
                            txtRepeat.setText(progress == 0 ? "Sonsuz" : progress + " kez");
                        }
                    }
                    
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }
            
            // Emergency Stop
            Button btnEmergencyStop = floatingView.findViewById(R.id.btn_emergency_stop);
            if (btnEmergencyStop != null) {
                btnEmergencyStop.setOnClickListener(v -> {
                    if (isRunning) {
                        stopClicking();
                        Log.d(TAG, "🔴 EMERGENCY STOP pressed!");
                    }
                });
            }
            
            // Minimize
            Button btnMinimize = floatingView.findViewById(R.id.btn_minimize);
            if (btnMinimize != null) {
                btnMinimize.setOnClickListener(v -> minimizePanel());
            }
            
            updateUI();
            
            Log.d(TAG, "✅ Floating panel created and added to window!");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating floating panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // POINTS OVERLAY
    // ══════════════════════════════════════════════════════════════
    
    private void createPointsOverlay() {
        try {
            // Remove old overlay if exists
            if (pointsOverlay != null && windowManager != null) {
                try {
                    windowManager.removeView(pointsOverlay);
                } catch (Exception e) {
                    // Ignore
                }
                pointsOverlay = null;
            }
            
            // ✅ GÜVENLİ OVERLAY: Tamamen pasif, touch almaz, ASLA DONMAZ
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |  // ✅ ASLA TOUCH ALMAZ
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            
            // Pass actions list directly (reference)
            PointsOverlayView overlay = new PointsOverlayView(this, actions);
            
            pointsOverlay = overlay;
            
            if (windowManager != null && pointsOverlay != null) {
                windowManager.addView(pointsOverlay, params);
                // ✅ Başlangıçta görünür
                pointsOverlay.setVisibility(pointsVisible ? View.VISIBLE : View.GONE);
                Log.d(TAG, "✅ Points overlay created (VISIBLE by default) with " + actions.size() + " actions");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating points overlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveToHistory() {
        try {
            // Remove all history after current index
            while (historyIndex < history.size() - 1 && history.size() > 0) {
                history.remove(history.size() - 1);
            }
            
            // Deep copy current actions
            List<ClickAction> snapshot = new ArrayList<>();
            for (ClickAction action : actions) {
                snapshot.add(action.copy());
            }
            
            history.add(snapshot);
            historyIndex = history.size() - 1;
            
            // Limit history size
            while (history.size() > MAX_HISTORY) {
                history.remove(0);
                if (historyIndex > 0) {
                    historyIndex--;
                }
            }
            
            Log.d(TAG, "✅ History saved - Index: " + historyIndex + " / " + history.size() + " (actions: " + actions.size() + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving to history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void undo() {
        try {
            Log.d(TAG, "UNDO requested - historyIndex: " + historyIndex + ", history size: " + history.size());
            
            if (history.isEmpty()) {
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "Geçmiş yok", android.widget.Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            if (historyIndex > 0) {
                historyIndex--;
                
                // Restore from history
                List<ClickAction> snapshot = history.get(historyIndex);
                actions.clear();
                for (ClickAction action : snapshot) {
                    actions.add(action.copy());
                }
                
                refreshPointsOverlay();
                updateUI();
                
                Log.d(TAG, "✅ UNDO - Index: " + historyIndex + " (actions: " + actions.size() + ")");
                
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "⬅ GERİ (" + (historyIndex + 1) + "/" + history.size() + ")", android.widget.Toast.LENGTH_SHORT).show();
                });
            } else {
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "En başa geldiniz", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in undo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void redo() {
        try {
            Log.d(TAG, "REDO requested - historyIndex: " + historyIndex + ", history size: " + history.size());
            
            if (history.isEmpty()) {
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "Geçmiş yok", android.widget.Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            if (historyIndex < history.size() - 1) {
                historyIndex++;
                
                // Restore from history
                List<ClickAction> snapshot = history.get(historyIndex);
                actions.clear();
                for (ClickAction action : snapshot) {
                    actions.add(action.copy());
                }
                
                refreshPointsOverlay();
                updateUI();
                
                Log.d(TAG, "✅ REDO - Index: " + historyIndex + " (actions: " + actions.size() + ")");
                
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "➡ İLERİ (" + (historyIndex + 1) + "/" + history.size() + ")", android.widget.Toast.LENGTH_SHORT).show();
                });
            } else {
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "En sona geldiniz", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in redo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void toggleEditMode(Button btn) {
        try {
            isEditMode = !isEditMode;
            
            if (isEditMode) {
                // Create edit overlay - touchable and allows dragging
                createEditOverlay();
                if (btn != null) {
                    btn.setText("✓ BİTTİ");
                    btn.setBackgroundColor(0xDDEF4444);
                }
                
                handler.post(() -> {
                    android.widget.Toast.makeText(this, "✏ Noktaları sürükleyin", android.widget.Toast.LENGTH_LONG).show();
                });
                
            } else {
                // Remove edit overlay
                removeEditOverlay();
                if (btn != null) {
                    btn.setText("✏ DÜZENLE");
                    btn.setBackgroundColor(0xDD6366F1);
                }
                
                // Save to history after editing
                saveToHistory();
            }
            
            Log.d(TAG, "✅ Edit mode: " + isEditMode);
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling edit mode: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createEditOverlay() {
        try {
            if (editOverlay != null) {
                removeEditOverlay();
            }
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            
            EditOverlayView overlay = new EditOverlayView(this, actions);
            overlay.setOnPointMovedListener((index, x, y) -> {
                Log.d(TAG, "Point #" + index + " moved to: " + x + ", " + y);
                if (pointsOverlay != null) {
                    pointsOverlay.postInvalidate();
                }
            });
            
            overlay.setOnAutoCloseListener(() -> {
                Log.d(TAG, "🔴 AUTO CLOSE edit mode");
                handler.post(() -> {
                    Button btn = floatingView != null ? floatingView.findViewById(R.id.btn_edit_points) : null;
                    isEditMode = false;
                    removeEditOverlay();
                    if (btn != null) {
                        btn.setText("✏ DÜZENLE");
                        btn.setBackgroundColor(0xDD6366F1);
                    }
                    saveToHistory();
                    android.widget.Toast.makeText(this, "✓ Kaydedildi", android.widget.Toast.LENGTH_SHORT).show();
                });
            });
            
            editOverlay = overlay;
            
            if (windowManager != null) {
                windowManager.addView(editOverlay, params);
                Log.d(TAG, "✅ Edit overlay created");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating edit overlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void removeEditOverlay() {
        try {
            if (editOverlay != null && windowManager != null) {
                windowManager.removeView(editOverlay);
                editOverlay = null;
                Log.d(TAG, "✅ Edit overlay removed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing edit overlay: " + e.getMessage());
        }
    }
    
    private void togglePointsVisibility(Button btn) {
        try {
            pointsVisible = !pointsVisible;
            
            if (pointsOverlay != null) {
                pointsOverlay.setVisibility(pointsVisible ? View.VISIBLE : View.GONE);
                pointsOverlay.postInvalidate();
            }
            
            if (btn != null) {
                btn.setText(pointsVisible ? "GİZLE" : "GÖSTER");
            }
            
            Log.d(TAG, "Points visibility: " + pointsVisible + " (actions: " + actions.size() + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling points: " + e.getMessage());
        }
    }
    
    private void refreshPointsOverlay() {
        try {
            if (pointsOverlay != null) {
                pointsOverlay.postInvalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing points: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // CROSSHAIR OVERLAY
    // ══════════════════════════════════════════════════════════════
    
    private void showCrosshairForPoint() {
        showCrosshair("TAP");
    }
    
    private void showCrosshairForSwipe() {
        showCrosshair("SWIPE");
    }
    
    private void showCrosshairForDoubleTap() {
        showCrosshair("DOUBLE_TAP");
    }
    
    private void showCrosshairForLongPress() {
        showCrosshair("LONG_PRESS");
    }
    
    private void showCrosshairForPinch() {
        // Show zoom dialog first
        showZoomDialog();
    }
    
    private void showZoomDialog() {
        try {
            if (windowManager == null) return;
            
            // Create dialog view
            android.widget.LinearLayout dialogLayout = new android.widget.LinearLayout(this);
            dialogLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            dialogLayout.setPadding(40, 30, 40, 30);
            dialogLayout.setBackgroundResource(R.drawable.zoom_dialog_bg);
            
            // Title
            TextView title = new TextView(this);
            title.setText("ZOOM TİPİ SEÇ");
            title.setTextSize(20);
            title.setTextColor(Color.WHITE);
            title.setGravity(android.view.Gravity.CENTER);
            title.setPadding(0, 0, 0, 20);
            dialogLayout.addView(title);
            
            // Zoom In button
            Button btnZoomIn = new Button(this);
            btnZoomIn.setText("▲ YAKINLAŞTIR");
            btnZoomIn.setTextSize(16);
            btnZoomIn.setTextColor(Color.WHITE);
            btnZoomIn.setBackgroundColor(0xDD10B981);
            btnZoomIn.setPadding(20, 15, 20, 15);
            android.widget.LinearLayout.LayoutParams zoomInParams = 
                new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
            zoomInParams.bottomMargin = 10;
            btnZoomIn.setLayoutParams(zoomInParams);
            btnZoomIn.setOnClickListener(v -> {
                pendingZoomIn = true;
                removeZoomDialog();
                showCrosshair("PINCH");
            });
            dialogLayout.addView(btnZoomIn);
            
            // Zoom Out button
            Button btnZoomOut = new Button(this);
            btnZoomOut.setText("▼ UZAKLAŞTIR");
            btnZoomOut.setTextSize(16);
            btnZoomOut.setTextColor(Color.WHITE);
            btnZoomOut.setBackgroundColor(0xDDEF4444);
            btnZoomOut.setPadding(20, 15, 20, 15);
            android.widget.LinearLayout.LayoutParams zoomOutParams = 
                new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
            zoomOutParams.bottomMargin = 10;
            btnZoomOut.setLayoutParams(zoomOutParams);
            btnZoomOut.setOnClickListener(v -> {
                pendingZoomIn = false;
                removeZoomDialog();
                showCrosshair("PINCH");
            });
            dialogLayout.addView(btnZoomOut);
            
            // Cancel button
            Button btnCancel = new Button(this);
            btnCancel.setText("İPTAL");
            btnCancel.setTextSize(14);
            btnCancel.setTextColor(Color.parseColor("#94A3B8"));
            btnCancel.setBackgroundColor(0x55334155);
            btnCancel.setPadding(20, 15, 20, 15);
            btnCancel.setOnClickListener(v -> removeZoomDialog());
            dialogLayout.addView(btnCancel);
            
            // Add to window
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.CENTER;
            
            crosshairOverlay = dialogLayout; // Reuse crosshairOverlay for dialog
            windowManager.addView(crosshairOverlay, params);
            
            Log.d(TAG, "✅ Zoom dialog shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing zoom dialog: " + e.getMessage());
        }
    }
    
    private void removeZoomDialog() {
        try {
            if (crosshairOverlay != null && windowManager != null) {
                windowManager.removeView(crosshairOverlay);
                crosshairOverlay = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing zoom dialog: " + e.getMessage());
        }
    }
    
    private String currentCrosshairMode = "TAP";
    private float firstPointX = 0;
    private float firstPointY = 0;
    private boolean hasFirstPoint = false;
    private boolean pendingZoomIn = true;
    
    private void showCrosshair(String mode) {
        try {
            currentCrosshairMode = mode;
            hasFirstPoint = false;
            
            if (crosshairOverlay != null && windowManager != null) {
                windowManager.removeView(crosshairOverlay);
                crosshairOverlay = null;
            }
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            
            CrosshairOverlayView crosshair = new CrosshairOverlayView(this);
            crosshair.setOnPointSelectedListener((x, y) -> {
                handleCrosshairPoint(x, y);
            });
            
            crosshairOverlay = crosshair;
            
            if (windowManager != null) {
                windowManager.addView(crosshairOverlay, params);
                Log.d(TAG, "✅ Crosshair shown for mode: " + mode);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing crosshair: " + e.getMessage());
        }
    }
    
    private void handleCrosshairPoint(float x, float y) {
        try {
            switch (currentCrosshairMode) {
                case "TAP":
                    addTapAction(x, y);
                    hideCrosshair();
                    break;
                    
                case "DOUBLE_TAP":
                    addDoubleTapAction(x, y);
                    hideCrosshair();
                    break;
                    
                case "LONG_PRESS":
                    addLongPressAction(x, y);
                    hideCrosshair();
                    break;
                    
                case "SWIPE":
                    if (!hasFirstPoint) {
                        firstPointX = x;
                        firstPointY = y;
                        hasFirstPoint = true;
                        Log.d(TAG, "First point set for SWIPE");
                    } else {
                        addSwipeAction(firstPointX, firstPointY, x, y);
                        hideCrosshair();
                        hasFirstPoint = false;
                    }
                    break;
                    
                case "PINCH":
                    addPinchAction(x, y, pendingZoomIn);
                    hideCrosshair();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling crosshair point: " + e.getMessage());
        }
    }
    
    private void hideCrosshair() {
        try {
            if (crosshairOverlay != null && windowManager != null) {
                windowManager.removeView(crosshairOverlay);
                crosshairOverlay = null;
                Log.d(TAG, "✅ Crosshair hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding crosshair: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // MINIMIZE/MAXIMIZE
    // ══════════════════════════════════════════════════════════════
    
    private void minimizePanel() {
        try {
            if (floatingView == null || windowManager == null) return;
            
            isMinimized = !isMinimized;
            
            if (isMinimized) {
                floatingView.setVisibility(View.GONE);
                createMinimizedButton();
            } else {
                floatingView.setVisibility(View.VISIBLE);
                if (minimizedView != null) {
                    windowManager.removeView(minimizedView);
                    minimizedView = null;
                }
            }
            
            Log.d(TAG, "Panel minimized: " + isMinimized);
            
        } catch (Exception e) {
            Log.e(TAG, "Error minimizing panel: " + e.getMessage());
        }
    }
    
    private void createMinimizedButton() {
        try {
            if (minimizedView != null) return;
            
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.END;
            params.x = 20;
            params.y = 200;
            
            Button minBtn = new Button(this);
            minBtn.setText("OT");
            minBtn.setTextSize(14);
            minBtn.setTextColor(Color.WHITE);
            minBtn.setBackgroundColor(0xDD3B82F6);
            minBtn.setPadding(24, 16, 24, 16);
            
            // Make it draggable
            minBtn.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;
                private boolean isDragging = false;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialX = params.x;
                                initialY = params.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                isDragging = false;
                                return true;
                                
                            case MotionEvent.ACTION_MOVE:
                                int deltaX = (int) (initialTouchX - event.getRawX());
                                int deltaY = (int) (event.getRawY() - initialTouchY);
                                
                                // If moved more than 10px, it's a drag
                                if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                                    isDragging = true;
                                }
                                
                                if (isDragging) {
                                    params.x = initialX + deltaX;
                                    params.y = initialY + deltaY;
                                    if (windowManager != null && minimizedView != null) {
                                        windowManager.updateViewLayout(minimizedView, params);
                                    }
                                }
                                return true;
                                
                            case MotionEvent.ACTION_UP:
                                if (!isDragging) {
                                    // It's a click
                                    minimizePanel();
                                }
                                return true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in minimized button drag: " + e.getMessage());
                    }
                    return false;
                }
            });
            
            minimizedView = minBtn;
            
            if (windowManager != null) {
                windowManager.addView(minimizedView, params);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating minimized button: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // ACTION MANAGEMENT
    // ══════════════════════════════════════════════════════════════
    
    private void addTapAction(float x, float y) {
        try {
            ClickAction action = new ClickAction();
            action.type = ActionType.TAP;
            action.x = x;
            action.y = y;
            actions.add(action);
            
            Log.d(TAG, String.format("✅ TAP action added: (%.0f, %.0f)", x, y));
            updateUI();
            
            // Update overlay
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding tap action: " + e.getMessage());
        }
    }
    
    private void addSwipeAction(float startX, float startY, float endX, float endY) {
        try {
            ClickAction action = new ClickAction();
            action.type = ActionType.SWIPE;
            action.x = startX;
            action.y = startY;
            action.x2 = endX;
            action.y2 = endY;
            action.duration = 300;
            actions.add(action);
            
            saveToHistory();  // ✅ Save to history
            
            Log.d(TAG, String.format("✅ SWIPE action added: (%.0f,%.0f) → (%.0f,%.0f)", 
                startX, startY, endX, endY));
            updateUI();
            
            // Update overlay
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding swipe action: " + e.getMessage());
        }
    }
    
    private void addDoubleTapAction(float x, float y) {
        try {
            ClickAction action = new ClickAction();
            action.type = ActionType.DOUBLE_TAP;
            action.x = x;
            action.y = y;
            action.duration = 100;
            actions.add(action);
            
            saveToHistory();  // ✅ Save to history
            
            Log.d(TAG, String.format("✅ DOUBLE TAP action added: (%.0f, %.0f)", x, y));
            updateUI();
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding double tap action: " + e.getMessage());
        }
    }
    
    private void addLongPressAction(float x, float y) {
        try {
            ClickAction action = new ClickAction();
            action.type = ActionType.LONG_PRESS;
            action.x = x;
            action.y = y;
            action.duration = 800; // Long press duration
            actions.add(action);
            
            saveToHistory();  // ✅ Save to history
            
            Log.d(TAG, String.format("✅ LONG PRESS action added: (%.0f, %.0f)", x, y));
            updateUI();
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding long press action: " + e.getMessage());
        }
    }
    
    private void addPinchAction(float centerX, float centerY, boolean zoomIn) {
        try {
            ClickAction action = new ClickAction();
            action.type = ActionType.PINCH;
            action.x = centerX;
            action.y = centerY;
            action.duration = 400;
            action.zoomIn = zoomIn;
            actions.add(action);
            
            saveToHistory();  // ✅ Save to history
            
            Log.d(TAG, String.format("✅ PINCH action added at: (%.0f, %.0f) - %s", 
                centerX, centerY, zoomIn ? "ZOOM IN" : "ZOOM OUT"));
            updateUI();
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding pinch action: " + e.getMessage());
        }
    }
    
    private void clearActions() {
        try {
            actions.clear();
            currentActionIndex = 0;
            
            saveToHistory();  // ✅ Save to history
            
            Log.d(TAG, "✅ All actions cleared");
            updateUI();
            
            // Update overlay
            refreshPointsOverlay();
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing actions: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // CLICK LOGIC
    // ══════════════════════════════════════════════════════════════
    
    private void startClicking() {
        try {
            if (isRunning) return;
            if (actions.isEmpty()) {
                Log.w(TAG, "No actions added");
                return;
            }
            
            isRunning = true;
            currentRepeat = 0;
            currentActionIndex = 0;
            
            clickRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!isRunning || actions.isEmpty()) return;
                        
                        if (repeatCount > 0 && currentRepeat >= repeatCount) {
                            stopClicking();
                            return;
                        }
                        
                        ClickAction action = actions.get(currentActionIndex);
                        executeAction(action);
                        
                        currentActionIndex++;
                        if (currentActionIndex >= actions.size()) {
                            currentActionIndex = 0;
                            currentRepeat++;
                        }
                        
                        if (handler != null && isRunning) {
                            handler.postDelayed(this, clickInterval);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error in click runnable: " + e.getMessage());
                    }
                }
            };
            
            if (handler != null) {
                handler.post(clickRunnable);
            }
            
            Log.d(TAG, "✅ Auto clicking STARTED");
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting clicks: " + e.getMessage());
        }
    }
    
    private void stopClicking() {
        try {
            isRunning = false;
            
            if (handler != null && clickRunnable != null) {
                handler.removeCallbacks(clickRunnable);
            }
            
            Log.d(TAG, "🛑 Auto clicking STOPPED");
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping clicks: " + e.getMessage());
        }
    }
    
    private void executeAction(ClickAction action) {
        try {
            AutoClickerAccessibilityService service = AutoClickerAccessibilityService.getInstance();
            if (service == null) {
                Log.e(TAG, "Accessibility service not available");
                return;
            }
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.e(TAG, "Gestures require Android N or higher");
                return;
            }
            
            switch (action.type) {
                case TAP:
                    service.performTap(action.x, action.y, 100);
                    break;
                    
                case DOUBLE_TAP:
                    service.performDoubleTap(action.x, action.y, 100, 100);
                    break;
                    
                case LONG_PRESS:
                    service.performTap(action.x, action.y, action.duration);
                    break;
                    
                case SWIPE:
                    service.performSwipe(action.x, action.y, action.x2, action.y2, action.duration);
                    break;
                    
                case PINCH:
                    service.performPinch(action.x, action.y, 300, action.zoomIn, action.duration);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // UI UPDATE
    // ══════════════════════════════════════════════════════════════
    
    private void updateUI() {
        try {
            if (floatingView == null) return;
            
            // UI updated automatically through buttons
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // CLEANUP
    // ══════════════════════════════════════════════════════════════
    
    private void removeAllOverlays() {
        try {
            if (windowManager != null) {
                if (floatingView != null) {
                    windowManager.removeView(floatingView);
                    floatingView = null;
                }
                
                if (minimizedView != null) {
                    windowManager.removeView(minimizedView);
                    minimizedView = null;
                }
                
                if (pointsOverlay != null) {
                    windowManager.removeView(pointsOverlay);
                    pointsOverlay = null;
                }
                
                if (crosshairOverlay != null) {
                    windowManager.removeView(crosshairOverlay);
                    crosshairOverlay = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing overlays: " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ══════════════════════════════════════════════════════════════
    
    public enum ActionType {
        TAP,
        SWIPE,
        DOUBLE_TAP,
        LONG_PRESS,
        PINCH
    }
    
    public static class ClickAction {
        public ActionType type;
        public float x;
        public float y;
        public float x2;
        public float y2;
        public long duration = 100;
        public boolean zoomIn = true; // For PINCH - true = zoom in, false = zoom out
        
        public ClickAction() {}
        
        public ClickAction copy() {
            ClickAction copy = new ClickAction();
            copy.type = this.type;
            copy.x = this.x;
            copy.y = this.y;
            copy.x2 = this.x2;
            copy.y2 = this.y2;
            copy.duration = this.duration;
            copy.zoomIn = this.zoomIn;
            return copy;
        }
    }
    
    public static class ClickPoint {
        public float x;
        public float y;
        
        public ClickPoint() {}
        
        public ClickPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
