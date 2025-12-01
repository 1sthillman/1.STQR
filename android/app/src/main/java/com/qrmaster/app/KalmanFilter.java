package com.qrmaster.app;

/**
 * 📊 KALMAN FİLTRESİ
 * 
 * Gürültülü sensör verilerini yumuşatır (smooth tracking)
 * - Process noise: Sistem gürültüsü
 * - Measurement noise: Ölçüm gürültüsü
 * 
 * Kullanım: Göz pozisyonu için sub-pixel hassasiyet
 */
public class KalmanFilter {
    
    // State
    private float x; // Tahmin edilen değer
    private float p; // Tahmin hatası kovaryansı
    
    // Noise parameters
    private final float q; // Process noise covariance
    private final float r; // Measurement noise covariance
    
    /**
     * @param processNoise Process gürültüsü (0.01 tipik)
     * @param measurementNoise Ölçüm gürültüsü (1.0 tipik)
     */
    public KalmanFilter(float processNoise, float measurementNoise) {
        this.q = processNoise;
        this.r = measurementNoise;
        this.x = 0.0f;
        this.p = 1.0f;
    }
    
    /**
     * Yeni ölçüm ile güncelle
     * 
     * @param measurement Yeni ölçüm değeri
     * @return Filtrelenmiş değer
     */
    public float filter(float measurement) {
        // Prediction
        float x_pred = x; // x = x (sabit hız modeli yok)
        float p_pred = p + q;
        
        // Update
        float k = p_pred / (p_pred + r); // Kalman gain
        x = x_pred + k * (measurement - x_pred);
        p = (1 - k) * p_pred;
        
        return x;
    }
    
    /**
     * Filtreyi sıfırla
     */
    public void reset() {
        x = 0.0f;
        p = 1.0f;
    }
    
    /**
     * Mevcut tahmin değeri
     */
    public float getEstimate() {
        return x;
    }
}





























