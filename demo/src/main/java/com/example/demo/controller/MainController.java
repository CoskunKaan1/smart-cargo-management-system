package com.example.demo.controller;

import com.example.demo.Main;
import com.example.demo.ds.RotaGraf;
import com.example.demo.model.Kamyon;
import com.example.demo.model.Kargo;
import com.example.demo.model.Kargo.Tip;
import com.example.demo.service.KargoServisi;
import com.example.demo.service.RaporServisi;
import com.example.demo.service.SimulasyonServisi;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import org.apache.poi.xssf.usermodel.XSSFSheet;

public class MainController implements Initializable {

    private KargoServisi servis;
    private SimulasyonServisi simulasyonServisi;

    @FXML private HBox navBar;

    // Navigasyon
    @FXML private ToggleButton btnDashboard, btnGiris, btnKamyon, btnSorgulama, btnRota, btnBst, btnRaporlar, btnMusteriler, btnBakim, btnFaturalar;
    @FXML private VBox dashboardPanel, kargoGirisPanel, kamyonPanel, sorgulamaPanel, rotaPanel, bstPanel;
    private ToggleGroup toggleGroup;

    // Dashboard
    @FXML private Label lblToplamKargo, lblDepodaki, lblYoldaki, lblTeslim, lblBekleyen;
    @FXML private VBox kargoListBox;

    // Kargo Giriş
    @FXML private TextField gonderenField, aliciField, adresField, agirlikField, hacimField;
    @FXML private ComboBox<String> tipCombo;
    @FXML private Label tipAcikLabel, kargoGirisSonucLabel;

    // Kamyon
    @FXML private VBox kamyonListBox;
    @FXML private Label kamyonInfoLabel;

    // Sorgulama
    @FXML private TextField aramaField;
    @FXML private VBox sorgulamaSonucBox;

    // Rota
    @FXML private ComboBox<String> baslangicCombo, hedefCombo;
    @FXML private Label rotaSonucLabel;
    @FXML private Canvas grafCanvas;

    // BST Liste
    @FXML private VBox bstListeBox;

    // Raporlar Paneli
    @FXML private VBox raporlarPanel;
    @FXML private TabPane raporTabPane;

    // Müşteriler Paneli
    @FXML private VBox musterilerPanel;
    @FXML private VBox musteriListBox;
    @FXML private Button musteriYenileBtn;

    // Bakım Paneli
    @FXML private VBox bakimPanel;
    @FXML private ComboBox<String> bakimKamyonCombo;
    @FXML private VBox bakimListBox;
    @FXML private ListView<String> bakimListView;
    @FXML private Button bakimYenileBtn;

    // Faturalar Paneli
    @FXML private VBox faturalarPanel;
    @FXML private TextField faturaKargoIdField;
    @FXML private Label faturaSonucLabel;
    @FXML private VBox faturaListBox;
    @FXML private Button faturaSorgulaBtn;

    // Rota için trafik kontrolü (dinamik rota)
    private DatePicker tarihPicker;
    private ComboBox<Integer> saatCombo;

    // Simülasyon Kontrolleri
    private Button btnSimBaslat, btnSimDurdur;
    private Label lblYakitMaliyet;
    private Timeline maliyetTimeline;

