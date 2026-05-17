package com.example.demo.model;

import java.time.LocalDateTime;

/**
 * Müşteri (Gönderen / Alıcı) bilgilerini tutan model sınıfı.
 * Veritabanındaki "musteri" tablosuyla eşleşir.
 */
public class Musteri {
    private int id;
    private String adSoyad;
    private String telefon;
    private String email;
    private String adres;
    private LocalDateTime kayitTarihi;

    // Parametresiz constructor (ORM / reflection için)
    public Musteri() {
    }

    public Musteri(String adSoyad, String telefon, String email, String adres) {
        this.adSoyad = adSoyad;
        this.telefon = telefon;
        this.email = email;
        this.adres = adres;
        this.kayitTarihi = LocalDateTime.now();
    }

    public Musteri(int id, String adSoyad, String telefon, String email, String adres, LocalDateTime kayitTarihi) {
        this.id = id;
        this.adSoyad = adSoyad;
        this.telefon = telefon;
        this.email = email;
        this.adres = adres;
        this.kayitTarihi = kayitTarihi;
    }

    // --- Getter ve Setter ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAdSoyad() {
        return adSoyad;
    }

    public void setAdSoyad(String adSoyad) {
        this.adSoyad = adSoyad;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", adSoyad, telefon);
    }
}