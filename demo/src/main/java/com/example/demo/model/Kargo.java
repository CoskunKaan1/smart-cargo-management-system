package com.example.demo.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Kargo implements Comparable<Kargo> {

    public enum Tip {
        NORMAL, VIP, HIZLI, BOZULABILIR
    }

    public enum Durum {
        DEPODA, YOLDA, TESLIM_EDILDI
    }

    private final String id;
    private final String gonderen;
    private final String alici;
    private final String adres;
    private final Tip tip;
    private final double agirlik;
    private final double hacim;
    private Durum durum;
    private final LocalDateTime olusturmaTarihi;
    private int oncelik;
    private LocalDateTime teslimTarihi;      // 1. adım: teslim tarihi için eklendi

    public Kargo(String id, String gonderen, String alici, String adres,
                 Tip tip, double agirlik, double hacim) {
        this.id = id;
        this.gonderen = gonderen;
        this.alici = alici;
        this.adres = adres;
        this.tip = tip;
        this.agirlik = agirlik;
        this.hacim = hacim;
        this.durum = Durum.DEPODA;
        this.olusturmaTarihi = LocalDateTime.now();
        this.teslimTarihi = null;     // başlangıçta teslim edilmemiş
        this.oncelik = switch (tip) {
            case BOZULABILIR -> 1;
            case VIP         -> 2;
            case HIZLI       -> 3;
            case NORMAL      -> 4;
        };
    }

    @Override
    public int compareTo(Kargo other) {
        return Integer.compare(this.oncelik, other.oncelik);
    }

    // --- Getter'lar ---
    public String getId()                  { return id; }
    public String getGonderen()            { return gonderen; }
    public String getAlici()               { return alici; }
    public String getAdres()               { return adres; }
    public Tip getTip()                    { return tip; }
    public double getAgirlik()             { return agirlik; }
    public double getHacim()               { return hacim; }
    public Durum getDurum()                { return durum; }
    public int getOncelik()                { return oncelik; }
    public LocalDateTime getOlusturmaTarihi() { return olusturmaTarihi; }
    public LocalDateTime getTeslimTarihi() { return teslimTarihi; }

    // --- Setter'lar ---
    public void setDurum(Durum durum)      { this.durum = durum; }
    public void setTeslimTarihi(LocalDateTime teslimTarihi) { this.teslimTarihi = teslimTarihi; }

    // --- Yardımcı metotlar ---
    public String getTarihStr() {
        return olusturmaTarihi.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public String getTipRenk() {
        return switch (tip) {
            case VIP        -> "#f59e0b";
            case HIZLI      -> "#3b82f6";
            case BOZULABILIR-> "#ef4444";
            case NORMAL     -> "#6b7280";
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s → %s (%.1fkg, %s)", id, gonderen, alici, agirlik, tip);
    }
}