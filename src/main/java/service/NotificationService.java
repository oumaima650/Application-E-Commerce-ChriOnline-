package service;
import server.ServeurUDP;
import dao.UtilisateurDAO;

import dao.NotificationDAO;
import model.Notification;
import shared.Reponse;
import shared.Requete;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public void creerNotification(int idUtilisateur, String contenu) {
        Notification notification = new Notification();
        notification.setIdUtilisateur(idUtilisateur);
        notification.setContenu(contenu);
        notification.setStatut(Notification.StatutNotification.NON_LU);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationDAO.create(notification);
        
        // Push UDP si l'utilisateur est en ligne
        ServeurUDP.getInstance().envoyerNotification(idUtilisateur, contenu);
    }

    public void notifierAdmins(String contenu) {
        try {
            List<Integer> adminIds = UtilisateurDAO.getAdminsIds();
            if (!adminIds.isEmpty()) {
                // Notifier uniquement le premier admin (comme demandé: un seul admin central)
                creerNotification(adminIds.get(0), contenu);
            }
        } catch (Exception e) {
            System.err.println("[NotificationService] Erreur lors de la notification de l'admin : " + e.getMessage());
        }
    }

    public Reponse getNotifications(Requete requete) {
        Integer idUtilisateur = (Integer) requete.getParametres().get("idUtilisateur");
        if (idUtilisateur == null) {
            return new Reponse(false, "ID Utilisateur manquant.", null);
        }

        List<Notification> notifications = notificationDAO.findByUtilisateur(idUtilisateur);
        System.out.println("[NotificationService] " + notifications.size() + " notifications trouvées en BDD pour l'utilisateur " + idUtilisateur);
        return new Reponse(true, "Notifications récupérées.", java.util.Map.of("notifications", notifications));
    }
    
    public Reponse markAsRead(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idNotification = (Integer) params.get("idNotification");
        Integer idUtilisateur = (Integer) params.get("idUtilisateur");

        if (idNotification != null) {
            // Marquage individuel
            boolean success = notificationDAO.markAsRead(idNotification);
            return success ? new Reponse(true, "Notification marquée comme lue.", null) 
                           : new Reponse(false, "Échec lors de la mise à jour de la notification.", null);
        } else if (idUtilisateur != null) {
            // Marquage en masse pour tout l'utilisateur
            boolean success = notificationDAO.markAllAsRead(idUtilisateur);
            return success ? new Reponse(true, "Toutes les notifications marquées comme lues.", null) 
                           : new Reponse(false, "Échec lors de la mise à jour des notifications.", null);
        }

        return new Reponse(false, "Paramètres manquants (idNotification ou idUtilisateur).", null);
    }
}
