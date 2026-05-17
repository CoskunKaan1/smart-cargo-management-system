package com.example.demo.model;

import java.time.LocalDateTime;

/**
 * Fatura bilgilerini tutan model sınıfı.
 * Veritabanındaki "fatura" tablosuyla eşleşir.
 */
public class Fatura {

    /**
     * Ödeme durumu enum'u
     */
    public enum OdemeDurumu {
        ODEMEDI,        // Henüz ödenmedi
        ODEME_BEKLIYOR, // Ödeme bekleniyor (fatura kesildi)
        ODEME_ALINDI    // Ödeme alındı
    }

    private int id;
    private String kargoId;
    private double tutar;
    private OdemeDurumu odemeDurumu;
    private LocalDateTime odemeTarihi;

    // Parametresiz constructor (ORM / reflection için)
    public Fatura() {
    }

    public Fatura(String kargoId, double tutar) {
        this.kargoId = kargoId;
        this.tutar = tutar;
        this.odemeDurumu = OdemeDurumu.ODEME_BEKLIYOR;
        this.odemeTarihi = null;
    }

    public Fatura(String kargoId, double tutar, OdemeDurumu odemeDurumu) {
        this.kargoId = kargoId;
        this.tutar = tutar;
        this.odemeDurumu = odemeDurumu;
        this.odemeTarihi = null;
    }

    public Fatura(int id, String kargoId, double tutar, OdemeDurumu odemeDurumu, LocalDateTime odemeTarihi) {
        this.id = id;
        this.kargoId = kargoId;
        this.tutar = tutar;
        this.odemeDurumu = odemeDurumu;
        this.odemeTarihi = odemeTarihi;
    }

    // --- Getter ve Setter ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKargoId() {
        return kargoId;
    }

    public void setKargoId(String kargoId) {
        this.kargoId = kargoId;
    }

    public double getTutar() {
        return tutar;
    }

    public void setTutar(double tutar) {
        this.tutar = tutar;
    }

    public OdemeDurumu getOdemeDurumu() {
        return odemeDurumu;
    }

    public void setOdemeDurumu(OdemeDurumu odemeDurumu) {
        this.odemeDurumu = odemeDurumu;
    }

    public LocalDateTime getOdemeTarihi() {
        return odemeTarihi;
    }

    public void setOdemeTarihi(LocalDateTime odemeTarihi) {
        this.odemeTarihi = odemeTarihi;
    }

    /**
     * Ödeme yapıldığında çağrılır, durumu günceller ve ödeme tarihini set eder.
     */
    public void odemeAl() {
        this.odemeDurumu = OdemeDurumu.ODEME_ALINDI;
        this.odemeTarihi = LocalDateTime.now();
    }

    /**
     * Ödeme durumunun Türkçe metin karşılığını döndürür.
     */
    public String getOdemeDurumuText() {
        switch (odemeDurumu) {
            case ODEMEDI:
                return "Ödenmedi";
            case ODEME_BEKLIYOR:
                return "Ödeme Bekleniyor";
            case ODEME_ALINDI:
                return "Ödeme Alındı";
            default:
                return "Bilinmiyor";
        }
    }

    @Override
    public String toString() {
        return String.format("Fatura #%d - Kargo: %s - %.2f TL - %s",
                id, kargoId, tutar, getOdemeDurumuText());
    }
}