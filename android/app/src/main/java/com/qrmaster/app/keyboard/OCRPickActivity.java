package com.qrmaster.app.keyboard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;

public class OCRPickActivity extends Activity {
    private static final String TAG = "OCRPickActivity";
    public static final String ACTION_OCR_PICK_RESULT = "com.qrmaster.app.OCR_PICK_RESULT";
    private static final int REQ_PICK_IMAGE = 1001;
    private static final int REQ_CAPTURE_IMAGE = 1002;

    private android.net.Uri cameraOutputUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            boolean useCamera = getIntent() != null && getIntent().getBooleanExtra("camera", false);
            if (useCamera) {
                java.io.File outFile = java.io.File.createTempFile("ocr_photo_", ".jpg", getCacheDir());
                cameraOutputUri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", outFile);
                Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cam.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraOutputUri);
                cam.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(cam, REQ_CAPTURE_IMAGE);
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQ_PICK_IMAGE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Picker start failed", e);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        // Persist permission for future access
                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception ignored) {}
                    // Broadcast result to IME
                    Intent br = new Intent(ACTION_OCR_PICK_RESULT);
                    br.setData(uri);
                    sendBroadcast(br);
                }
            }
            finish();
        } else if (requestCode == REQ_CAPTURE_IMAGE) {
            if (resultCode == RESULT_OK && cameraOutputUri != null) {
                try {
                    Intent br = new Intent(ACTION_OCR_PICK_RESULT);
                    br.setData(cameraOutputUri);
                    sendBroadcast(br);
                } catch (Exception e) {
                    Log.e(TAG, "Camera result broadcast failed", e);
                }
            }
            finish();
        }
    }
}

