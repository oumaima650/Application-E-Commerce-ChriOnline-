package client.utils;

import shared.Session;
import model.Utilisateur;

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
    public void ouvrir(String token, Utilisateur user) {
        this.currentSession = new Session(token, user);
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
        return currentSession != null && currentSession.getToken() != null;
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
}
