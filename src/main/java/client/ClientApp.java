package client;

import client.utils.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import ui.NotificationsController;

public class ClientApp extends Application {

    // Contrôleur de la page Notifications, accessible globalement
    private static NotificationsController notificationsController;
    private static ClientUDP udpListener;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("ChriOnline - E-Commerce");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(750);
        
        // INITIALISER LE SCENE MANAGER
        SceneManager.init(primaryStage);

        try {
            // Charger la homepage premium
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-home.fxml"));
            Parent root = loader.load();
            
            // Pré-charger d'autres scènes pour la fluidité
            SceneManager.cacheScene("main-home.fxml", root);
            SceneManager.loadAsync("product-detail.fxml");
            SceneManager.loadAsync("panier.fxml");

            // Créer la scène avec les styles
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            String cssPath = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            
            primaryStage.setTitle("ChriOnline - Boutique en ligne");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("✅ Homepage ChriOnline chargée avec succès !");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la homepage:");
            e.printStackTrace();
            
            // Afficher une scène de secours avec un message d'erreur
            javafx.scene.layout.VBox errorBox = new javafx.scene.layout.VBox(20);
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            errorBox.setStyle("-fx-padding: 40; -fx-alignment: center;");
            
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                "Erreur de chargement de l'interface\n\n" + e.getMessage() +
                "\n\nVérifiez que:\n" +
                "1. Le fichier /fxml/main-home.fxml existe\n" +
                "2. Le fichier /css/styles.css existe\n" +
                "3. Le controller ui.MainHomeController est correct"
            );
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-wrap-text: true;");
            
            errorBox.getChildren().add(errorLabel);
            
            javafx.scene.Scene errorScene = new javafx.scene.Scene(errorBox, 600, 400);
            primaryStage.setScene(errorScene);
            primaryStage.show();
        }
    }

    /**
     * Retourne le contrôleur Notifications pour qu'other pages puissent naviguer
     * vers lui
     */
    public static NotificationsController getNotificationsController() {
        return notificationsController;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
