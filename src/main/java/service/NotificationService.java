package service;

import dao.NotificationDAO;
import model.Notification;
import shared.Reponse;
import shared.Requete;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public void creerNotification(int idUtilisateur, String contenu) {
        Notification notification = new Notification();
        notification.setIdUtilisateur(idUtilisateur);
        notification.setContenu(contenu);
        notification.setStatut(Notification.StatutNotification.non_lu);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationDAO.create(notification);
        // Optional: Call NotificationManager UDP send here if implemented.
    }

    public Reponse getNotifications(Requete requete) {
        Integer idUtilisateur = (Integer) requete.getParametres().get("idUtilisateur");
        if (idUtilisateur == null) {
            return new Reponse(false, "ID Utilisateur manquant.", null);
        }

        List<Notification> notifications = notificationDAO.findByUtilisateur(idUtilisateur);
        return new Reponse(true, "Notifications récupérées.", java.util.Map.of("notifications", notifications));
    }
    
    public Reponse markAsRead(Requete requete) {
        Integer idNotification = (Integer) requete.getParametres().get("idNotification");
        if (idNotification == null) {
            return new Reponse(false, "ID Notification manquant.", null);
        }

        boolean success = notificationDAO.updateStatut(idNotification, Notification.StatutNotification.lu);
        if (success) {
            return new Reponse(true, "Notification marquée comme lue.", null);
        } else {
            return new Reponse(false, "Échec lors de la mise à jour de la notification.", null);
        }
    }
}
