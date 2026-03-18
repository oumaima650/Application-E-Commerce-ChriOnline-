package client.utils;

import client.ClientApp;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        SceneManager.primaryStage = stage;
    }

    public static void switchTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/com/chrionline/fxml/" + fxmlFile));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            
            // Appliquer le CSS global
            String cssPath = ClientApp.class.getResource("/com/chrionline/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

            // Animation de fondu
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
