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
        primaryStage.setTitle("ChriOnline - E-Commerce");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(750);
        
        // INITIALISER LE SCENE MANAGER
        SceneManager.init(primaryStage);

        try {
            // Charger la homepage premium
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-home.fxml"));
            Parent root = loader.load();
            
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
            
            // Scène de secours en cas d'erreur
            javafx.scene.layout.VBox errorBox = new javafx.scene.layout.VBox(20);
            errorBox.setAlignment(javafx.geometry.Pos.CENTER);
            errorBox.setStyle("-fx-padding: 40; -fx-alignment: center;");
            
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label("Erreur de chargement...");
            errorBox.getChildren().add(errorLabel);
            
            javafx.scene.Scene errorScene = new javafx.scene.Scene(errorBox, 600, 400);
            primaryStage.setScene(errorScene);
            primaryStage.show();
        }
        
        // Démarrer le listener UDP pour les notifications
        udpListener = new ClientUDP(UDP_PORT);
        udpListener.start();
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
