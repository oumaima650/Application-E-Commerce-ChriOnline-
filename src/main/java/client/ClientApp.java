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

        // 1. Précharger le FXML Notifications pour récupérer son Controller
        FXMLLoader notifLoader = new FXMLLoader(
            ClientApp.class.getResource("/com/chrionline/fxml/notifications.fxml")
        );
        Parent notifRoot = notifLoader.load();
        notificationsController = notifLoader.getController();
        SceneManager.cacheScene("notifications.fxml", notifRoot);

        // 2. Lancer l'écouteur UDP et le brancher sur la page Notifications
        udpListener = new ClientUDP(9090);
        udpListener.setNotificationsController(notificationsController);
        udpListener.start();

        // 3. Démarrer sur la page Admin (changer par "checkout.fxml" ou autre au besoin)
        SceneManager.switchTo("admin.fxml", "ChriOnline - Administration");

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
