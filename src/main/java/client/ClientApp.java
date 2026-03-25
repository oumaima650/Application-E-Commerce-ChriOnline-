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
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        SceneManager.init(primaryStage);
        
        // Démarrer sur la page Admin pour visualiser l'interface demandée
        SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier");
        
        primaryStage.show();
    }

    /** Retourne le contrôleur Notifications pour qu'other  pages puissent naviguer vers lui */
    public static NotificationsController getNotificationsController() {
        return notificationsController;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
