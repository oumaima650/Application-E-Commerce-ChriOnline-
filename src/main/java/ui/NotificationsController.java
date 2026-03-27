package ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import model.Notification;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import client.ClientSocket;
import client.utils.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsController {

    @FXML private VBox notificationsBox;
    @FXML private javafx.scene.layout.StackPane notifBadge;
    @FXML private Label notifCount;

    @FXML
    public void initialize() {
        // Connecter ce contrôleur au ClientUDP global
        client.ClientApp.setNotificationsController(this);
        
        // Nettoyer la liste avant de charger
        notificationsBox.getChildren().clear();
        
        // Charger les notifications depuis la base de données
        loadNotifications();
    }

    private void loadNotifications() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            System.err.println("[NotificationsController] Erreur: Utilisateur non authentifié dans SessionManager.");
            return;
        }
        
        int currentUserId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        String token = SessionManager.getInstance().getSession().getToken();
        
        System.out.println("[NotificationsController] Chargement des notifications pour l'utilisateur ID: " + currentUserId);
        
        Task<Reponse> task = new Task<>() {
            @Override
            protected Reponse call() {
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("idUtilisateur", currentUserId);
                    
                    Requete req = new Requete(RequestType.GET_NOTIFICATIONS, params, token);
                    return ClientSocket.getInstance().envoyer(req);
                } catch (Exception e) {
                    System.err.println("[NotificationsController] Erreur de communication TCP: " + e.getMessage());
                    return null;
                }
            }
        };

        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                @SuppressWarnings("unchecked")
                List<Notification> notifsData = (List<Notification>) rep.getDonnees().get("notifications");
                
                System.out.println("[NotificationsController] " + (notifsData != null ? notifsData.size() : 0) + " notifications reçues du serveur.");
                
                Platform.runLater(() -> {
                    if (notifsData != null) {
                        int unreadCount = 0;
                        for (Notification n : notifsData) {
                            boolean isUnread = n.getStatut() == Notification.StatutNotification.NON_LU;
                            if (isUnread) unreadCount++;
                            
                            addNotification(
                                n.getIdNotification(),
                                isUnread ? "Nouveau" : "Notification", 
                                n.getContenu(), 
                                n.getCreatedAt(),
                                isUnread
                            );
                        }
                        
                        // Mettre à jour le badge (uniquement les non lues)
                        if (notifCount != null) notifCount.setText(String.valueOf(unreadCount));
                        if (notifBadge != null) notifBadge.setVisible(unreadCount > 0);
                    }
                    
                    // Si aucune notification
                    if (notificationsBox.getChildren().isEmpty()) {
                        System.out.println("[NotificationsController] Aucune notification trouvée pour cet utilisateur.");
                        Label empty = new Label("Aucune notification pour le moment.");
                        empty.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                        notificationsBox.getChildren().add(empty);
                        if (notifBadge != null) notifBadge.setVisible(false);
                    }
                });
            } else {
                System.err.println("[NotificationsController] Échec du serveur: " + (rep != null ? rep.getMessage() : "Pas de réponse"));
            }
        });
        
        new Thread(task).start();
    }

    /**
     * Méthode à appeler depuis le vrai `ClientUDP.java` au lieu d'une Alert Box.
     */
    public void addNotification(String titre, String messageUdp) {
        // Fallback pour les notifications UDP directes (on met un ID factice ou on gère autrement)
        addNotification(-1, titre, messageUdp, LocalDateTime.now(), true);
    }

    public void addNotification(int idNotification, String titre, String messageUdp, LocalDateTime time, boolean isUnread) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        
        // Curseur main pour indiquer que c'est cliquable si non lu
        if (isUnread) card.setCursor(javafx.scene.Cursor.HAND);
        
        // Style différent si non lu
        if (isUnread) {
            card.setStyle("-fx-background-color: #F0F7FF; -fx-border-color: #D0E3FF; -fx-border-radius: 15px; -fx-background-radius: 15px; -fx-padding: 15;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-border-color: #EEEEEE; -fx-border-radius: 15px; -fx-background-radius: 15px; -fx-padding: 15;");
        }
        
        // Evenement au clic
        card.setOnMouseClicked(event -> {
            if (isUnread && idNotification > 0) {
                markSingleAsRead(idNotification, card);
            }
        });
        
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        if (isUnread) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web("#FF724C"));
            header.getChildren().add(dot);
        }

        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isUnread ? "#24316B" : "#FF724C") + "; -fx-font-size: 16px;");
        
        Label lblTime = new Label(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        
        header.getChildren().addAll(lblTitre, new javafx.scene.layout.Region(), lblTime);
        HBox.setHgrow(header.getChildren().get(header.getChildren().size()-2), javafx.scene.layout.Priority.ALWAYS);

        Text txtMessage = new Text(messageUdp);
        txtMessage.setStyle("-fx-fill: #2A2C41;");
        txtMessage.setWrappingWidth(500);

        card.getChildren().addAll(header, txtMessage);
        
        // Ajouter au début de la liste (les plus récents en haut)
        if (notificationsBox != null) {
            notificationsBox.getChildren().add(0, card);
        }
    }

    @FXML
    private void clearNotifications() {
        if (!SessionManager.getInstance().isAuthenticated()) return;
        
        int userId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        String token = SessionManager.getInstance().getSession().getToken();

        Task<Reponse> task = new Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new HashMap<>();
                params.put("idUtilisateur", userId);
                // On utilise un nouveau type ou on surcharge MARK_NOTIFICATION_READ
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.MARK_NOTIFICATION_READ, params, token));
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(this::loadNotifications); // Recharger la liste
        });
        new Thread(task).start();
    }

    private void markSingleAsRead(int idNotif, VBox card) {
        String token = SessionManager.getInstance().getSession().getToken();
        Task<Reponse> task = new Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new HashMap<>();
                params.put("idNotification", idNotif);
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.MARK_NOTIFICATION_READ, params, token));
            }
        };
        task.setOnSucceeded(e -> {
            Platform.runLater(this::loadNotifications); // Recharger pour mettre à jour les styles et le badge
        });
        new Thread(task).start();
    }

    @FXML
    private void retourArriere() {
        client.utils.SceneManager.back();
    }
}
