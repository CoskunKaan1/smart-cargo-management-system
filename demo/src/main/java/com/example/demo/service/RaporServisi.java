package com.example.demo.service;

import com.example.demo.model.Kargo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class RaporServisi {

    private final List<Kargo> tumKargolar;

    public RaporServisi(List<Kargo> tumKargolar) {
        this.tumKargolar = tumKargolar;
    }

    // ==================== Günlük / Haftalık / Aylık Kargo Giriş Sayıları ====================

    /**
     * Son belirtilen gün sayısına ait günlük kargo giriş sayıları.
     * @param gunSayisi 30, 60 vb.
     * @return Tarih -> Kargo sayısı haritası
     */
    public Map<LocalDate, Long> gunlukKargoSayisi(int gunSayisi) {
        LocalDate now = LocalDate.now();
        return tumKargolar.stream()
                .filter(k -> k.getOlusturmaTarihi().toLocalDate().isAfter(now.minusDays(gunSayisi)))
                .collect(Collectors.groupingBy(k -> k.getOlusturmaTarihi().toLocalDate(), Collectors.counting()));
    }

    /**
     * Son belirtilen hafta sayısına ait haftalık kargo giriş sayıları.
     * @param haftaSayisi 12, 24 vb.
     * @return Hafta numarası (yıl bazında) -> kargo sayısı
     */
    public Map<Integer, Long> haftalikKargoSayisi(int haftaSayisi) {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        return tumKargolar.stream()
                .filter(k -> k.getOlusturmaTarihi().toLocalDate().isAfter(now.minusWeeks(haftaSayisi)))
                .collect(Collectors.groupingBy(k -> k.getOlusturmaTarihi().toLocalDate().get(weekFields.weekOfWeekBasedYear()),
                        Collectors.counting()));
    }

    /**
     * Son belirtilen ay sayısına ait aylık kargo giriş sayıları.
     * @param aySayisi 12, 24 vb.
     * @return "yyyy-MM" formatında ay -> kargo sayısı
     */
    public Map<String, Long> aylikKargoSayisi(int aySayisi) {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return tumKargolar.stream()
                .filter(k -> k.getOlusturmaTarihi().toLocalDate().isAfter(now.minusMonths(aySayisi)))
                .collect(Collectors.groupingBy(k -> k.getOlusturmaTarihi().format(formatter),
                        Collectors.counting()));
    }

    // ==================== Teslimat Süresi Ortalamaları (Tip Bazında) ====================

    /**
     * Teslim edilmiş kargoların tip bazında ortalama teslimat süresi (saat cinsinden).
     * @return Tip -> Ortalama saat haritası
     */
    public Map<Kargo.Tip, Double> ortalamaTeslimatSuresi() {
        return tumKargolar.stream()
                .filter(k -> k.getDurum() == Kargo.Durum.TESLIM_EDILDI && k.getTeslimTarihi() != null)
                .collect(Collectors.groupingBy(Kargo::getTip,
                        Collectors.averagingDouble(k -> ChronoUnit.HOURS.between(k.getOlusturmaTarihi(), k.getTeslimTarihi()))));
    }

    // ==================== En Yoğun Bölgeler (İlçe Bazında) ====================

    /**
     * Kargo adreslerinde geçen ilçe isimlerine göre en yoğun ilk 'limit' bölgeyi döndürür.
     * @param limit kaç tane döndürüleceği
     * @return İlçe adı -> kargo sayısı (sıralı)
     */
    public Map<String, Long> enYogunBolge(int limit) {
        List<String> ilceler = Arrays.asList(
                "Kadıköy", "Üsküdar", "Beşiktaş", "Şişli", "Beyoğlu",
                "Fatih", "Bağcılar", "Bakırköy", "Ataşehir", "Maltepe",
                "Pendik", "Kartal", "Ümraniye", "Beykoz", "Sarıyer"
        );
        return tumKargolar.stream()
                .map(Kargo::getAdres)
                .flatMap(adres -> ilceler.stream().filter(adres::contains).findFirst().stream())
                .collect(Collectors.groupingBy(ilce -> ilce, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    // ==================== Excel Raporu İçin Tablo Verisi ====================

    /**
     * Tüm kargoların listesini Excel satırlarına dönüştürmeye uygun formatta döndürür.
     * İlk satır başlık satırıdır.
     * @return List<List<Object>> – her bir iç liste bir satırı temsil eder
     */
    public List<List<Object>> getRaporTabloVerisi() {
        List<List<Object>> data = new ArrayList<>();
        // Başlık satırı
        data.add(Arrays.asList(
                "ID", "Gönderen", "Alıcı", "Tip", "Durum",
                "Oluşturma Tarihi", "Teslim Tarihi", "Teslim Süresi (saat)"
        ));
        // Veri satırları
        for (Kargo k : tumKargolar) {
            double sure = -1;
            if (k.getDurum() == Kargo.Durum.TESLIM_EDILDI && k.getTeslimTarihi() != null) {
                sure = ChronoUnit.HOURS.between(k.getOlusturmaTarihi(), k.getTeslimTarihi());
            }
            data.add(Arrays.asList(
                    k.getId(),
                    k.getGonderen(),
                    k.getAlici(),
                    k.getTip().name(),
                    k.getDurum().name(),
                    k.getTarihStr(),
                    k.getTeslimTarihi() == null ? "-" : k.getTeslimTarihi().toString(),
                    sure == -1 ? "-" : String.format("%.2f", sure)
            ));
        }
        return data;
    }
}