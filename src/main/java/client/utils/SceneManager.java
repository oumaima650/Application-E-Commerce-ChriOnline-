package client.utils;

import client.ClientApp;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    private static Stage primaryStage;
    private static final Map<String, Parent> sceneCache = new HashMap<>();
    // Historique de navigation pour revenir en arrière
    private static final java.util.Deque<javafx.scene.Scene> history = new java.util.ArrayDeque<>();

    public static void init(Stage stage) {
        SceneManager.primaryStage = stage;
    }

    /** Permet de mettre en cache un Parent déjà chargé par l'appelant */
    public static void cacheScene(String key, Parent root) {
        sceneCache.put(key, root);
    }

    /** Vérifie si une scène est en cache */
    public static boolean isCached(String key) {
        return sceneCache.containsKey(key);
    }

    /** Supprime une scène du cache pour forcer un rechargement */
    public static void clearCache(String key) {
        sceneCache.remove(key);
    }

    /** Vide l'historique de navigation */
    public static void clearHistory() {
        history.clear();
    }

    /** Bascule vers une scène en cache (sans recharger le FXML) */
    public static void switchToCached(String key, String title) {
        Parent root = sceneCache.get(key);
        if (root == null) {
            System.err.println("Aucune scène en cache pour la clé : " + key);
            return;
        }

        // Sauvegarder la scène actuelle pour pouvoir revenir
        if (primaryStage.getScene() != null) {
            history.push(primaryStage.getScene());
        }

        Scene scene = root.getScene();
        if (scene == null) {
            scene = new Scene(root);
            String cssPath = ClientApp.class.getResource("/com/chrionline/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /** Revenir à la scène précédente */
    public static void back() {
        if (!history.isEmpty()) {
            Scene previous = history.pop();
            primaryStage.setTitle("ChriOnline");
            primaryStage.setScene(previous);
            FadeTransition ft = new FadeTransition(Duration.millis(250), previous.getRoot());
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public static void switchTo(String fxmlFile, String title) {
        // Check if cached first
        if (sceneCache.containsKey(fxmlFile)) {
            switchToCached(fxmlFile, title);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            sceneCache.put(fxmlFile, root); // Cache for next time

            if (primaryStage.getScene() != null) {
                history.push(primaryStage.getScene());
            }

            Scene scene = new Scene(root);
            String cssPath = ClientApp.class.getResource("/com/chrionline/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

        } catch (IOException e) {
            System.err.println("Erreur de chargement de la vue: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /** Charge un FXML en arrière-plan et le met en cache */
    public static void loadAsync(String fxmlFile) {
        if (sceneCache.containsKey(fxmlFile))
            return;

        Thread thread = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/fxml/" + fxmlFile));
                Parent root = loader.load();

                // PRÉ-CRÉER LA SCÈNE AVEC LES CSS POUR ÉVITER LE RECHARGEMENT
                javafx.application.Platform.runLater(() -> {
                    Scene scene = new Scene(root);
                    String cssPath = ClientApp.class.getResource("/com/chrionline/css/styles.css").toExternalForm();
                    scene.getStylesheets().add(cssPath);

                    sceneCache.put(fxmlFile, root);
                    System.out.println("Scène préchargée : " + fxmlFile);
                });
            } catch (IOException e) {
                System.err.println("Échec du pré-chargement : " + fxmlFile);
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
