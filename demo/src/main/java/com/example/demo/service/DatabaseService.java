package com.example.demo.service;

import com.example.demo.model.Fatura;
import com.example.demo.model.Kargo;
import com.example.demo.model.Kamyon;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {
    private static final String DB_URL = "jdbc:h2:./database/kargonet_db;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private Connection connection;

    public DatabaseService() {
        try {
            // Proje klasörü altında "database" dizini yoksa oluştur
            java.io.File dbDir = new java.io.File("./database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            createTables();
            if (isKargoTableEmpty()) {
                insertDemoData();
            }
            migrateExistingKargolarToMusteri(); // Eski verileri müşteri tablosuna taşı (opsiyonel)
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Veritabanı bağlantısı kurulamadı.");
        }
    }

    // ==================== TABLO OLUŞTURMA ====================

    private void createTables() throws SQLException {
        // 1. Müşteri tablosu (önce oluştur ki kargo tablosu foreign key kullanabilsin)
        String musteriTable = """
            CREATE TABLE IF NOT EXISTS musteri (
                id INT AUTO_INCREMENT PRIMARY KEY,
                ad_soyad VARCHAR(100),
                telefon VARCHAR(20),
                email VARCHAR(100),
                adres VARCHAR(200),
                kayit_tarihi TIMESTAMP
            )
        """;

        // 2. Kargo tablosu (gonderen/alici string olarak kalır, müşteri tablosuna opsiyonel foreign key)
        String kargoTable = """
            CREATE TABLE IF NOT EXISTS kargo (
                id VARCHAR(20) PRIMARY KEY,
                gonderen VARCHAR(100),
                alici VARCHAR(100),
                adres VARCHAR(200),
                tip VARCHAR(20),
                agirlik DOUBLE,
                hacim DOUBLE,
                durum VARCHAR(20),
                olusturma_tarihi TIMESTAMP,
                oncelik INT,
                teslim_tarihi TIMESTAMP NULL,
                gonderen_id INT NULL,
                alici_id INT NULL,
                FOREIGN KEY (gonderen_id) REFERENCES musteri(id),
                FOREIGN KEY (alici_id) REFERENCES musteri(id)
            )
        """;

        // 3. Kamyon tablosu (mevcut)
        String kamyonTable = """
            CREATE TABLE IF NOT EXISTS kamyon (
                id VARCHAR(10) PRIMARY KEY,
                plaka VARCHAR(20),
                max_agirlik DOUBLE,
                max_hacim DOUBLE,
                mevcut_agirlik DOUBLE,
                mevcut_hacim DOUBLE,
                aktif BOOLEAN,
                mevcut_konum VARCHAR(100)
            )
        """;

        // 4. Kargo-Kamyon log tablosu
        String kargoKamyonLogTable = """
            CREATE TABLE IF NOT EXISTS kargo_kamyon_log (
                id INT AUTO_INCREMENT PRIMARY KEY,
                kargo_id VARCHAR(20),
                kamyon_id VARCHAR(10),
                islem_tarihi TIMESTAMP,
                islem_tip VARCHAR(20),
                FOREIGN KEY (kargo_id) REFERENCES kargo(id),
                FOREIGN KEY (kamyon_id) REFERENCES kamyon(id)
            )
        """;

        // 5. Kamyon bakım tablosu
        String kamyonBakimTable = """
            CREATE TABLE IF NOT EXISTS kamyon_bakim (
                id INT AUTO_INCREMENT PRIMARY KEY,
                kamyon_id VARCHAR(10),
                bakim_tarihi DATE,
                kilometre INT,
                ariza_aciklamasi VARCHAR(500),
                ucret DOUBLE,
                FOREIGN KEY (kamyon_id) REFERENCES kamyon(id)
            )
        """;

        // 6. Fatura tablosu
        String faturaTable = """
            CREATE TABLE IF NOT EXISTS fatura (
                id INT AUTO_INCREMENT PRIMARY KEY,
                kargo_id VARCHAR(20),
                tutar DOUBLE,
                odeme_durumu VARCHAR(20),
                odeme_tarihi TIMESTAMP NULL,
                FOREIGN KEY (kargo_id) REFERENCES kargo(id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(musteriTable);
            stmt.execute(kargoTable);
            stmt.execute(kamyonTable);
            stmt.execute(kargoKamyonLogTable);
            stmt.execute(kamyonBakimTable);
            stmt.execute(faturaTable);
        }
    }

    // ==================== MİGRASYON (ESKİ VERİLERİ MÜŞTERİ TABLOSUNA TAŞI) ====================

    private void migrateExistingKargolarToMusteri() {
        try {
            // Eski kargolardan gonderen ve alici alanlarını alıp musteri tablosuna ekle
            String selectSql = "SELECT id, gonderen, alici FROM kargo WHERE gonderen_id IS NULL AND gonderen IS NOT NULL";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {
                while (rs.next()) {
                    String kargoId = rs.getString("id");
                    String gonderenAd = rs.getString("gonderen");
                    String aliciAd = rs.getString("alici");

                    int gonderenId = getOrCreateMusteri(gonderenAd, null, null, null);
                    int aliciId = getOrCreateMusteri(aliciAd, null, null, null);

                    String updateSql = "UPDATE kargo SET gonderen_id = ?, alici_id = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                        ps.setInt(1, gonderenId);
                        ps.setInt(2, aliciId);
                        ps.setString(3, kargoId);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getOrCreateMusteri(String adSoyad, String telefon, String email, String adres) throws SQLException {
        String selectSql = "SELECT id FROM musteri WHERE ad_soyad = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, adSoyad);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insertSql = "INSERT INTO musteri (ad_soyad, telefon, email, adres, kayit_tarihi) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, adSoyad);
            ps.setString(2, telefon);
            ps.setString(3, email);
            ps.setString(4, adres);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Müşteri eklenemedi");
    }

    // ==================== KARGO İŞLEMLERİ ====================

    public void kargoEkle(Kargo kargo) throws SQLException {
        String sql = """
            INSERT INTO kargo (id, gonderen, alici, adres, tip, agirlik, hacim, durum, olusturma_tarihi, oncelik, teslim_tarihi)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kargo.getId());
            ps.setString(2, kargo.getGonderen());
            ps.setString(3, kargo.getAlici());
            ps.setString(4, kargo.getAdres());
            ps.setString(5, kargo.getTip().name());
            ps.setDouble(6, kargo.getAgirlik());
            ps.setDouble(7, kargo.getHacim());
            ps.setString(8, kargo.getDurum().name());
            ps.setTimestamp(9, Timestamp.valueOf(kargo.getOlusturmaTarihi()));
            ps.setInt(10, kargo.getOncelik());
            ps.setTimestamp(11, kargo.getTeslimTarihi() == null ? null : Timestamp.valueOf(kargo.getTeslimTarihi()));
            ps.executeUpdate();
        }
    }

    public void kargoGuncelle(Kargo kargo) throws SQLException {
        String sql = """
            UPDATE kargo SET gonderen=?, alici=?, adres=?, tip=?, agirlik=?, hacim=?, durum=?, oncelik=?, teslim_tarihi=?
            WHERE id=?
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kargo.getGonderen());
            ps.setString(2, kargo.getAlici());
            ps.setString(3, kargo.getAdres());
            ps.setString(4, kargo.getTip().name());
            ps.setDouble(5, kargo.getAgirlik());
            ps.setDouble(6, kargo.getHacim());
            ps.setString(7, kargo.getDurum().name());
            ps.setInt(8, kargo.getOncelik());
            ps.setTimestamp(9, kargo.getTeslimTarihi() == null ? null : Timestamp.valueOf(kargo.getTeslimTarihi()));
            ps.setString(10, kargo.getId());
            ps.executeUpdate();
        }
    }

    public List<Kargo> tumKargolariGetir() throws SQLException {
        List<Kargo> list = new ArrayList<>();
        String sql = "SELECT * FROM kargo ORDER BY olusturma_tarihi";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Kargo k = new Kargo(
                        rs.getString("id"),
                        rs.getString("gonderen"),
                        rs.getString("alici"),
                        rs.getString("adres"),
                        Kargo.Tip.valueOf(rs.getString("tip")),
                        rs.getDouble("agirlik"),
                        rs.getDouble("hacim")
                );
                k.setDurum(Kargo.Durum.valueOf(rs.getString("durum")));
                Timestamp ts = rs.getTimestamp("teslim_tarihi");
                if (ts != null) k.setTeslimTarihi(ts.toLocalDateTime());
                list.add(k);
            }
        }
        return list;
    }

    private boolean isKargoTableEmpty() throws SQLException {
        String sql = "SELECT COUNT(*) FROM kargo";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    // ==================== KAMYON İŞLEMLERİ ====================

    public void kamyonEkle(Kamyon kamyon) throws SQLException {
        String sql = "INSERT INTO kamyon (id, plaka, max_agirlik, max_hacim, mevcut_agirlik, mevcut_hacim, aktif, mevcut_konum) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kamyon.getId());
            ps.setString(2, kamyon.getPlaka());
            ps.setDouble(3, kamyon.getMaxAgirlik());
            ps.setDouble(4, kamyon.getMaxHacim());
            ps.setDouble(5, kamyon.getMevcutAgirlik());
            ps.setDouble(6, kamyon.getMevcutHacim());
            ps.setBoolean(7, kamyon.isAktif());
            ps.setString(8, kamyon.getMevcutKonum());
            ps.executeUpdate();
        }
    }

    public void kamyonGuncelle(Kamyon kamyon) throws SQLException {
        String sql = "UPDATE kamyon SET plaka=?, max_agirlik=?, max_hacim=?, mevcut_agirlik=?, mevcut_hacim=?, aktif=?, mevcut_konum=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kamyon.getPlaka());
            ps.setDouble(2, kamyon.getMaxAgirlik());
            ps.setDouble(3, kamyon.getMaxHacim());
            ps.setDouble(4, kamyon.getMevcutAgirlik());
            ps.setDouble(5, kamyon.getMevcutHacim());
            ps.setBoolean(6, kamyon.isAktif());
            ps.setString(7, kamyon.getMevcutKonum());
            ps.setString(8, kamyon.getId());
            ps.executeUpdate();
        }
    }

    public List<Kamyon> tumKamyonlariGetir() throws SQLException {
        List<Kamyon> list = new ArrayList<>();
        String sql = "SELECT * FROM kamyon";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Kamyon k = new Kamyon(
                        rs.getString("id"),
                        rs.getString("plaka"),
                        rs.getDouble("max_agirlik"),
                        rs.getDouble("max_hacim")
                );
                k.setMevcutAgirlik(rs.getDouble("mevcut_agirlik"));
                k.setMevcutHacim(rs.getDouble("mevcut_hacim"));
                k.setAktif(rs.getBoolean("aktif"));
                k.setMevcutKonum(rs.getString("mevcut_konum"));
                list.add(k);
            }
        }
        return list;
    }

    // ==================== LOG (KARGO-KAMYON) ====================

    public void logKargoKamyon(String kargoId, String kamyonId, String islemTip) throws SQLException {
        String sql = "INSERT INTO kargo_kamyon_log (kargo_id, kamyon_id, islem_tarihi, islem_tip) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kargoId);
            ps.setString(2, kamyonId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, islemTip);
            ps.executeUpdate();
        }
    }

    // ==================== MÜŞTERİ İŞLEMLERİ ====================

    public int musteriEkle(String adSoyad, String telefon, String email, String adres) throws SQLException {
        return getOrCreateMusteri(adSoyad, telefon, email, adres);
    }

    public List<Map<String, Object>> musterileriListele() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM musteri ORDER BY ad_soyad";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("ad_soyad", rs.getString("ad_soyad"));
                row.put("telefon", rs.getString("telefon"));
                row.put("email", rs.getString("email"));
                row.put("adres", rs.getString("adres"));
                row.put("kayit_tarihi", rs.getTimestamp("kayit_tarihi"));
                list.add(row);
            }
        }
        return list;
    }

    // ==================== KAMYON BAKIM İŞLEMLERİ ====================

    public void kamyonBakimEkle(String kamyonId, LocalDate bakimTarihi, int kilometre, String ariza, double ucret) throws SQLException {
        String sql = "INSERT INTO kamyon_bakim (kamyon_id, bakim_tarihi, kilometre, ariza_aciklamasi, ucret) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kamyonId);
            ps.setDate(2, Date.valueOf(bakimTarihi));
            ps.setInt(3, kilometre);
            ps.setString(4, ariza);
            ps.setDouble(5, ucret);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> kamyonBakimListele(String kamyonId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM kamyon_bakim WHERE kamyon_id = ? ORDER BY bakim_tarihi DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kamyonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("bakim_tarihi", rs.getDate("bakim_tarihi"));
                    row.put("kilometre", rs.getInt("kilometre"));
                    row.put("ariza", rs.getString("ariza_aciklamasi"));
                    row.put("ucret", rs.getDouble("ucret"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    // ==================== FATURA İŞLEMLERİ ====================

    public void faturaOlustur(String kargoId, double tutar) throws SQLException {
        String sql = "INSERT INTO fatura (kargo_id, tutar, odeme_durumu) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, kargoId);
            ps.setDouble(2, tutar);
            ps.setString(3, Fatura.OdemeDurumu.ODEME_BEKLIYOR.name());
            ps.executeUpdate();
        }
    }

    public void faturaOdemeAl(String kargoId) throws SQLException {
        String sql = "UPDATE fatura SET odeme_durumu = ?, odeme_tarihi = ? WHERE kargo_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Fatura.OdemeDurumu.ODEME_ALINDI.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, kargoId);
            ps.executeUpdate();
        }
    }

    public double kargoUcretHesapla(Kargo kargo) {
        double base = 10.0;
        double agirlikFaktor = kargo.getAgirlik() * 2.5;
        double hacimFaktor = kargo.getHacim() * 50;
        double tipEk = switch (kargo.getTip()) {
            case VIP -> 30;
            case HIZLI -> 20;
            case BOZULABILIR -> 15;
            default -> 0;
        };
        return base + agirlikFaktor + hacimFaktor + tipEk;
    }

    // ==================== DİĞER ====================

    private void insertDemoData() throws SQLException {
        // Önce müşterileri ekle (gönderen ve alıcılar)
        int ahmetId = musteriEkle("Ahmet Yılmaz", "5551112233", "ahmet@mail.com", "Kadıköy, İstanbul");
        int mehmetId = musteriEkle("Mehmet Demir", "5552223344", "mehmet@mail.com", "Kadıköy, İstanbul");
        int fatmaId  = musteriEkle("Fatma Kaya", "5553334455", "fatma@mail.com", "Beşiktaş, İstanbul");
        int ayseId   = musteriEkle("Ayşe Çelik", "5554445566", "ayse@mail.com", "Beşiktaş, İstanbul");
        int aliId    = musteriEkle("Ali Öztürk", "5555556677", "ali@mail.com", "Şişli, İstanbul");
        int zeynepId = musteriEkle("Zeynep Arslan", "5556667788", "zeynep@mail.com", "Şişli, İstanbul");
        int huseyinId= musteriEkle("Hüseyin Şahin", "5557778899", "huseyin@mail.com", "Üsküdar, İstanbul");
        int canId    = musteriEkle("Can Yıldız", "5558889900", "can@mail.com", "Üsküdar, İstanbul");
        int elifId   = musteriEkle("Elif Aktaş", "5559990011", "elif@mail.com", "Maltepe, İstanbul");
        int denizId  = musteriEkle("Deniz Koç", "5550001122", "deniz@mail.com", "Maltepe, İstanbul");

        // Demo kargolar (artık gonderen/alici string olarak, foreign key'ler de dolduruldu)
        Kargo[] demos = {
                new Kargo("KRG1001", "Ahmet Yılmaz", "Mehmet Demir", "Kadıköy, İstanbul", Kargo.Tip.NORMAL, 5.0, 0.3),
                new Kargo("KRG1002", "Fatma Kaya", "Ayşe Çelik", "Beşiktaş, İstanbul", Kargo.Tip.VIP, 2.0, 0.1),
                new Kargo("KRG1003", "Ali Öztürk", "Zeynep Arslan", "Şişli, İstanbul", Kargo.Tip.HIZLI, 8.0, 0.5),
                new Kargo("KRG1004", "Hüseyin Şahin", "Can Yıldız", "Üsküdar, İstanbul", Kargo.Tip.BOZULABILIR, 3.5, 0.2),
                new Kargo("KRG1005", "Elif Aktaş", "Deniz Koç", "Maltepe, İstanbul", Kargo.Tip.NORMAL, 12.0, 0.8)
        };
        for (Kargo k : demos) {
            kargoEkle(k);
            // Foreign key güncellemeleri (sonradan set etmek daha doğru. Yukarıdaki kargoEkle foreign id'leri almaz, burada update yapalım)
            int gonderenId = getOrCreateMusteri(k.getGonderen(), null, null, null);
            int aliciId = getOrCreateMusteri(k.getAlici(), null, null, null);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE kargo SET gonderen_id=?, alici_id=? WHERE id=?")) {
                ps.setInt(1, gonderenId);
                ps.setInt(2, aliciId);
                ps.setString(3, k.getId());
                ps.executeUpdate();
            }
            // Fatura oluştur
            faturaOlustur(k.getId(), kargoUcretHesapla(k));
        }

        // Demo kamyonlar
        Kamyon k1 = new Kamyon("K1", "34 ABC 001", 1000, 50);
        Kamyon k2 = new Kamyon("K2", "34 DEF 002", 800, 40);
        Kamyon k3 = new Kamyon("K3", "34 GHI 003", 1200, 60);
        kamyonEkle(k1);
        kamyonEkle(k2);
        kamyonEkle(k3);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}