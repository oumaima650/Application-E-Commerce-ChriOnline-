package client;

import client.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ChriOnline - E-Commerce");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        SceneManager.init(primaryStage);
        
        // Démarrer sur la page Admin pour visualiser l'interface demandée
        SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier");
        
        primaryStage.show();
        
        // Démarrer l'écouteur UDP en arrière-plan
        ClientUDP udpListener = new ClientUDP(9090);
        udpListener.setDaemon(true);
        udpListener.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
