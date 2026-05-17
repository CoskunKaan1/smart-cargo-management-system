package com.example.demo.service;

import com.example.demo.model.Kamyon;
import com.example.demo.model.Kargo;
import com.example.demo.ds.RotaGraf;
import com.example.demo.Main;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.awt.TrayIcon;
import java.util.*;

public class SimulasyonServisi {

    private final KargoServisi servis;
    private final RotaGraf rotaGraf;
    private final Map<Kamyon, List<String>> kamyonRotalari;   // her kamyon için gidilecek sıralı durak listesi
    private final Map<Kamyon, Integer> kamyonDurakIndex;      // o an hangi durakta olduğu (index)
    private final Map<Kamyon, Double> kamyonYakıtTuketimi;    // toplam yakıt (litre)
    private final Map<Kamyon, Double> kamyonMaliyet;          // toplam maliyet (TL)

    private Timeline timeline;
    private boolean simülasyonCalisiyor = false;

    // Yakıt parametreleri
    private static final double YAKIT_LITRE_KM = 0.3;   // km başına 0.3 litre
    private static final double LITRE_FIYATI = 40.0;    // 40 TL

    public SimulasyonServisi(KargoServisi servis) {
        this.servis = servis;
        this.rotaGraf = servis.getRotaGraf();
        this.kamyonRotalari = new HashMap<>();
        this.kamyonDurakIndex = new HashMap<>();
        this.kamyonYakıtTuketimi = new HashMap<>();
        this.kamyonMaliyet = new HashMap<>();
    }

    /**
     * Tüm kamyonlar için rotaları oluştur (mevcut kargolarının adreslerine göre)
     * Her kamyon için: mevcut konum (ilk durak) + kargoların adreslerinden benzersiz ilçeler
     */
    public void rotalariOlustur() {
        for (Kamyon kamyon : servis.getKamyonlar()) {
            List<String> rota = new ArrayList<>();
            // Başlangıç noktasını kamyonun mevcut konumundan al, yoksa varsayılan "Kadıköy"
            String baslangic = kamyon.getMevcutKonum();
            if (baslangic == null || baslangic.isBlank()) {
                baslangic = "Kadıköy";
            }
            rota.add(baslangic);

            // Kamyondaki kargoların adreslerinden ilçe isimlerini bul
            Set<String> durakSet = new LinkedHashSet<>();
            for (Kargo k : kamyon.getKargoStack()) {
                String ilce = extractIlce(k.getAdres());
                if (ilce != null && rotaGraf.getDugumler().contains(ilce)) {
                    durakSet.add(ilce);
                }
            }
            // Nearest neighbour ile sıralı rota oluştur (mevcut konumdan başlayarak)
            List<String> siraliDuraklar = new ArrayList<>(durakSet);
            if (!siraliDuraklar.isEmpty()) {
                List<String> siraliRota = rotaGraf.nearestNeighbourRoute(baslangic, siraliDuraklar);
                rota.addAll(siraliRota);
            }
            kamyonRotalari.put(kamyon, rota);
            kamyonDurakIndex.put(kamyon, 0);
            kamyonYakıtTuketimi.put(kamyon, 0.0);
            kamyonMaliyet.put(kamyon, 0.0);
        }
    }

    /**
     * Adres string'inden ilçe adını çıkarır (virgülden sonraki kısım).
     * Basit bir implementasyon, gerektiğinde geliştirilebilir.
     */
    private String extractIlce(String adres) {
        if (adres == null) return null;
        String[] parts = adres.split(",");
        if (parts.length > 0) {
            String ilce = parts[0].trim();
            return normalizeIlce(ilce);
        }
        return null;
    }

    /**
     * Türkçe karakterleri normalize eder (graf düğümleriyle uyum için).
     */
    private String normalizeIlce(String ilce) {
        return ilce.toLowerCase()
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replace(" ", "");
    }

    /**
     * Simülasyonu başlat (her 2 saniyede bir kamyonlar bir sonraki durağa hareket eder)
     */
    public void baslat() {
        if (simülasyonCalisiyor) return;
        rotalariOlustur();

        timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> adimAt()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        simülasyonCalisiyor = true;
    }

