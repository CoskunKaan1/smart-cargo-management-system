package com.example.demo.ds;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Teslimat Rotası Grafiği - Dijkstra ile en kısa yol.
 * Şehirler/mahalleler node, mesafeler edge olarak tanımlanır.
 * Ek özellikler: Dinamik trafik (saat dilimine göre ağırlık değişimi),
 * Nearest Neighbour algoritması ile çoklu durak rotası oluşturma.
 */
public class RotaGraf {

    private final Map<String, Map<String, Integer>> adjacency = new LinkedHashMap<>();

    public void dugumEkle(String sehir) {
        adjacency.putIfAbsent(sehir, new LinkedHashMap<>());
    }

    public void kenarEkle(String kaynak, String hedef, int mesafe) {
        adjacency.computeIfAbsent(kaynak, k -> new LinkedHashMap<>()).put(hedef, mesafe);
        adjacency.computeIfAbsent(hedef, k -> new LinkedHashMap<>()).put(kaynak, mesafe);
    }

    public Set<String> getDugumler() {
        return adjacency.keySet();
    }

    public Map<String, Map<String, Integer>> getAdjacency() {
        return adjacency;
    }

    /**
     * İki düğüm arasındaki direk mesafeyi döndürür (edge yoksa -1)
     */
    public int getDistance(String from, String to) {
        Map<String, Integer> edges = adjacency.get(from);
        if (edges != null && edges.containsKey(to)) {
            return edges.get(to);
        }
        return -1;
    }

    // ==================== DİJKSKTRA ALGORİTMALARI ====================

