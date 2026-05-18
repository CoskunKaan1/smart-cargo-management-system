package com.example.demo.service;

import com.example.demo.ds.KargoBST;
import com.example.demo.ds.RotaGraf;
import com.example.demo.model.Kargo;
import com.example.demo.model.Kargo.Durum;
import com.example.demo.model.Kargo.Tip;
import com.example.demo.model.Kamyon;
import com.example.demo.Main;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class KargoServisi {

    // === VERİ YAPILARI (Bellek içi) ===
    private final HashMap<String, Kargo>       kargoHashMap    = new HashMap<>();    // O(1) arama
    private final PriorityQueue<Kargo>         oncelikKuyrugu  = new PriorityQueue<>(); // Min-heap (öncelik)
    private final LinkedList<Kargo>            normalKuyruk    = new LinkedList<>();
    private final KargoBST                     bst             = new KargoBST();     // AVL BST (alıcı sıralı)
    private final RotaGraf                     rotaGraf        = RotaGraf.ornekHaritaOlustur();
    private final List<Kamyon>                 kamyonlar       = new ArrayList<>();
    private final List<Kargo>                  tumKargolar     = new ArrayList<>();

    private final AtomicInteger idSayaci = new AtomicInteger(1000);

    // === Veritabanı Servisi ===
    private final DatabaseService db;

    // === Bozulabilir kargo zamanlayıcısı (2. adım) ===
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Kargo, ScheduledFuture<?>> bozulabilirTimerMap = new HashMap<>();

    public KargoServisi() {
        // 1. Veritabanı bağlantısını başlat (tabloları oluşturur, demo verileri ekler)
        db = new DatabaseService();

        // 2. Bellek yapılarını temizle
        tumKargolar.clear();
        kargoHashMap.clear();
        normalKuyruk.clear();
        oncelikKuyrugu.clear();
        bst.temizle();
        kamyonlar.clear();

        // 3. Kargoları veritabanından oku ve bellek yapılarına yükle
        try {
            List<Kargo> dbKargolar = db.tumKargolariGetir();
            for (Kargo k : dbKargolar) {
                tumKargolar.add(k);
                kargoHashMap.put(k.getId(), k);
                if (k.getDurum() == Durum.DEPODA) {  // Sadece depodakiler kuyruklara eklenir
                    if (k.getTip() == Tip.NORMAL) {
                        normalKuyruk.offer(k);
                    } else {
                        oncelikKuyrugu.offer(k);
                    }
                }
                bst.ekle(k);
                // Eğer kargo DEPODA değilse (YOLDA, TESLIM) kuyruklarda olmamalı.
                // Bozulabilir ve henüz teslim edilmemişse zamanlayıcıyı yeniden başlat (opsiyonel)
                if (k.getTip() == Tip.BOZULABILIR && k.getDurum() != Durum.TESLIM_EDILDI) {
                    scheduleBozulabilirUyarisi(k);
                }
            }

            // 4. Kamyonları veritabanından oku
            List<Kamyon> dbKamyonlar = db.tumKamyonlariGetir();
            // Kamyon mevcut ağırlık/hacim değerlerini kargo_kamyon_log üzerinden
            // yeniden hesapla. Böylece program yeniden başlatıldığında
            // teslim edilmiş kargolar hâlâ yüklüymüş gibi görünmez.
            // Her kamyon için: o kamyona YUKLENDI logu olan ama TESLIM_EDILDI
            // durumuna geçmemiş kargolar = gerçek yük.
            // Ancak kargo-kamyon eşleşmesi log tablosunda tutulduğundan burada
            // en güvenli yol: YOLDA durumunda hiç kargo yoksa tüm kamyonları sıfırla.
            boolean hicYoldaKargo = dbKargolar.stream()
                    .noneMatch(k -> k.getDurum() == Durum.YOLDA);
            for (Kamyon kamyon : dbKamyonlar) {
                if (hicYoldaKargo) {
                    kamyon.setMevcutAgirlik(0);
                    kamyon.setMevcutHacim(0);
                    try { db.kamyonGuncelle(kamyon); } catch (SQLException ignored) {}
                }
            }
            kamyonlar.addAll(dbKamyonlar);

            // 5. ID sayacını en büyük KRG ID'sine göre ayarla (örn: KRG1005 -> 1005)
            int maxId = tumKargolar.stream()
                    .map(Kargo::getId)
                    .filter(id -> id.startsWith("KRG"))
                    .map(id -> Integer.parseInt(id.substring(3)))
                    .max(Integer::compareTo)
                    .orElse(1000);
            idSayaci.set(maxId);

        } catch (SQLException e) {
            System.err.println("Veritabanından veri okunurken hata oluştu!");
            e.printStackTrace();
        }
    }

    // ==================== KARGO İŞLEMLERİ ====================

    public String kargoEkle(String gonderen, String alici, String adres,
                            Tip tip, double agirlik, double hacim) {
        String id = "KRG" + idSayaci.incrementAndGet();
        Kargo kargo = new Kargo(id, gonderen, alici, adres, tip, agirlik, hacim);

        // Bellek yapılarına ekle
        kargoHashMap.put(id, kargo);
        if (tip == Tip.NORMAL) {
            normalKuyruk.offer(kargo);
        } else {
            oncelikKuyrugu.offer(kargo);
        }
        bst.ekle(kargo);
        tumKargolar.add(kargo);

        // Veritabanına ekle
        try {
            db.kargoEkle(kargo);
            // Fatura oluştur
            double ucret = db.kargoUcretHesapla(kargo);
            db.faturaOlustur(kargo.getId(), ucret);
        } catch (SQLException e) {
            System.err.println("Kargo veritabanına eklenirken hata: " + e.getMessage());
        }

        // Bozulabilir kargo için zamanlayıcı başlat
        if (tip == Tip.BOZULABILIR) {
            scheduleBozulabilirUyarisi(kargo);
        }

        return id;
    }

    public Kargo kargoAra(String id) {
        return kargoHashMap.get(id);
    }

    public Kargo sonrakiKargoAl() {
        if (!oncelikKuyrugu.isEmpty()) return oncelikKuyrugu.poll();
        if (!normalKuyruk.isEmpty())   return normalKuyruk.poll();
        return null;
    }

    // ==================== KAMYON YÜKLEME ====================

    public String kamyonaYukle(Kamyon kamyon) {
        List<Kargo> geriEkle = new ArrayList<>();
        int yuklenen = 0;

        // Önce öncelikli kuyruktakileri yükle
        while (!oncelikKuyrugu.isEmpty()) {
            Kargo k = oncelikKuyrugu.poll();
            if (kamyon.kargoEkle(k)) {
                yuklenen++;
                // Bozulabilir zamanlayıcıyı iptal et (kargo yola çıktı)
                cancelBozulabilirTimer(k);
                try {
                    db.logKargoKamyon(k.getId(), kamyon.getId(), "YUKLENDI");
                    db.kargoGuncelle(k);      // durum YOLDA olarak güncellenir
                    db.kamyonGuncelle(kamyon);
                } catch (SQLException e) {
                    System.err.println("Yükleme log/güncelleme hatası: " + e.getMessage());
                }
            } else {
                geriEkle.add(k);
            }
        }
        oncelikKuyrugu.addAll(geriEkle);
        geriEkle.clear();

        // Normal kuyruktakileri dene
        Iterator<Kargo> it = normalKuyruk.iterator();
        while (it.hasNext()) {
            Kargo k = it.next();
            if (kamyon.kargoEkle(k)) {
                it.remove();
                yuklenen++;
                cancelBozulabilirTimer(k);
                try {
                    db.logKargoKamyon(k.getId(), kamyon.getId(), "YUKLENDI");
                    db.kargoGuncelle(k);
                    db.kamyonGuncelle(kamyon);
                } catch (SQLException e) {
                    System.err.println("Yükleme log/güncelleme hatası: " + e.getMessage());
                }
            }
        }

        return yuklenen + " kargo yüklendi. Doluluk: " +
                String.format("%.1f", kamyon.dolulukOrani()) + "%";
    }

    // Bozulabilir kargo alarmı zamanlayıcısı
    private void scheduleBozulabilirUyarisi(Kargo kargo) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (kargo.getDurum() != Durum.TESLIM_EDILDI) {
                Main.showTrayNotification("Bozulabilir Kargo Alarmı",
                        kargo.getId() + " - " + kargo.getAlici() + " adresine 2 saat içinde teslim edilmedi!",
                        java.awt.TrayIcon.MessageType.ERROR);
            }
        }, 2, TimeUnit.HOURS);
        bozulabilirTimerMap.put(kargo, future);
    }

    public void cancelBozulabilirTimer(Kargo kargo) {
        ScheduledFuture<?> future = bozulabilirTimerMap.remove(kargo);
        if (future != null) future.cancel(false);
    }

    // ==================== ROTA İŞLEMLERİ (3. ADIM) ====================

    public RotaGraf.DijkstraResult rotaHesapla(String baslangic, String hedef) {
        return rotaGraf.dijkstra(baslangic, hedef);
    }

    public RotaGraf.DijkstraResult rotaHesaplaDynamic(String baslangic, String hedef, LocalDateTime time) {
        return rotaGraf.dijkstraDynamic(baslangic, hedef, time);
    }

    public List<String> nearestNeighbourRoute(String start, List<String> waypoints) {
        return rotaGraf.nearestNeighbourRoute(start, waypoints);
    }

    // ==================== İSTATİSTİKLER ====================

    public long depodaKi() {
        return tumKargolar.stream().filter(k -> k.getDurum() == Durum.DEPODA).count();
    }

    public long yoldaKi() {
        return tumKargolar.stream().filter(k -> k.getDurum() == Durum.YOLDA).count();
    }

    public long teslimEdilenler() {
        return tumKargolar.stream().filter(k -> k.getDurum() == Durum.TESLIM_EDILDI).count();
    }

    public int toplamKargo() {
        return tumKargolar.size();
    }

    public int getBekleyenSayi() {
        return oncelikKuyrugu.size() + normalKuyruk.size();
    }

    // ==================== GETTER'LAR ====================

    public List<Kargo> getTumKargolar() {
        return Collections.unmodifiableList(tumKargolar);
    }

    public List<Kargo> getSiraliKargolar() {
        return bst.siraliListe();
    }

    public List<Kamyon> getKamyonlar() {
        return Collections.unmodifiableList(kamyonlar);
    }

    public RotaGraf getRotaGraf() {
        return rotaGraf;
    }

    public DatabaseService getDb() {
        return db;
    }

    // ==================== UYGULAMA KAPANIRKEN ====================
    public void close() {
        scheduler.shutdownNow();
        if (db != null) db.close();
    }
}