    public void durdur() {
        if (timeline != null) {
            timeline.stop();
        }
        simülasyonCalisiyor = false;
    }

    public boolean isCalisiyor() {
        return simülasyonCalisiyor;
    }

    /**
     * Her adımda tüm kamyonları bir sonraki durağa taşır, yakıt hesaplar,
     * varılan duraktaki kargoları teslim eder.
     */
    private void adimAt() {
        boolean herhangiHareket = false;
        for (Kamyon kamyon : servis.getKamyonlar()) {
            List<String> rota = kamyonRotalari.get(kamyon);
            Integer index = kamyonDurakIndex.get(kamyon);
            if (rota == null || index == null) continue;

            if (index + 1 < rota.size()) {
                String mevcutDurak = rota.get(index);
                String sonrakiDurak = rota.get(index + 1);
                // Mesafeyi al
                int mesafe = rotaGraf.getDistance(mevcutDurak, sonrakiDurak);
                if (mesafe > 0) {
                    // Yakıt ve maliyet hesapla
                    double yakit = mesafe * YAKIT_LITRE_KM;
                    double maliyet = yakit * LITRE_FIYATI;
                    kamyonYakıtTuketimi.put(kamyon, kamyonYakıtTuketimi.get(kamyon) + yakit);
                    kamyonMaliyet.put(kamyon, kamyonMaliyet.get(kamyon) + maliyet);

                    // Kamyonun konumunu güncelle
                    kamyonDurakIndex.put(kamyon, index + 1);
                    kamyon.setMevcutKonum(sonrakiDurak);
                    // Veritabanında kamyon konumunu güncelle (opsiyonel)
                    try {
                        servis.getDb().kamyonGuncelle(kamyon);
                    } catch (Exception ex) {
                        // log hatası
                    }

                    // Yeni durağa varınca, o durağa ait kargoları teslim et
                    teslimEt(kamyon, sonrakiDurak);

                    herhangiHareket = true;
                }
            }
        }
        if (!herhangiHareket) {
            // Tüm kamyonlar rotasını tamamladıysa simülasyonu durdur
            durdur();
            Main.showTrayNotification("Simülasyon Tamamlandı",
                    "Tüm kamyonlar rotalarını bitirdi.",
                    TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Belirtilen durağa ait kargoları kamyondan teslim eder.
     * (Adres içinde durak adı geçen kargolar teslim alınır)
     */
    private void teslimEt(Kamyon kamyon, String durak) {
        Stack<Kargo> stack = kamyon.getKargoStack();
        Stack<Kargo> kalanlar = new Stack<>();
        int teslimEdilen = 0;

        while (!stack.isEmpty()) {
            Kargo k = stack.pop();
            if (k.getAdres().toLowerCase().contains(durak.toLowerCase())) {
                // Kamyondan teslim (pop zaten yapıldı)
                // Durumu güncelle, teslim tarihini set et
                k.setDurum(Kargo.Durum.TESLIM_EDILDI);
                k.setTeslimTarihi(java.time.LocalDateTime.now());
                teslimEdilen++;
                // Veritabanını güncelle
                try {
                    servis.getDb().kargoGuncelle(k);
                    servis.getDb().logKargoKamyon(k.getId(), kamyon.getId(), "TESLIM_EDILDI");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                kalanlar.push(k);
            }
        }
        // Kalan kargoları geri koy (LIFO sırası korunur)
        while (!kalanlar.isEmpty()) {
            stack.push(kalanlar.pop());
        }

        if (teslimEdilen > 0) {
            Main.showTrayNotification("Teslimat",
                    kamyon.getPlaka() + " -> " + durak + " adresine " + teslimEdilen + " kargo teslim etti.",
                    TrayIcon.MessageType.INFO);
        }
    }

    // ==================== GETTER'LAR (UI'da göstermek için) ====================
    public Map<Kamyon, Double> getKamyonYakıtTuketimi() {
        return Collections.unmodifiableMap(kamyonYakıtTuketimi);
    }

    public Map<Kamyon, Double> getKamyonMaliyet() {
        return Collections.unmodifiableMap(kamyonMaliyet);
    }
}