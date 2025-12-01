package com.qrmaster.app.keyboard.miniapps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.qrmaster.app.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.DecimalFormat;

/**
 * Mini Harita Modülü - OpenStreetMap
 */
public class MiniMapView extends LinearLayout implements LocationListener {
    private static final String TAG = "MiniMapView";
    private static final double DEFAULT_LAT = 41.0082; // İstanbul
    private static final double DEFAULT_LON = 28.9784;
    
    private MapView mapView;
    private TextView coordsText;
    private Marker selectedMarker;
    private LocationManager locationManager;
    private GeoPoint currentLocation = new GeoPoint(DEFAULT_LAT, DEFAULT_LON);

    public interface MapCallback {
        void onLocationSelected(String locationUrl);
        void onClose();
    }

    private final MapCallback callback;

    public MiniMapView(Context context, MapCallback callback) {
        super(context);
        this.callback = callback;
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xFF1C1C1E);
        setPadding(dp(12), dp(12), dp(12), dp(12));

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        
        TextView title = new TextView(context);
        title.setText("🗺️ Harita");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageButton closeBtn = new ImageButton(context);
        closeBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_button_bg));
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        closeBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> { if (callback != null) callback.onClose(); });
        header.addView(closeBtn);
        addView(header);

        // Map
        try {
            mapView = new MapView(context);
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(currentLocation);
            
            // Do not steal focus from IME window
            mapView.setFocusable(false);
            mapView.setFocusableInTouchMode(false);
            mapView.setClickable(true);
            
            LayoutParams mapParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
            mapParams.bottomMargin = dp(12);
            android.graphics.drawable.GradientDrawable mapBg = new android.graphics.drawable.GradientDrawable();
            mapBg.setCornerRadius(dp(12));
            mapBg.setColor(0xFF2C2C2E);
            mapView.setBackground(mapBg);
            mapView.setClipToOutline(true);
            addView(mapView, mapParams);

            // Marker on tap
            selectedMarker = new Marker(mapView);
            selectedMarker.setIcon(context.getResources().getDrawable(R.drawable.ic_camera, null));
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(selectedMarker);

            mapView.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels(
                        (int) event.getX(), (int) event.getY()
                    );
                    selectedMarker.setPosition(geoPoint);
                    currentLocation = geoPoint;
                    updateCoords();
                    mapView.invalidate();
                }
                // Do not request focus to prevent IME hide
                return false;
            });

        } catch (Exception e) {
            Log.e(TAG, "Map initialization failed", e);
            TextView errorText = new TextView(context);
            errorText.setText("❌ Harita yüklenemedi\n(İnternet bağlantısı gerekli)");
            errorText.setTextColor(0xFFFF3B30);
            errorText.setGravity(Gravity.CENTER);
            errorText.setTextSize(14);
            addView(errorText, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        }

        // Coords display
        coordsText = new TextView(context);
        coordsText.setTextColor(0xFF8E8E93);
        coordsText.setTextSize(12);
        coordsText.setGravity(Gravity.CENTER);
        coordsText.setPadding(dp(8), dp(8), dp(8), dp(8));
        coordsText.setBackgroundColor(0xFF2C2C2E);
        LayoutParams coordsParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        coordsParams.bottomMargin = dp(12);
        android.graphics.drawable.GradientDrawable coordsBg = new android.graphics.drawable.GradientDrawable();
        coordsBg.setCornerRadius(dp(8));
        coordsBg.setColor(0xFF2C2C2E);
        coordsText.setBackground(coordsBg);
        addView(coordsText, coordsParams);
        updateCoords();

        // Buttons
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(HORIZONTAL);
        btnRow.setWeightSum(2f);
        
        Button myLocationBtn = createButton(context, "📍 Konumum", 0xFF007AFF);
        myLocationBtn.setOnClickListener(v -> goToMyLocation());
        LinearLayout.LayoutParams btn1Params = new LinearLayout.LayoutParams(0, dp(50), 1f);
        btn1Params.rightMargin = dp(6);
        btnRow.addView(myLocationBtn, btn1Params);

        Button shareBtn = createButton(context, "✓ Paylaş", 0xFF34C759);
        shareBtn.setOnClickListener(v -> shareLocation());
        LinearLayout.LayoutParams btn2Params = new LinearLayout.LayoutParams(0, dp(50), 1f);
        btn2Params.leftMargin = dp(6);
        btnRow.addView(shareBtn, btn2Params);
        
        addView(btnRow);

        // Try to get location
        goToMyLocation();
    }

    private Button createButton(Context context, String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(14);
        btn.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(color);
        btn.setBackground(bg);
        return btn;
    }

    private void goToMyLocation() {
        try {
            locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return;

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Konum izni gerekli", Toast.LENGTH_SHORT).show();
                return;
            }

            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnown != null) {
                currentLocation = new GeoPoint(lastKnown.getLatitude(), lastKnown.getLongitude());
                if (mapView != null) {
                    mapView.getController().animateTo(currentLocation);
                    selectedMarker.setPosition(currentLocation);
                    mapView.invalidate();
                }
                updateCoords();
            } else {
                Toast.makeText(getContext(), "Konum alınamadı", Toast.LENGTH_SHORT).show();
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, this);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.e(TAG, "Location error", e);
        }
    }

    private void shareLocation() {
        if (callback == null) return;
        double lat = currentLocation.getLatitude();
        double lon = currentLocation.getLongitude();
        String url = "https://maps.google.com/?q=" + lat + "," + lon;
        callback.onLocationSelected(url);
        callback.onClose();
    }

    private void updateCoords() {
        DecimalFormat df = new DecimalFormat("#.####");
        String coords = "📍 " + df.format(currentLocation.getLatitude()) + ", " + 
                        df.format(currentLocation.getLongitude());
        coordsText.setText(coords);
    }

    private interface AddressCallback {
        void onAddress(String text);
    }

    private void resolveAddressAsync(double lat, double lon, AddressCallback cb) {
        new Thread(() -> {
            String result = null;
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(getContext(), java.util.Locale.getDefault());
                java.util.List<android.location.Address> list = geocoder.getFromLocation(lat, lon, 1);
                if (list != null && !list.isEmpty()) {
                    android.location.Address a = list.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (a.getSubLocality() != null) sb.append(a.getSubLocality()).append(' ');
                    if (a.getThoroughfare() != null) sb.append(a.getThoroughfare()).append(' ');
                    if (a.getSubThoroughfare() != null) sb.append("No ").append(a.getSubThoroughfare()).append(' ');
                    if (a.getSubAdminArea() != null) sb.append(a.getSubAdminArea()).append(' ');
                    if (a.getAdminArea() != null) sb.append(a.getAdminArea());
                    result = sb.toString().trim();
                    if (result.isEmpty() && a.getAddressLine(0) != null) {
                        result = a.getAddressLine(0);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Reverse geocode failed", e);
            }
            if (result == null || result.isEmpty()) {
                DecimalFormat df = new DecimalFormat("#.######");
                result = df.format(lat) + ", " + df.format(lon);
            }
            final String text = result;
            post(() -> cb.onAddress(text));
        }).start();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (mapView != null) {
            mapView.getController().animateTo(currentLocation);
            selectedMarker.setPosition(currentLocation);
            mapView.invalidate();
        }
        updateCoords();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public void release() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove location updates", e);
            }
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    public void onResume() {
        if (mapView != null) mapView.onResume();
    }

    public void onPause() {
        if (mapView != null) mapView.onPause();
    }
}