    /**
     * Klasik Dijkstra – sabit ağırlıklarla en kısa yol.
     */
    public DijkstraResult dijkstra(String baslangic, String hedef) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));

        for (String node : adjacency.keySet()) dist.put(node, Integer.MAX_VALUE);
        dist.put(baslangic, 0);
        pq.add(baslangic);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(hedef)) break;
            Map<String, Integer> komsular = adjacency.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, Integer> e : komsular.entrySet()) {
                String v = e.getKey();
                int w = e.getValue();
                int newDist = dist.get(u) + w;
                if (newDist < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        List<String> yol = new ArrayList<>();
        String cur = hedef;
        while (cur != null) {
            yol.add(0, cur);
            cur = prev.get(cur);
        }
        if (yol.isEmpty() || !yol.get(0).equals(baslangic)) yol.clear();

        return new DijkstraResult(yol, dist.getOrDefault(hedef, Integer.MAX_VALUE));
    }

    /**
     * Dinamik Dijkstra – saat dilimine göre ağırlık değişimi (trafik etkisi).
     * Yoğun saatlerde (8-10, 17-19) mesafe 1.5 kat, gece (22-06) 0.8 kat.
     */
    public DijkstraResult dijkstraDynamic(String baslangic, String hedef, LocalDateTime time) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));

        for (String node : adjacency.keySet()) dist.put(node, Integer.MAX_VALUE);
        dist.put(baslangic, 0);
        pq.add(baslangic);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(hedef)) break;
            Map<String, Integer> komsular = adjacency.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, Integer> e : komsular.entrySet()) {
                String v = e.getKey();
                int w = getDynamicWeight(u, v, time);
                int newDist = dist.get(u) + w;
                if (newDist < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        List<String> yol = new ArrayList<>();
        String cur = hedef;
        while (cur != null) {
            yol.add(0, cur);
            cur = prev.get(cur);
        }
        if (yol.isEmpty() || !yol.get(0).equals(baslangic)) yol.clear();

        return new DijkstraResult(yol, dist.getOrDefault(hedef, Integer.MAX_VALUE));
    }

    /**
     * Saat dilimine göre dinamik ağırlık hesaplama.
     */
    private int getDynamicWeight(String from, String to, LocalDateTime time) {
        int baseWeight = adjacency.get(from).get(to);
        int hour = time.getHour();
        double multiplier = 1.0;
        if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) {
            multiplier = 1.5;      // trafik yoğun
        } else if (hour >= 22 || hour <= 6) {
            multiplier = 0.8;      // gece seyri
        }
        return (int) Math.round(baseWeight * multiplier);
    }

    // ==================== NEAREST NEIGHBOUR (TSP YAKLAŞIMI) ====================

    /**
     * Nearest neighbour algoritması – başlangıç noktasından başlayarak tüm düğümleri ziyaret eden yaklaşık rota.
     */
    public List<String> nearestNeighbourRoute(String start) {
        Set<String> unvisited = new LinkedHashSet<>(adjacency.keySet());
        List<String> route = new ArrayList<>();
        String current = start;
        route.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            String next = null;
            int minDist = Integer.MAX_VALUE;
            Map<String, Integer> edges = adjacency.get(current);
            for (String neighbor : unvisited) {
                if (edges.containsKey(neighbor)) {
                    int dist = edges.get(neighbor);
                    if (dist < minDist) {
                        minDist = dist;
                        next = neighbor;
                    }
                }
            }
            if (next == null) break; // bağlantı yok
            route.add(next);
            unvisited.remove(next);
            current = next;
        }
        return route;
    }

    /**
     * Nearest neighbour – sadece belirtilen ara noktaları (waypoints) ziyaret eder.
     * Başlangıç noktası start ile birlikte waypoints listesindeki her noktaya uğrayan rota.
     */
    public List<String> nearestNeighbourRoute(String start, List<String> waypoints) {
        List<String> unvisited = new ArrayList<>(waypoints);
        List<String> route = new ArrayList<>();
        String current = start;
        route.add(current);

        while (!unvisited.isEmpty()) {
            String next = null;
            int minDist = Integer.MAX_VALUE;
            for (String candidate : unvisited) {
                int dist = getDistance(current, candidate);
                if (dist > 0 && dist < minDist) {
                    minDist = dist;
                    next = candidate;
                }
            }
            if (next == null) break; // bağlantı yok, kalanlar ziyaret edilemiyor
            route.add(next);
            unvisited.remove(next);
            current = next;
        }
        return route;
    }

    // ==================== SONUÇ SINIFI ====================

    public static class DijkstraResult {
        public final List<String> yol;
        public final int toplam;
        public DijkstraResult(List<String> yol, int toplam) {
            this.yol = yol;
            this.toplam = toplam;
        }
    }

    // ==================== ÖRNEK İSTANBUL HARİTASI ====================

    /**
     * Varsayılan İstanbul haritasını yükler (Düzeltilmiş Türkçe karakterlerle).
     */
    public static RotaGraf ornekHaritaOlustur() {
        RotaGraf g = new RotaGraf();
        String[] sehirler = {
                "Kadıköy", "Üsküdar", "Beşiktaş", "Şişli", "Beyoğlu",
                "Fatih", "Bağcılar", "Bakırköy", "Ataşehir", "Maltepe",
                "Pendik", "Kartal", "Ümraniye", "Beykoz", "Sarıyer"
        };
        for (String s : sehirler) g.dugumEkle(s);

        g.kenarEkle("Kadıköy",   "Üsküdar",  8);
        g.kenarEkle("Kadıköy",   "Ataşehir", 12);
        g.kenarEkle("Kadıköy",   "Maltepe",  15);
        g.kenarEkle("Üsküdar",   "Beşiktaş", 10);
        g.kenarEkle("Üsküdar",   "Ümraniye", 14);
        g.kenarEkle("Üsküdar",   "Beykoz",   20);
        g.kenarEkle("Beşiktaş",  "Şişli",    7);
        g.kenarEkle("Beşiktaş",  "Beyoğlu",  5);
        g.kenarEkle("Şişli",     "Beyoğlu",  4);
        g.kenarEkle("Şişli",     "Bağcılar", 18);
        g.kenarEkle("Beyoğlu",   "Fatih",    6);
        g.kenarEkle("Fatih",     "Bağcılar", 12);
        g.kenarEkle("Fatih",     "Bakırköy", 16);
        g.kenarEkle("Bağcılar",  "Bakırköy", 9);
        g.kenarEkle("Maltepe",   "Kartal",   8);
        g.kenarEkle("Kartal",    "Pendik",   10);
        g.kenarEkle("Ataşehir",  "Ümraniye", 9);
        g.kenarEkle("Ümraniye",  "Beykoz",   15);
        g.kenarEkle("Sarıyer",   "Beykoz",   25);
        g.kenarEkle("Sarıyer",   "Şişli",    20);
        return g;
    }
}