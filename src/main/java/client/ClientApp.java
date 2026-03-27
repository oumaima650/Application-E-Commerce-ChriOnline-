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
    public static final int UDP_PORT = 9090;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("[ClientApp] Starting ChriOnline Client Application...");

        // INITIALISER LE SCENE MANAGER
        SceneManager.init(primaryStage);

        // Démarrer le listener UDP
        udpListener = new ClientUDP(UDP_PORT);
        udpListener.start();
        
        // DEMARRER SUR LA PAGE D'ACCUEIL (Guest Mode)
        // SceneManager s'occupe de charger le FXML et d'appliquer le CSS
        SceneManager.switchTo("main-home.fxml", "ChriOnline - Accueil");
        
        primaryStage.show();
    }
    
    /**
     * Enregistre le port UDP auprès du serveur pour recevoir les notifications
     */
    public void registerUdpPort(String token) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("udpPort", UDP_PORT);
            
            shared.Requete requete = new shared.Requete(shared.RequestType.REGISTER_UDP_PORT, params, token);
            shared.Reponse reponse = ClientSocket.getInstance().envoyer(requete);
            
            if (reponse != null && reponse.isSucces()) {
                System.out.println("[ClientApp] Port UDP " + UDP_PORT + " enregistré avec succès");
            } else {
                System.err.println("[ClientApp] Erreur enregistrement port UDP: " + (reponse != null ? reponse.getMessage() : "Pas de réponse"));
            }
        } catch (Exception e) {
            System.err.println("[ClientApp] Exception lors de l'enregistrement UDP: " + e.getMessage());
        }
    }

    private static ClientApp instance;
    public static ClientApp getInstance() { return instance; }

    public ClientApp() { instance = this; }

    /**
     * Retourne le contrôleur Notifications pour qu'other pages puissent naviguer
     * vers lui
     */
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
