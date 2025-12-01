package com.qrmaster.app.keyboard;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 📷 IN-KEYBOARD GALLERY VIEW
 * 
 * Displays user's photos directly in keyboard for background selection
 * Modern, compact, 3-column grid layout
 */
public class InlineGalleryView extends LinearLayout {
    private Context context;
    private GalleryCallback callback;
    private GridLayout photoGrid;
    private List<String> photoPaths = new ArrayList<>();
    
    public interface GalleryCallback {
        void onPhotoSelected(String photoPath);
        void onClose();
    }
    
    public InlineGalleryView(Context context, GalleryCallback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            dp(320)  // Compact height
        ));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0A0A0A);
        setBackground(bg);
        
        init();
    }
    
    private void init() {
        // Header
        addView(createHeader());
        
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
        addView(scroll);
        
        // Load photos
        loadPhotos();
    }
    
    private LinearLayout createHeader() {
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
        TextView title = new TextView(context);
        title.setText("Galeriden Arka Plan Seç");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);
        
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
                    int limit = 50;  // Load max 50 photos
                    int count = 0;
                    
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    
                    while (cursor.moveToNext() && count < limit) {
                        String path = cursor.getString(dataColumn);
                        if (path != null) {
                            photoPaths.add(path);
                            count++;
                        }
                    }
                    cursor.close();
                    
                    // Update UI on main thread
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
                options.inSampleSize = 4;  // Scale down
                Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                
                if (bitmap != null) {
                    post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        imageView.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPhotoSelected(path);
            }
        });
        
        return imageView;
    }
    
    private void showError() {
        TextView error = new TextView(context);
        error.setText("📷 Fotoğraf bulunamadı\n\nLütfen galeri izni verin");
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
}

