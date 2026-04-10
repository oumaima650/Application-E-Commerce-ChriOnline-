package client.utils;

import shared.Session;
import model.Utilisateur;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Singleton class to manage the current user session on the client side.
 */
public class SessionManager {
    private static SessionManager instance;
    private Session currentSession;
    //c pour cree une sorte de memeoire pour se rappeler de la page ou on etait avant de se connecter 
    private String pendingRedirect;
    private String pendingRedirectTitle;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Initializes a new session after successful login.
     */
    public void ouvrir(String accessToken, String refreshToken, Utilisateur user) {
        this.currentSession = new Session(accessToken, refreshToken, user);
        System.out.println("[Client Session] Session ouverte pour : " + user.getEmail());
    }

    /**
     * Clears the current session (logout).
     */
    public void fermer() {
        this.currentSession = null;
        System.out.println("[Client Session] Session fermée.");
    }

    public Session getSession() {
        return currentSession;
    }

    public boolean isAuthenticated() {
        return currentSession != null && currentSession.getAccessToken() != null;
    }

    public Utilisateur getCurrentUser() {
        return (currentSession != null) ? currentSession.getUtilisateur() : null;
    }

    public void setPendingRedirect(String fxml, String title) {
        this.pendingRedirect = fxml;
        this.pendingRedirectTitle = title;
    }

    public String getPendingRedirect() {
        return pendingRedirect;
    }

    public String getPendingRedirectTitle() {
        return pendingRedirectTitle;
    }

    public void clearPendingRedirect() {
        this.pendingRedirect = null;
        this.pendingRedirectTitle = null;
    }

    /**
     * [WHITELIST IP ADMIN] Gestionnaire global pour les accès admin refusés par la whitelist IP.
     * 
     * À appeler depuis n'importe quel contrôleur JavaFX dès qu'une réponse "IP_NOT_AUTHORIZED" est reçue.
     * Cette méthode :
     *   1. Affiche une alerte d'erreur avec message explicatif.
     *   2. Efface la session locale (JWT, Refresh Token, Utilisateur).
     *   3. Redirige vers la page de connexion.
     */
    public static void handleIpNotAuthorized() {
        Platform.runLater(() -> {
            // Étape 1 : Afficher l'alerte d'accès refusé
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Accès Refusé");
            alert.setHeaderText("Adresse IP non autorisée");
            alert.setContentText(
                "Votre adresse IP ne fait pas partie des adresses de confiance " +
                "pour les fonctions administratives.\n" +
                "Veuillez contacter votre administrateur système."
            );
            alert.showAndWait();

            // Étape 2 : Effacer la session locale (JWT + Refresh Token + Utilisateur)
            SessionManager.getInstance().fermer();

            // Étape 3 : Rediriger vers la page de connexion
            SceneManager.switchTo("Login.fxml", "Connexion - ChriOnline");
        });
    }
}
