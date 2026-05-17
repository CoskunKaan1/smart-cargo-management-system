package com.example.demo.model;

import java.time.LocalDate;

/**
 * Kamyon Bakım Kayıtlarını tutan model sınıfı.
 * Veritabanındaki "kamyon_bakim" tablosuyla eşleşir.
 */
public class KamyonBakim {
    private int id;
    private String kamyonId;
    private LocalDate bakimTarihi;
    private int kilometre;
    private String arizaAciklamasi;
    private double ucret;

    // Parametresiz constructor (ORM / reflection için)
    public KamyonBakim() {
    }

    public KamyonBakim(String kamyonId, LocalDate bakimTarihi, int kilometre, String arizaAciklamasi, double ucret) {
        this.kamyonId = kamyonId;
        this.bakimTarihi = bakimTarihi;
        this.kilometre = kilometre;
        this.arizaAciklamasi = arizaAciklamasi;
        this.ucret = ucret;
    }

    public KamyonBakim(int id, String kamyonId, LocalDate bakimTarihi, int kilometre, String arizaAciklamasi, double ucret) {
        this.id = id;
        this.kamyonId = kamyonId;
        this.bakimTarihi = bakimTarihi;
        this.kilometre = kilometre;
        this.arizaAciklamasi = arizaAciklamasi;
        this.ucret = ucret;
    }

    // --- Getter ve Setter ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKamyonId() {
        return kamyonId;
    }

    public void setKamyonId(String kamyonId) {
        this.kamyonId = kamyonId;
    }

    public LocalDate getBakimTarihi() {
        return bakimTarihi;
    }

    public void setBakimTarihi(LocalDate bakimTarihi) {
        this.bakimTarihi = bakimTarihi;
    }

    public int getKilometre() {
        return kilometre;
    }

    public void setKilometre(int kilometre) {
        this.kilometre = kilometre;
    }

    public String getArizaAciklamasi() {
        return arizaAciklamasi;
    }

    public void setArizaAciklamasi(String arizaAciklamasi) {
        this.arizaAciklamasi = arizaAciklamasi;
    }

    public double getUcret() {
        return ucret;
    }

    public void setUcret(double ucret) {
        this.ucret = ucret;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%.2f TL)", kamyonId, bakimTarihi, arizaAciklamasi, ucret);
    }
}