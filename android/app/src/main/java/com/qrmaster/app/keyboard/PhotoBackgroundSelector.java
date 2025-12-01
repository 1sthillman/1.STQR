package com.qrmaster.app.keyboard;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 📷 PHOTO BACKGROUND SELECTOR WITH PREVIEW
 * 
 * 1. Grid view: Select photo from gallery
 * 2. Preview view: Adjust visible area (pan/zoom)
 * 3. Apply: Crop and set as keyboard background
 */
public class PhotoBackgroundSelector extends LinearLayout {
    private Context context;
    private PhotoCallback callback;
    private GridLayout photoGrid;
    private List<String> photoPaths = new ArrayList<>();
    
    // Preview mode
    private LinearLayout gridView;
    private LinearLayout previewView;
    private PhotoPreviewView photoPreview;
    private String selectedPhotoPath;
    
    public interface PhotoCallback {
        void onPhotoApplied(String croppedPhotoPath);
        void onClose();
    }
    
    public PhotoBackgroundSelector(Context context, PhotoCallback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(380)  // Taller for preview
        ));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0A0A0A);
        setBackground(bg);
        
        init();
    }
    
    private void init() {
        // Grid view (photo selection)
        gridView = createGridView();
        addView(gridView);
        
        // Preview view (crop/adjust) - initially hidden
        previewView = createPreviewView();
        previewView.setVisibility(GONE);
        addView(previewView);
        
        // Load photos
        loadPhotos();
    }
    
    private LinearLayout createGridView() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        layout.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ));
        
        // Header
        layout.addView(createHeader("Galeriden Fotoğraf Seç", false));
        
        // Photo grid (scrollable)
        ScrollView scroll = new ScrollView(context);
        LayoutParams scrollParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        scroll.setLayoutParams(scrollParams);
        
        photoGrid = new GridLayout(context);
        photoGrid.setColumnCount(3);  // 3 columns
        photoGrid.setPadding(dp(8), dp(8), dp(8), dp(8));
        
        scroll.addView(photoGrid);
        layout.addView(scroll);
        
        return layout;
    }
    
    private LinearLayout createPreviewView() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        layout.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ));
        
        // Header
        layout.addView(createHeader("Arka Planı Ayarla", true));
        
        // Instructions
        TextView instructions = new TextView(context);
        instructions.setText("📌 Fotoğrafı sürükle/zoom yap • Klavye alanını ayarla");
        instructions.setTextColor(0xFF666666);
        instructions.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(dp(16), dp(8), dp(16), dp(8));
        layout.addView(instructions);
        
        // Photo preview with crop overlay
        photoPreview = new PhotoPreviewView(context);
        LayoutParams previewParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        layout.addView(photoPreview, previewParams);
        
        // Action buttons
        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(HORIZONTAL);
        buttons.setPadding(dp(16), dp(12), dp(16), dp(12));
        buttons.setGravity(Gravity.CENTER);
        
        // Back button
        Button backBtn = createActionButton("Geri", 0xFF666666);
        backBtn.setOnClickListener(v -> showGrid());
        LayoutParams backParams = new LayoutParams(0, dp(48), 1f);
        backParams.setMargins(0, 0, dp(8), 0);
        buttons.addView(backBtn, backParams);
        
        // Apply button
        Button applyBtn = createActionButton("Uygula", 0xFF0A84FF);
        applyBtn.setOnClickListener(v -> applyPhoto());
        LayoutParams applyParams = new LayoutParams(0, dp(48), 2f);
        buttons.addView(applyBtn, applyParams);
        
        layout.addView(buttons);
        
        return layout;
    }
    
    private LinearLayout createHeader(String title, boolean showBack) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(0xFF1A1A1A);
        header.setBackground(headerBg);
        
        LayoutParams headerParams = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        header.setLayoutParams(headerParams);
        
        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(titleView, titleParams);
        
        // Close button
        ImageButton closeBtn = new ImageButton(context);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setCornerRadius(dp(8));
        closeBg.setColor(0xFF2A2A2A);
        closeBtn.setBackground(closeBg);
        closeBtn.setImageResource(com.qrmaster.app.R.drawable.ic_close);
        closeBtn.setColorFilter(0xFFFFFFFF);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        LayoutParams closeParams = new LayoutParams(dp(40), dp(40));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            if (callback != null) {
                callback.onClose();
            }
        });
        header.addView(closeBtn);
        
        return header;
    }
    
    private Button createActionButton(String text, int color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setAllCaps(false);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(color);
        button.setBackground(bg);
        
        return button;
    }
    
    private void loadPhotos() {
        new Thread(() -> {
            try {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
                };
                
                Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                );
                
                if (cursor != null) {
                    photoPaths.clear();
                    int limit = 50;
                    int count = 0;
                    
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    
                    while (cursor.moveToNext() && count < limit) {
                        String path = cursor.getString(dataColumn);
                        if (path != null && new File(path).exists()) {
                            photoPaths.add(path);
                            count++;
                        }
                    }
                    cursor.close();
                    
                    post(() -> displayPhotos());
                }
            } catch (Exception e) {
                e.printStackTrace();
                post(() -> showError());
            }
        }).start();
    }
    
    private void displayPhotos() {
        photoGrid.removeAllViews();
        
        if (photoPaths.isEmpty()) {
            showError();
            return;
        }
        
        for (String path : photoPaths) {
            photoGrid.addView(createPhotoCard(path));
        }
    }
    
    private View createPhotoCard(String path) {
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(8));
        cardBg.setColor(0xFF2A2A2A);
        imageView.setBackground(cardBg);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(110);
        params.height = dp(110);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        imageView.setLayoutParams(params);
        
        // Load thumbnail async
        new Thread(() -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                
                if (bitmap != null) {
                    post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        imageView.setOnClickListener(v -> showPreview(path));
        
        return imageView;
    }
    
    private void showPreview(String photoPath) {
        selectedPhotoPath = photoPath;
        photoPreview.setPhoto(photoPath);
        
        gridView.setVisibility(GONE);
        previewView.setVisibility(VISIBLE);
    }
    
    private void showGrid() {
        gridView.setVisibility(VISIBLE);
        previewView.setVisibility(GONE);
    }
    
    private void applyPhoto() {
        if (selectedPhotoPath == null) return;
        
        try {
            // Get cropped bitmap
            Bitmap croppedBitmap = photoPreview.getCroppedBitmap();
            if (croppedBitmap == null) return;
            
            // Save to cache
            File cacheDir = context.getCacheDir();
            File outputFile = new File(cacheDir, "keyboard_bg_" + System.currentTimeMillis() + ".jpg");
            
            FileOutputStream fos = new FileOutputStream(outputFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            if (callback != null) {
                callback.onPhotoApplied(outputFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showError() {
        TextView error = new TextView(context);
        error.setText("📷 Fotoğraf bulunamadı\n\nGaleri iznini kontrol edin");
        error.setTextColor(0xFF666666);
        error.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        error.setGravity(Gravity.CENTER);
        error.setPadding(dp(32), dp(32), dp(32), dp(32));
        
        photoGrid.removeAllViews();
        photoGrid.addView(error);
    }
    
    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    /**
     * Photo preview with pan/zoom support
     */
    private class PhotoPreviewView extends View {
        private Bitmap photoBitmap;
        private Matrix matrix = new Matrix();
        private Matrix savedMatrix = new Matrix();
        
        private PointF startPoint = new PointF();
        private PointF midPoint = new PointF();
        private float oldDist = 1f;
        
        private static final int MODE_NONE = 0;
        private static final int MODE_DRAG = 1;
        private static final int MODE_ZOOM = 2;
        private int mode = MODE_NONE;
        
        private Paint photoPaint = new Paint();
        private Paint overlayPaint = new Paint();
        
        public PhotoPreviewView(Context context) {
            super(context);
            
            photoPaint.setAntiAlias(true);
            photoPaint.setFilterBitmap(true);
            
            overlayPaint.setColor(0x88000000);
            overlayPaint.setStyle(Paint.Style.FILL);
        }
        
        public void setPhoto(String path) {
            new Thread(() -> {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;  // Half size for performance
                    photoBitmap = BitmapFactory.decodeFile(path, options);
                    
                    if (photoBitmap != null) {
                        post(() -> {
                            centerPhoto();
                            invalidate();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        private void centerPhoto() {
            if (photoBitmap == null) return;
            
            matrix.reset();
            
            float viewWidth = getWidth();
            float viewHeight = getHeight();
            float bitmapWidth = photoBitmap.getWidth();
            float bitmapHeight = photoBitmap.getHeight();
            
            // Scale to fit width
            float scale = viewWidth / bitmapWidth;
            float scaledHeight = bitmapHeight * scale;
            
            // Center vertically
            float dy = (viewHeight - scaledHeight) / 2f;
            
            matrix.postScale(scale, scale);
            matrix.postTranslate(0, dy);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (photoBitmap != null) {
                canvas.drawBitmap(photoBitmap, matrix, photoPaint);
                
                // Draw overlay (darker area outside keyboard region)
                int viewHeight = getHeight();
                int keyboardHeight = dp(240);  // Approx keyboard height
                int overlayTop = viewHeight - keyboardHeight;
                
                // Top overlay
                canvas.drawRect(0, 0, getWidth(), overlayTop, overlayPaint);
                
                // Draw keyboard region border
                Paint borderPaint = new Paint();
                borderPaint.setColor(0xFF0A84FF);
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(dp(2));
                canvas.drawRect(0, overlayTop, getWidth(), viewHeight, borderPaint);
                
                // Label
                Paint textPaint = new Paint();
                textPaint.setColor(0xFFFFFFFF);
                textPaint.setTextSize(dp(12));
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Bu alan klavyede görünecek", getWidth() / 2f, overlayTop + dp(20), textPaint);
            }
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (photoBitmap == null) return false;
            
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set(event.getX(), event.getY());
                    mode = MODE_DRAG;
                    break;
                    
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(midPoint, event);
                        mode = MODE_ZOOM;
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = MODE_NONE;
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    if (mode == MODE_DRAG) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - startPoint.x;
                        float dy = event.getY() - startPoint.y;
                        matrix.postTranslate(dx, dy);
                    } else if (mode == MODE_ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        }
                    }
                    break;
            }
            
            invalidate();
            return true;
        }
        
        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }
        
        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }
        
        public Bitmap getCroppedBitmap() {
            if (photoBitmap == null) return null;
            
            try {
                // Create bitmap of visible area (keyboard region)
                int viewHeight = getHeight();
                int keyboardHeight = dp(240);
                int overlayTop = viewHeight - keyboardHeight;
                
                Bitmap output = Bitmap.createBitmap(getWidth(), keyboardHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(output);
                
                // Draw photo with current matrix, shifted up
                Matrix cropMatrix = new Matrix(matrix);
                cropMatrix.postTranslate(0, -overlayTop);
                canvas.drawBitmap(photoBitmap, cropMatrix, photoPaint);
                
                return output;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

