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

    /** Permet de mettre en cache un Parent d\u00e9j\u00e0 charg\u00e9 par l'appelant */
    public static void cacheScene(String key, Parent root) {
        sceneCache.put(key, root);
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
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/com/chrionline/fxml/" + fxmlFile));
            Parent root = loader.load();

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
}