    // Harita koordinatları (çizim için)
    private static final Map<String, double[]> POZISYONLAR = new LinkedHashMap<>();
    static {
        POZISYONLAR.put("Sariyer",    new double[]{150, 60});
        POZISYONLAR.put("Beykoz",     new double[]{480, 80});
        POZISYONLAR.put("Besiktas",   new double[]{200, 160});
        POZISYONLAR.put("Sisli",      new double[]{250, 120});
        POZISYONLAR.put("Beyoglu",    new double[]{220, 200});
        POZISYONLAR.put("Fatih",      new double[]{200, 270});
        POZISYONLAR.put("Uskudar",    new double[]{400, 175});
        POZISYONLAR.put("Kadikoy",    new double[]{380, 260});
        POZISYONLAR.put("Atasehir",   new double[]{480, 240});
        POZISYONLAR.put("Umraniye",   new double[]{500, 160});
        POZISYONLAR.put("Bagcilar",   new double[]{120, 240});
        POZISYONLAR.put("Bakirkoy",   new double[]{100, 320});
        POZISYONLAR.put("Maltepe",    new double[]{400, 350});
        POZISYONLAR.put("Kartal",     new double[]{490, 350});
        POZISYONLAR.put("Pendik",     new double[]{590, 370});
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        servis = Main.servis;
        simulasyonServisi = new SimulasyonServisi(servis);

        // Navigasyon Grubu
        toggleGroup = new ToggleGroup();
        btnDashboard.setToggleGroup(toggleGroup);
        btnGiris.setToggleGroup(toggleGroup);
        btnKamyon.setToggleGroup(toggleGroup);
        btnSorgulama.setToggleGroup(toggleGroup);
        btnRota.setToggleGroup(toggleGroup);
        btnBst.setToggleGroup(toggleGroup);
        btnRaporlar.setToggleGroup(toggleGroup);
        btnBakim.setToggleGroup(toggleGroup);
        btnMusteriler.setToggleGroup(toggleGroup);
        btnFaturalar.setToggleGroup(toggleGroup);
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) o.setSelected(true);
        });

        // Kargo giriş
        tipCombo.getItems().addAll("NORMAL", "VIP", "HIZLI", "BOZULABILIR");
        tipCombo.setValue("NORMAL");
        onTipDegisti();
        gonderenField.setOnAction(e -> onKaydet());
        aliciField.setOnAction(e -> onKaydet());
        adresField.setOnAction(e -> onKaydet());

        // Rota comboları doldur
        Set<String> dugumler = servis.getRotaGraf().getDugumler();
        baslangicCombo.getItems().addAll(dugumler);
        hedefCombo.getItems().addAll(dugumler);
        if (!dugumler.isEmpty()) {
            baslangicCombo.setValue(dugumler.iterator().next());
            String son = null;
            for (String s : dugumler) son = s;
            hedefCombo.setValue(son);
        }

        // Dinamik rota için tarih ve saat seçicileri ekle (Rota panelinin üstüne)
        tarihPicker = new DatePicker(LocalDate.now());
        saatCombo = new ComboBox<>();
        for (int i = 0; i <= 23; i++) saatCombo.getItems().add(i);
        saatCombo.setValue(LocalDateTime.now().getHour());
        HBox dynamicRow = new HBox(10, new Label("Tarih:"), tarihPicker, new Label("Saat:"), saatCombo);
        dynamicRow.setAlignment(Pos.CENTER_LEFT);
        dynamicRow.setPadding(new Insets(5, 0, 5, 0));
        // Rota panelindeki ilk HBox'tan sonra eklemek için (FXML'de id yoksa programatik)
        rotaPanel.getChildren().add(2, dynamicRow);

        // Simülasyon butonlarını kamyon panelinin üstüne ekle
        btnSimBaslat = new Button("▶ Simülasyon Başlat");
        btnSimDurdur = new Button("■ Durdur");
        btnSimDurdur.setDisable(true);
        lblYakitMaliyet = new Label("Yakıt/Maliyet: -");
        btnSimBaslat.setOnAction(e -> baslatSimulasyon());
        btnSimDurdur.setOnAction(e -> durdurSimulasyon());
        HBox simBox = new HBox(10, btnSimBaslat, btnSimDurdur, lblYakitMaliyet);
        simBox.setPadding(new Insets(10));
        simBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8;");
        kamyonPanel.getChildren().add(0, simBox);

        // Raporlar tablarını oluştur
        raporTablariOlustur();

        // Başlangıç ekranı
        onDashboard();
    }

    // ==================== NAVİGASYON ====================
    private void panelleriGizle() {
        dashboardPanel.setVisible(false);
        kargoGirisPanel.setVisible(false);
        kamyonPanel.setVisible(false);
        sorgulamaPanel.setVisible(false);
        rotaPanel.setVisible(false);
        bstPanel.setVisible(false);
        raporlarPanel.setVisible(false);
        musterilerPanel.setVisible(false);
        bakimPanel.setVisible(false);
        faturalarPanel.setVisible(false);
    }

    private void gosterAnimasyonlu(VBox panel) {
        panelleriGizle();
        panel.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(300), panel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    @FXML public void onDashboard() { gosterAnimasyonlu(dashboardPanel); yenileDashboard(); }
    @FXML public void onGiris() { gosterAnimasyonlu(kargoGirisPanel); }
    @FXML public void onKamyon() { gosterAnimasyonlu(kamyonPanel); yenileKamyon(); }
    @FXML public void onSorgulama() { gosterAnimasyonlu(sorgulamaPanel); }
    @FXML public void onRota() { gosterAnimasyonlu(rotaPanel); grafCiz(Collections.emptyList()); }
    @FXML public void onBst() { gosterAnimasyonlu(bstPanel); yenileBst(); }

    // ==================== DASHBOARD ====================
    private void yenileDashboard() {
        lblToplamKargo.setText(String.valueOf(servis.toplamKargo()));
        lblDepodaki.setText(String.valueOf(servis.depodaKi()));
        lblYoldaki.setText(String.valueOf(servis.yoldaKi()));
        lblTeslim.setText(String.valueOf(servis.teslimEdilenler()));
        lblBekleyen.setText(String.valueOf(servis.getBekleyenSayi()));

        kargoListBox.getChildren().clear();
        List<Kargo> kargolar = servis.getTumKargolar();
        for (int i = kargolar.size() - 1; i >= 0 && i >= kargolar.size() - 20; i--) {
            Kargo k = kargolar.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add("kargo-satir");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            Label idLbl = new Label(k.getId()); idLbl.getStyleClass().add("kargo-id"); idLbl.setPrefWidth(95);
            Label isim = new Label(k.getGonderen() + " > " + k.getAlici()); isim.getStyleClass().add("kargo-isim"); HBox.setHgrow(isim, Priority.ALWAYS);
            Label adres = new Label(k.getAdres()); adres.getStyleClass().add("kargo-adres"); adres.setPrefWidth(200);
            Label agirlik = new Label(String.format("%.1f kg", k.getAgirlik())); agirlik.getStyleClass().add("kargo-adres"); agirlik.setPrefWidth(70);
            row.getChildren().addAll(idLbl, tipBadge(k), isim, adres, agirlik, durumBadge(k));
            kargoListBox.getChildren().add(row);
        }
    }

    // ==================== KARGO GİRİŞ ====================
    @FXML public void onTipDegisti() {
        String v = tipCombo.getValue();
        if (v == null) return;
        switch (v) {
            case "NORMAL":      tipAcikLabel.setText("Standart Queue  - Oncelik: 4"); break;
            case "VIP":         tipAcikLabel.setText("Priority Queue  - Oncelik: 2"); break;
            case "HIZLI":       tipAcikLabel.setText("Priority Queue  - Oncelik: 3"); break;
            case "BOZULABILIR": tipAcikLabel.setText("Priority Queue  - Oncelik: 1 (EN YUKSEK)"); break;
        }
    }

    @FXML public void onKaydet() {
        try {
            String gonderen = gonderenField.getText().trim();
            String alici    = aliciField.getText().trim();
            String adres    = adresField.getText().trim();
            double agirlik  = Double.parseDouble(agirlikField.getText().trim());
            double hacim    = Double.parseDouble(hacimField.getText().trim());
            Tip tip         = Tip.valueOf(tipCombo.getValue());

            if (gonderen.isEmpty() || alici.isEmpty() || adres.isEmpty()) {
                kargoGirisSonucLabel.setText("Lutfen tum alanlari doldurun.");
                kargoGirisSonucLabel.setStyle("-fx-text-fill: #f59e0b;");
                return;
            }

            String id = servis.kargoEkle(gonderen, alici, adres, tip, agirlik, hacim);
            String kuyruk = (tip == Tip.NORMAL) ? "Normal Queue" : "Priority Queue (Oncelik: " + tip.ordinal() + ")";
            kargoGirisSonucLabel.setText("Kargo basariyla kaydedildi!\nID: " + id + "\n" + kuyruk + "'a eklendi.");
            kargoGirisSonucLabel.setStyle("-fx-text-fill: #10b981;");
            yenileDashboard();
        } catch (NumberFormatException ex) {
            kargoGirisSonucLabel.setText("Agirlik ve hacim sayisal deger olmali (ornek: 5.0)");
            kargoGirisSonucLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML public void onTemizle() {
        gonderenField.clear(); aliciField.clear(); adresField.clear();
        agirlikField.setText("1.0"); hacimField.setText("0.1");
        tipCombo.setValue("NORMAL");
        kargoGirisSonucLabel.setText("");
    }

    // ==================== KAMYON (Stack görünümü) ====================
    private void yenileKamyon() {
        kamyonListBox.getChildren().clear();
        for (Kamyon k : servis.getKamyonlar()) {
            VBox kart = new VBox(12);
            kart.getStyleClass().add("kamyon-kart");
            kart.setPadding(new Insets(16));

            HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
            Label plaka = new Label("Kamyon: " + k.getPlaka()); plaka.getStyleClass().add("kamyon-plaka");
            Label adet = new Label(k.getKargoSayisi() + " kargo"); adet.getStyleClass().add("badge");
            header.getChildren().addAll(plaka, adet);

            GridPane pg = new GridPane(); pg.setHgap(12); pg.setVgap(8);
            ProgressBar agirlikBar = new ProgressBar(k.agirlikDolulukOrani() / 100); agirlikBar.setPrefWidth(280); agirlikBar.getStyleClass().add("progress-bar-custom");
            ProgressBar hacimBar = new ProgressBar(k.hacimDolulukOrani() / 100); hacimBar.setPrefWidth(280); hacimBar.getStyleClass().add("progress-bar-custom");
            Label lblA = new Label("Agirlik:"); lblA.getStyleClass().add("form-label");
            Label lblAVal = new Label(String.format("%.1f/%.0f kg (%.0f%%)", k.getMevcutAgirlik(), k.getMaxAgirlik(), k.agirlikDolulukOrani())); lblAVal.getStyleClass().add("form-label");
            Label lblH = new Label("Hacim:"); lblH.getStyleClass().add("form-label");
            Label lblHVal = new Label(String.format("%.2f/%.0f m3 (%.0f%%)", k.getMevcutHacim(), k.getMaxHacim(), k.hacimDolulukOrani())); lblHVal.getStyleClass().add("form-label");
            pg.add(lblA, 0, 0); pg.add(agirlikBar, 1, 0); pg.add(lblAVal, 2, 0);
            pg.add(lblH, 0, 1); pg.add(hacimBar, 1, 1); pg.add(lblHVal, 2, 1);

            Button yukleBtn = new Button("Kuyruğu Yükle (Knapsack)"); yukleBtn.getStyleClass().add("btn-primary");
            yukleBtn.setOnAction(e -> {
                String msg = servis.kamyonaYukle(k);
                kamyonInfoLabel.setText(k.getPlaka() + ": " + msg);
                kamyonInfoLabel.setStyle("-fx-text-fill: #10b981;");
                yenileKamyon();
                yenileDashboard();
            });
            Button teslimBtn = new Button("Teslim Et (Pop)"); teslimBtn.getStyleClass().add("btn-secondary");
            teslimBtn.setOnAction(e -> {
                Kargo kargo = k.kargoTeslimEt();
                if (kargo != null) {
                    kamyonInfoLabel.setText("Teslim edildi: " + kargo.getId() + " -> " + kargo.getAlici() + " (" + kargo.getAdres() + ")");
                    kamyonInfoLabel.setStyle("-fx-text-fill: #10b981;");
                } else {
                    kamyonInfoLabel.setText("Kamyonda kargo yok.");
                    kamyonInfoLabel.setStyle("-fx-text-fill: #f59e0b;");
                }
                yenileKamyon();
                yenileDashboard();
            });
            HBox btnRow = new HBox(10, yukleBtn, teslimBtn);

            Label stackBaslik = new Label("Stack (Ust = Sonraki Teslimat - LIFO):"); stackBaslik.getStyleClass().add("section-baslik");
            VBox stackBox = new VBox(4);
            List<Kargo> stackList = new ArrayList<>(k.getKargoStack());
            if (stackList.isEmpty()) {
                Label bos = new Label("  -- Stack bos --"); bos.getStyleClass().add("info-text"); stackBox.getChildren().add(bos);
            } else {
                for (int i = stackList.size() - 1; i >= 0; i--) {
                    Kargo kargo = stackList.get(i);
                    HBox row = new HBox(10); row.getStyleClass().add("stack-satir"); row.setPadding(new Insets(6, 12, 6, 12)); row.setAlignment(Pos.CENTER_LEFT);
                    Label topLbl = new Label(i == stackList.size() - 1 ? "TOP" : "   "); topLbl.getStyleClass().add(i == stackList.size() - 1 ? "stack-top" : "form-label"); topLbl.setPrefWidth(50);
                    Label kId = new Label(kargo.getId()); kId.getStyleClass().add("kargo-id"); kId.setPrefWidth(90);
                    Label isim = new Label(kargo.getAlici() + " - " + kargo.getAdres()); isim.getStyleClass().add("kargo-isim");
                    row.getChildren().addAll(topLbl, kId, tipBadge(kargo), isim);
                    stackBox.getChildren().add(row);
                }
            }
            kart.getChildren().addAll(header, pg, btnRow, stackBaslik, stackBox);
            kamyonListBox.getChildren().add(kart);
        }
    }

    // ==================== SORGULAMA ====================
    @FXML public void onAra() {
        sorgulamaSonucBox.getChildren().clear();
        String id = aramaField.getText().trim().toUpperCase();
        if (id.isEmpty()) return;
        Kargo k = servis.kargoAra(id);
        if (k == null) {
            Label lbl = new Label("Kargo bulunamadi: " + id);
            lbl.getStyleClass().add("sonuc-label");
            lbl.setStyle("-fx-text-fill: #ef4444;");
            sorgulamaSonucBox.getChildren().add(lbl);
            return;
        }
        VBox box = new VBox(10); box.getStyleClass().add("detay-kart"); box.setPadding(new Insets(20)); box.setMaxWidth(600);
        Label header = new Label("Kargo: " + k.getId()); header.getStyleClass().add("detay-baslik");
        GridPane grid = new GridPane(); grid.setHgap(20); grid.setVgap(10);
        String[][] rows = {
                {"Gonderici:", k.getGonderen()}, {"Alici:", k.getAlici()}, {"Adres:", k.getAdres()},
                {"Tip:", k.getTip().name()}, {"Agirlik:", String.format("%.2f kg", k.getAgirlik())},
                {"Hacim:", String.format("%.2f m3", k.getHacim())}, {"Kayit Tarihi:", k.getTarihStr()},
        };
        for (int i = 0; i < rows.length; i++) {
            Label lbl = new Label(rows[i][0]); lbl.getStyleClass().add("form-label");
            Label val = new Label(rows[i][1]); val.getStyleClass().add("detay-val");
            grid.add(lbl, 0, i); grid.add(val, 1, i);
        }
        Label hiz = new Label("HashMap O(1) erisim - tum kargolarin arasinda arama yapilmadi.");
        box.getChildren().addAll(header, grid, durumBadge(k), hiz);
        sorgulamaSonucBox.getChildren().add(box);
    }

    @FXML public void onTumIdler() {
        sorgulamaSonucBox.getChildren().clear();
        Label baslik = new Label("Mevcut Kargo IDleri (HashMap keys):");
        sorgulamaSonucBox.getChildren().add(baslik);
        for (Kargo k : servis.getTumKargolar()) {
            HBox row = new HBox(12); row.getStyleClass().add("kargo-satir"); row.setPadding(new Insets(8, 14, 8, 14)); row.setAlignment(Pos.CENTER_LEFT);
            Label idLbl = new Label(k.getId()); idLbl.getStyleClass().add("kargo-id"); idLbl.setStyle("-fx-cursor: hand; -fx-underline: true;");
            idLbl.setOnMouseClicked(e -> { aramaField.setText(k.getId()); onAra(); }); idLbl.setPrefWidth(100);
            Label isim = new Label(k.getGonderen() + " > " + k.getAlici()); isim.getStyleClass().add("kargo-isim"); HBox.setHgrow(isim, Priority.ALWAYS);
            row.getChildren().addAll(idLbl, isim, durumBadge(k));
            sorgulamaSonucBox.getChildren().add(row);
        }
    }

    // ==================== ROTA (Dijkstra) ====================
    @FXML public void onHesapla() {
        String bas = baslangicCombo.getValue();
        String hdf = hedefCombo.getValue();
        if (bas == null || hdf == null || bas.equals(hdf)) {
            rotaSonucLabel.setText("Lutfen farkli baslangic ve hedef secin.");
            rotaSonucLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }
        RotaGraf.DijkstraResult result = servis.rotaHesapla(bas, hdf);
        if (result.yol.isEmpty()) {
            rotaSonucLabel.setText("Rota bulunamadi: " + bas + " > " + hdf);
            rotaSonucLabel.setStyle("-fx-text-fill: #ef4444;");
        } else {
            rotaSonucLabel.setText("En kisa rota (" + result.toplam + " km):  " + String.join(" > ", result.yol) + "  |  " + result.yol.size() + " durak");
            rotaSonucLabel.setStyle("-fx-text-fill: #10b981;");
            grafCiz(result.yol);
        }
    }

    @FXML public void onDinamikHesapla() {
        String bas = baslangicCombo.getValue();
        String hdf = hedefCombo.getValue();
        if (bas == null || hdf == null || bas.equals(hdf)) {
            rotaSonucLabel.setText("Lutfen farkli baslangic ve hedef secin.");
            rotaSonucLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }
        LocalDateTime time = LocalDateTime.of(tarihPicker.getValue(), LocalTime.of(saatCombo.getValue(), 0));
        RotaGraf.DijkstraResult result = servis.rotaHesaplaDynamic(bas, hdf, time);
        if (result.yol.isEmpty()) {
            rotaSonucLabel.setText("Rota bulunamadi: " + bas + " > " + hdf + " (Dinamik)");
            rotaSonucLabel.setStyle("-fx-text-fill: #ef4444;");
        } else {
            rotaSonucLabel.setText("Dinamik rota (" + result.toplam + " km, saat " + saatCombo.getValue() + "): " + String.join(" > ", result.yol));
            grafCiz(result.yol);
        }
    }

    @FXML public void onNearestNeighbour() {
        String start = baslangicCombo.getValue();
        if (start == null) return;
        List<String> route = servis.nearestNeighbourRoute(start, new ArrayList<>(servis.getRotaGraf().getDugumler()));
        rotaSonucLabel.setText("En yakın komşu rotası: " + String.join(" → ", route));
        grafCiz(route);
    }

    private void grafCiz(List<String> yol) {
        GraphicsContext gc = grafCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, grafCanvas.getWidth(), grafCanvas.getHeight());
        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(0, 0, grafCanvas.getWidth(), grafCanvas.getHeight());

        Map<String, Map<String, Integer>> adj = servis.getRotaGraf().getAdjacency();
        for (Map.Entry<String, Map<String, Integer>> e : adj.entrySet()) {
            String src = normalizeKey(e.getKey());
            double[] p1 = POZISYONLAR.get(src);
            if (p1 == null) continue;
            for (Map.Entry<String, Integer> edge : e.getValue().entrySet()) {
                String dst = normalizeKey(edge.getKey());
                double[] p2 = POZISYONLAR.get(dst);
                if (p2 == null) continue;
                boolean onYol = yolBaglantisi(yol, e.getKey(), edge.getKey());
                gc.setStroke(onYol ? Color.web("#f59e0b") : Color.web("#334155"));
                gc.setLineWidth(onYol ? 3.5 : 1.5);
                gc.strokeLine(p1[0], p1[1], p2[0], p2[1]);
                if (onYol) {
                    gc.setFill(Color.web("#f59e0b"));
                    gc.setFont(Font.font("monospace", FontWeight.BOLD, 11));
                    gc.fillText(edge.getValue() + "km", (p1[0]+p2[0])/2+3, (p1[1]+p2[1])/2-3);
                }
            }
        }
        for (Map.Entry<String, double[]> e : POZISYONLAR.entrySet()) {
            String name = e.getKey();
            double[] p = e.getValue();
            String original = findOriginal(name);
            boolean onYol = original != null && yol.contains(original);
            boolean ucNokta = !yol.isEmpty() && original != null && (original.equals(yol.get(0)) || original.equals(yol.get(yol.size()-1)));
            double r = ucNokta ? 12 : (onYol ? 10 : 7);
            gc.setFill(ucNokta ? Color.web("#10b981") : (onYol ? Color.web("#f59e0b") : Color.web("#1e3a5f")));
            gc.setStroke(ucNokta ? Color.web("#34d399") : (onYol ? Color.web("#fbbf24") : Color.web("#3b82f6")));
            gc.setLineWidth(2);
            gc.fillOval(p[0]-r, p[1]-r, r*2, r*2);
            gc.strokeOval(p[0]-r, p[1]-r, r*2, r*2);
            gc.setFill(onYol ? Color.WHITE : Color.web("#94a3b8"));
            gc.setFont(Font.font("monospace", FontWeight.BOLD, onYol ? 12 : 11));
            gc.fillText(name, p[0]+r+3, p[1]+4);
        }
    }

    private String normalizeKey(String s) {
        return s.replace("\u015e","S").replace("\u015f","s").replace("\u00dc","U").replace("\u00fc","u")
                .replace("\u0130","I").replace("\u0131","i").replace("\u00d6","O").replace("\u00f6","o")
                .replace("\u00c7","C").replace("\u00e7","c").replace("\u011e","G").replace("\u011f","g");
    }

    private String findOriginal(String normalized) {
        for (String key : servis.getRotaGraf().getDugumler()) {
            if (normalizeKey(key).equals(normalized)) return key;
        }
        return null;
    }

    private boolean yolBaglantisi(List<String> yol, String a, String b) {
        for (int i = 0; i < yol.size()-1; i++) {
            if ((yol.get(i).equals(a) && yol.get(i+1).equals(b)) || (yol.get(i).equals(b) && yol.get(i+1).equals(a))) return true;
        }
        return false;
    }

    // ==================== BST LİSTE ====================
    @FXML public void onYenile() { yenileBst(); }
    private void yenileBst() {
        bstListeBox.getChildren().clear();
        List<Kargo> sirali = servis.getSiraliKargolar();
        if (sirali.isEmpty()) {
            Label bos = new Label("  -- Kayitli kargo yok --"); bos.getStyleClass().add("info-text");
            bstListeBox.getChildren().add(bos);
            return;
        }
        for (int i = 0; i < sirali.size(); i++) {
            Kargo k = sirali.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add(i % 2 == 0 ? "kargo-satir" : "kargo-satir-alt");
            row.setPadding(new Insets(9, 16, 9, 16)); row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(
                    colVal(String.valueOf(i+1), 50, "#64748b"),
                    colVal(k.getId(), 95, "#60a5fa"),
                    colVal(k.getAlici(), 170, "#f1f5f9"),
                    colVal(k.getGonderen(), 160, "#94a3b8"),
                    tipBadge(k),
                    colVal(k.getAdres(), 200, "#94a3b8"),
                    colVal(String.format("%.1fkg", k.getAgirlik()), 75, "#cbd5e1"),
                    durumBadge(k)
            );
            bstListeBox.getChildren().add(row);
        }
    }

    //raporlar düğmesine basınca
    @FXML private void onRaporlar() {
        gosterAnimasyonlu(raporlarPanel);
    }

    private void raporTablariOlustur() {
        // Tab 1: Günlük/Haftalık/Aylık
        Tab tabGiris = new Tab("Kargo Giriş Grafiği");
        tabGiris.setClosable(false);
        VBox girisBox = new VBox(10);
        girisBox.setPadding(new Insets(10));
        ComboBox<String> periyotCombo = new ComboBox<>();
        periyotCombo.getItems().addAll("Günlük (30 gün)", "Haftalık (12 hafta)", "Aylık (12 ay)");
        periyotCombo.setValue("Günlük (30 gün)");
        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(new javafx.scene.chart.CategoryAxis(), new javafx.scene.chart.NumberAxis());
        barChart.setTitle("Kargo Giriş Sayısı");
        barChart.setAnimated(false);
        Button yenileBtn = new Button("Yenile");
        yenileBtn.setOnAction(e -> {
            RaporServisi rs = new RaporServisi(servis.getTumKargolar());
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Kargo Sayısı");
            if (periyotCombo.getValue().equals("Günlük (30 gün)")) {
                Map<LocalDate, Long> gunluk = rs.gunlukKargoSayisi(30);
                for (Map.Entry<LocalDate, Long> entry : gunluk.entrySet()) {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
                }
            } else if (periyotCombo.getValue().equals("Haftalık (12 hafta)")) {
                Map<Integer, Long> haftalik = rs.haftalikKargoSayisi(12);
                for (Map.Entry<Integer, Long> entry : haftalik.entrySet()) {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>("Hafta " + entry.getKey(), entry.getValue()));
                }
            } else {
                Map<String, Long> aylik = rs.aylikKargoSayisi(12);
                for (Map.Entry<String, Long> entry : aylik.entrySet()) {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
                }
            }
            barChart.getData().clear();
            barChart.getData().add(series);
        });
        yenileBtn.fire();
        girisBox.getChildren().addAll(periyotCombo, yenileBtn, barChart);
        tabGiris.setContent(girisBox);

        // Tab 2: Ortalama Teslimat Süresi (PieChart)
        Tab tabSure = new Tab("Teslimat Süreleri");
        tabSure.setClosable(false);
        VBox sureBox = new VBox(10);
        sureBox.setPadding(new Insets(10));
        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
        Button sureYenile = new Button("Yenile");
        sureYenile.setOnAction(e -> {
            RaporServisi rs = new RaporServisi(servis.getTumKargolar());
            Map<Kargo.Tip, Double> ort = rs.ortalamaTeslimatSuresi();
            pieChart.getData().clear();
            for (Map.Entry<Kargo.Tip, Double> entry : ort.entrySet()) {
                pieChart.getData().add(new javafx.scene.chart.PieChart.Data(entry.getKey().name(), entry.getValue()));
            }
        });
        sureYenile.fire();
        sureBox.getChildren().addAll(sureYenile, pieChart);
        tabSure.setContent(sureBox);

        // Tab 3: Yoğun Bölgeler
        Tab tabBolge = new Tab("Yoğun Bölgeler");
        tabBolge.setClosable(false);
        VBox bolgeBox = new VBox(10);
        bolgeBox.setPadding(new Insets(10));
        javafx.scene.chart.BarChart<String, Number> bolgeChart = new javafx.scene.chart.BarChart<>(new javafx.scene.chart.CategoryAxis(), new javafx.scene.chart.NumberAxis());
        bolgeChart.setTitle("En Yoğun 5 İlçe");
        Button bolgeYenile = new Button("Yenile");
        bolgeYenile.setOnAction(e -> {
            RaporServisi rs = new RaporServisi(servis.getTumKargolar());
            Map<String, Long> yogun = rs.enYogunBolge(5);
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Kargo Adedi");
            for (Map.Entry<String, Long> entry : yogun.entrySet()) {
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            bolgeChart.getData().clear();
            bolgeChart.getData().add(series);
        });
        bolgeYenile.fire();
        bolgeBox.getChildren().addAll(bolgeYenile, bolgeChart);
        tabBolge.setContent(bolgeBox);

        // Tab 4: Excel Çıktı
        Tab tabExcel = new Tab("Excel Raporu");
        tabExcel.setClosable(false);
        VBox excelBox = new VBox(10);
        excelBox.setPadding(new Insets(10));
        Button excelBtn = new Button("Excel'e Aktar (Tüm Kargolar)");
        excelBtn.setOnAction(e -> exportToExcel());
        excelBox.getChildren().add(excelBtn);
        tabExcel.setContent(excelBox);

        raporTabPane.getTabs().addAll(tabGiris, tabSure, tabBolge, tabExcel);
    }

    private void exportToExcel() {
        try {
            RaporServisi rs = new RaporServisi(servis.getTumKargolar());
            List<List<Object>> data = rs.getRaporTabloVerisi();
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Kargo Raporu");
            int rowNum = 0;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (Object cellData : rowData) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(colNum++);
                    if (cellData instanceof String) cell.setCellValue((String) cellData);
                    else if (cellData instanceof Double) cell.setCellValue((Double) cellData);
                    else if (cellData instanceof Integer) cell.setCellValue((Integer) cellData);
                    else cell.setCellValue(cellData.toString());
                }
            }
            for (int i = 0; i < data.get(0).size(); i++) sheet.autoSizeColumn(i);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Excel Raporu Kaydet");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Dosyası", "*.xlsx"));
            java.io.File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
                new Alert(Alert.AlertType.INFORMATION, "Rapor başarıyla kaydedildi.").show();
            }
            workbook.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Excel oluşturulurken hata: " + ex.getMessage()).show();
        }
    }

    // ==================== SİMÜLASYON (4. Adım) ====================
    private void baslatSimulasyon() {
        if (simulasyonServisi.isCalisiyor()) return;
        simulasyonServisi.baslat();
        btnSimBaslat.setDisable(true);
        btnSimDurdur.setDisable(false);
        startMaliyetGuncelleme();
    }

    private void durdurSimulasyon() {
        simulasyonServisi.durdur();
        btnSimBaslat.setDisable(false);
        btnSimDurdur.setDisable(true);
        if (maliyetTimeline != null) maliyetTimeline.stop();
    }

    private void startMaliyetGuncelleme() {
        if (maliyetTimeline != null) maliyetTimeline.stop();
        maliyetTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (simulasyonServisi.isCalisiyor()) {
                double toplamYakit = simulasyonServisi.getKamyonYakıtTuketimi().values().stream().mapToDouble(Double::doubleValue).sum();
                double toplamMaliyet = simulasyonServisi.getKamyonMaliyet().values().stream().mapToDouble(Double::doubleValue).sum();
                lblYakitMaliyet.setText(String.format("Toplam Yakıt: %.1f L | Maliyet: %.0f TL", toplamYakit, toplamMaliyet));
            }
        }));
        maliyetTimeline.setCycleCount(Timeline.INDEFINITE);
        maliyetTimeline.play();
    }

    //müşteriler bakım fatura düğmelerine basınca
    @FXML private void onMusteriler() {
        gosterAnimasyonlu(musterilerPanel);
        yenileMusteriler();
    }

    private void yenileMusteriler() {
        musteriListBox.getChildren().clear();
        try {
            for (Map<String, Object> m : servis.getDb().musterileriListele()) {
                Label lbl = new Label(m.get("id") + " - " + m.get("ad_soyad") + " (" + m.get("telefon") + ")");
                lbl.setPadding(new Insets(6, 12, 6, 12));
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.getStyleClass().add("kargo-satir");
                musteriListBox.getChildren().add(lbl);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    @FXML private void onBakim() {
        gosterAnimasyonlu(bakimPanel);
        for (Kamyon k : servis.getKamyonlar()) bakimKamyonCombo.getItems().add(k.getId());
    }
    @FXML private void onBakimYenile() {
        String kid = bakimKamyonCombo.getValue();
        if (kid == null) return;
        try {
            bakimListView.getItems().clear();
            for (Map<String, Object> b : servis.getDb().kamyonBakimListele(kid)) {
                bakimListView.getItems().add(b.get("bakim_tarihi") + " - " + b.get("ariza") + " (" + b.get("ucret") + " TL)");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }


    private void yenileBakim() {
        String kid = bakimKamyonCombo.getValue();
        if (kid == null) return;
        bakimListBox.getChildren().clear();
        try {
            for (Map<String, Object> b : servis.getDb().kamyonBakimListele(kid)) {
                Label lbl = new Label(b.get("bakim_tarihi") + " - " + b.get("ariza") + " (" + b.get("ucret") + " TL)");
                lbl.setPadding(new Insets(6, 12, 6, 12));
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.getStyleClass().add("kargo-satir");
                bakimListBox.getChildren().add(lbl);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    @FXML private void onFaturalar() {
        gosterAnimasyonlu(faturalarPanel);
    }

    private void sorgulaPatura() {
        String kid = faturaKargoIdField.getText();
        faturaSonucLabel.setText("Fatura detayları için özel bir sorgu eklenebilir. (Demo) - Kargo ID: " + kid);
    }

    // ==================== YARDIMCI METOTLAR ====================
    private Label tipBadge(Kargo k) {
        Label l = new Label(k.getTip().name()); l.getStyleClass().add("badge");
        l.setStyle("-fx-background-color:" + k.getTipRenk() + "33; -fx-text-fill:" + k.getTipRenk() + ";");
        l.setPrefWidth(90); return l;
    }

    private Label durumBadge(Kargo k) {
        String renk, text;
        switch (k.getDurum()) {
            case DEPODA:        renk = "#f59e0b"; text = "Depoda"; break;
            case YOLDA:         renk = "#8b5cf6"; text = "Yolda";  break;
            case TESLIM_EDILDI: renk = "#10b981"; text = "Teslim Edildi"; break;
            default:            renk = "#64748b"; text = "-";       break;
        }
        Label l = new Label(text); l.getStyleClass().add("badge");
        l.setStyle("-fx-background-color:" + renk + "33; -fx-text-fill:" + renk + ";");
        l.setPrefWidth(110); return l;
    }

    private Label colVal(String text, double w, String color) {
        Label l = new Label(text); l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12.5px;");
        l.setPrefWidth(w); l.setMaxWidth(w); return l;
    }
}