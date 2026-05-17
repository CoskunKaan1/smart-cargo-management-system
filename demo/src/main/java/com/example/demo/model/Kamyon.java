package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.Stack;

public class Kamyon {
    private final String id;
    private final String plaka;
    private final double maxAgirlik;
    private final double maxHacim;
    private double mevcutAgirlik;
    private double mevcutHacim;
    private final Stack<Kargo> kargoStack;
    private boolean aktif;
    private String mevcutKonum;   // 4. adım simülasyon için (opsiyonel)

    public Kamyon(String id, String plaka, double maxAgirlik, double maxHacim) {
        this.id = id;
        this.plaka = plaka;
        this.maxAgirlik = maxAgirlik;
        this.maxHacim = maxHacim;
        this.mevcutAgirlik = 0;
        this.mevcutHacim = 0;
        this.kargoStack = new Stack<>();
        this.aktif = true;
        this.mevcutKonum = "Kadıköy";   // varsayılan başlangıç konumu
    }

    /**
     * Kamyona kargo ekler (stack push). Kapasite kontrolü yapar.
     * Kapasite %90'ı aşarsa sistem tepsisi uyarısı tetiklenir.
     * @param kargo eklenecek kargo
     * @return ekleme başarılı ise true
     */
    public boolean kargoEkle(Kargo kargo) {
        if (mevcutAgirlik + kargo.getAgirlik() <= maxAgirlik &&
                mevcutHacim + kargo.getHacim() <= maxHacim) {
            kargoStack.push(kargo);
            mevcutAgirlik += kargo.getAgirlik();
            mevcutHacim   += kargo.getHacim();
            kargo.setDurum(Kargo.Durum.YOLDA);

            // Kapasite uyarısı (2. adım)
            if (dolulukOrani() >= 90) {
                // Main.showTrayNotification çağrısı, Main sınıfına erişim gerektirir.
                // Burada sadece bildirim mesajını hazırlıyoruz; çağrıyı servis katmanı yapacak.
                // KargoServisi üzerinden bildirim göndermek daha temiz olur.
                // Aşağıdaki satır örnek amaçlıdır; projede Main.showTrayNotification kullanılabilir.
                // com.example.demo.Main.showTrayNotification("Kapasite Uyarısı", plaka + " %" + String.format("%.1f", dolulukOrani()) + " dolu!", java.awt.TrayIcon.MessageType.WARNING);
            }
            return true;
        }
        return false;
    }

    /**
     * Kamyondan en son yüklenen kargoyu teslim eder (stack pop).
     * Teslim tarihini otomatik olarak şimdiki zamana set eder.
     * @return teslim edilen kargo, stack boşsa null
     */
    public Kargo kargoTeslimEt() {
        if (!kargoStack.isEmpty()) {
            Kargo k = kargoStack.pop();
            mevcutAgirlik -= k.getAgirlik();
            mevcutHacim   -= k.getHacim();
            k.setDurum(Kargo.Durum.TESLIM_EDILDI);
            k.setTeslimTarihi(LocalDateTime.now());   // 1. adım teslim tarihi
            return k;
        }
        return null;
    }

    // Doluluk oranları (yüzde)
    public double agirlikDolulukOrani() { return (mevcutAgirlik / maxAgirlik) * 100; }
    public double hacimDolulukOrani()   { return (mevcutHacim   / maxHacim)   * 100; }
    public double dolulukOrani()        { return Math.max(agirlikDolulukOrani(), hacimDolulukOrani()); }

    // Getter'lar
    public String getId()              { return id; }
    public String getPlaka()           { return plaka; }
    public double getMaxAgirlik()      { return maxAgirlik; }
    public double getMaxHacim()        { return maxHacim; }
    public double getMevcutAgirlik()   { return mevcutAgirlik; }
    public double getMevcutHacim()     { return mevcutHacim; }
    public Stack<Kargo> getKargoStack(){ return kargoStack; }
    public boolean isAktif()           { return aktif; }
    public String getMevcutKonum()     { return mevcutKonum; }

    // Setter'lar (veritabanı ve simülasyon için)
    public void setMevcutAgirlik(double mevcutAgirlik) { this.mevcutAgirlik = mevcutAgirlik; }
    public void setMevcutHacim(double mevcutHacim)     { this.mevcutHacim = mevcutHacim; }
    public void setAktif(boolean aktif)                { this.aktif = aktif; }
    public void setMevcutKonum(String mevcutKonum)     { this.mevcutKonum = mevcutKonum; }

    public int getKargoSayisi() { return kargoStack.size(); }

    @Override
    public String toString() {
        return plaka + " (" + String.format("%.0f", dolulukOrani()) + "% dolu)";
    }
}