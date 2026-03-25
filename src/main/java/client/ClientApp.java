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
    private static final int UDP_PORT = 9090;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("ChriOnline - E-Commerce");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        SceneManager.init(primaryStage);
        
        // Démarrer le listener UDP
        udpListener = new ClientUDP(UDP_PORT);
        udpListener.start();
        System.out.println("[ClientApp] Client UDP démarré sur le port " + UDP_PORT);
        
        // Enregistrer le port UDP auprès du serveur (simulation pour admin)
        // Dans un vrai cas, ce serait fait après un login réussi
        registerUdpPort();
        
        // Démarrer sur la page Admin pour visualiser l'interface demandée
        SceneManager.switchTo("admin.fxml", "ChriOnline Admin");
        
        primaryStage.show();
    }
    
    /**
     * Enregistre le port UDP auprès du serveur pour recevoir les notifications
     */
    private void registerUdpPort() {
        try {
            // Simuler l'enregistrement pour l'admin
            // Dans un vrai cas, ce serait fait après authentification
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("udpPort", UDP_PORT);
            
            shared.Requete requete = new shared.Requete(shared.RequestType.REGISTER_UDP_PORT, params, "ADMIN_TOKEN");
            shared.Reponse reponse = ClientSocket.getInstance().envoyer(requete);
            
            if (reponse.isSuccess()) {
                System.out.println("[ClientApp] Port UDP " + UDP_PORT + " enregistré avec succès");
            } else {
                System.err.println("[ClientApp] Erreur enregistrement port UDP: " + reponse.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[ClientApp] Exception lors de l'enregistrement UDP: " + e.getMessage());
        }
    }

    /** Retourne le contrôleur Notifications pour qu'other  pages puissent naviguer vers lui */
    public static NotificationsController getNotificationsController() {
        return notificationsController;
    }
    
    /**
     * Définit le contrôleur Notifications et le connecte au ClientUDP
     */
    public static void setNotificationsController(NotificationsController controller) {
        notificationsController = controller;
        if (udpListener != null) {
            udpListener.setNotificationsController(controller);
            System.out.println("[ClientApp] NotificationsController connecté au ClientUDP");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
