package client.utils;

import client.ClientApp;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gestionnaire de navigation entre les scènes FXML.
 * Cette version a été simplifiée pour garantir une stabilité maximale en rechargeant
 * chaque vue à chaque navigation, évitant ainsi les erreurs de type "root is already set".
 */
public class SceneManager {

    private static Stage primaryStage;

    // Historique de navigation (FXML path and Title)
    private static final Deque<NavigationRecord> history = new ArrayDeque<>();

    private static String currentFxml;
    private static String currentTitle;

    private static class NavigationRecord {
        String fxmlFile;
        String title;
        NavigationRecord(String fxml, String t) { this.fxmlFile = fxml; this.title = t; }
    }

    public static void init(Stage stage) {
        SceneManager.primaryStage = stage;
    }

    public static void clearHistory() {
        history.clear();
    }

    public static void back() {
        Platform.runLater(() -> {
            if (!history.isEmpty()) {
                NavigationRecord previous = history.pop();
                switchToInternal(previous.fxmlFile, previous.title, false);
            }
        });
    }

    public static void switchTo(String fxmlFile, String title) {
        switchToInternal(fxmlFile, title, true);
    }

    private static void switchToInternal(String fxmlFile, String title, boolean pushToHistory) {
        Platform.runLater(() -> {
            try {
                if (pushToHistory && currentFxml != null) {
                    history.push(new NavigationRecord(currentFxml, currentTitle));
                }

                currentFxml = fxmlFile;
                currentTitle = title;

                FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/fxml/" + fxmlFile));
                Parent root = loader.load();

                Scene scene = new Scene(root);
                String cssPath = ClientApp.class.getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(cssPath);

                primaryStage.setTitle(title);
                primaryStage.setScene(scene);

                FadeTransition ft = new FadeTransition(Duration.millis(300), root);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.play();

            } catch (IOException e) {
                System.err.println("Erreur de chargement: " + fxmlFile);
                e.printStackTrace();
            }
        });
    }

    public static void pushHistory(String fxml, String title) {
        history.push(new NavigationRecord(fxml, title));
    }
}
