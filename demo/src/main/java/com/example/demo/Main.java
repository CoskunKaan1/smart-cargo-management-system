package com.example.demo;

import com.example.demo.service.KargoServisi;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URL;

/**
 * KargoNet — Akıllı Kargo Yönetim Sistemi
 * Bu sınıf uygulamanın giriş noktasıdır ve merkezi KargoServisi'ni başlatır.
 */
public class Main extends Application {

    // Tüm controller sınıflarının erişebileceği merkezi servis nesnesi.
    public static KargoServisi servis;

    // Sistem tepsisi simgesi (2. adım)
    private static TrayIcon trayIcon;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Veri Yapılarını ve İş Mantığını Yöneten Servisi Başlat
            servis = new KargoServisi();

            // 2. Birleştirilmiş FXML Dosyasını Yükle
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/KargoNet.fxml"));
            Parent root = loader.load();

            // 3. Sahneyi Oluştur
            Scene scene = new Scene(root, 1280, 800);

            // CSS dosyasını bağla
            URL cssUrl = getClass().getResource("/com/example/demo/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            // 4. Pencere Ayarları
            primaryStage.setTitle("📦 KargoNet — Akıllı Kargo Yönetim Sistemi");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1150);
            primaryStage.setMinHeight(750);

            // Uygulama kapanırken servis kapatılsın
            primaryStage.setOnCloseRequest(e -> {
                if (servis != null) servis.close();
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();

            // Sistem tepsisi simgesini oluştur (2. adım)
            createTrayIcon(primaryStage);

        } catch (Exception e) {
            System.err.println("Uygulama başlatılırken kritik bir hata oluştu!");
            e.printStackTrace();
        }
    }

    /**
     * Sistem tepsisi (tray) simgesini oluşturur ve bildirimleri gösterir.
     * @param stage Ana pencere (simgeye tıklanınca gösterilecek)
     */
    private void createTrayIcon(Stage stage) {
        if (!SystemTray.isSupported()) {
            System.out.println("Sistem tepsisi desteklenmiyor.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            // 16x16 veya 32x32 boyutunda bir PNG simgesi resources altında olmalı
            Image image = Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/com/example/demo/tray_incon.png"));
            trayIcon = new TrayIcon(image, "KargoNet");
            trayIcon.setImageAutoSize(true);
            // Simgeye tıklayınca uygulamayı öne getir
            trayIcon.addActionListener(e -> {
                Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                });
            });
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sistem tepsisi bildirimi gösterir.
     * @param title Başlık
     * @param message Mesaj içeriği
     * @param type Bildirim türü (INFO, WARNING, ERROR)
     */
    public static void showTrayNotification(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}