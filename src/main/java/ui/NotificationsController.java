package ui;

import client.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationsController {

    @FXML private VBox notificationsBox;

    @FXML
    public void initialize() {
        // Connecter ce contrôleur au ClientUDP global
        client.ClientApp.setNotificationsController(this);
        
        // Ajouter quelques fausses notifications udp pour illustrer l'UI 
        addNotification("Système", "Service de notifications UDP actif sur le port 9090");
        addNotification("Nouvelle connexion TCP", "Un nouvel appareil vient de se connecter à votre compte.");
        addNotification("Commande expédiée", "Votre commande #CMD-002 vient de partir de notre entrepôt !");
        addNotification("Paiement refusé", "Attention, le paiement de 120 MAD a échoué (Fonds insuffisants).");
    }

    /**
     * Méthode à appeler depuis le vrai `ClientUDP.java` au lieu d'une Alert Box.
     */
    public void addNotification(String titre, String messageUdp) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        
        HBox header = new HBox(10);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF724C; -fx-font-size: 16px;");
        
        Label lblTime = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        
        header.getChildren().addAll(lblTitre, new javafx.scene.layout.Region(), lblTime);
        HBox.setHgrow(header.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        Text txtMessage = new Text(messageUdp);
        txtMessage.setStyle("-fx-fill: #2A2C41;");
        txtMessage.setWrappingWidth(500);

        card.getChildren().addAll(header, txtMessage);
        
        // Ajouter au début de la liste (les plus récents en haut)
        notificationsBox.getChildren().add(0, card);
    }

    @FXML
    private void clearNotifications() {
        notificationsBox.getChildren().clear();
    }

    @FXML
    private void retourArriere() {
        SceneManager.back();
    }
}
