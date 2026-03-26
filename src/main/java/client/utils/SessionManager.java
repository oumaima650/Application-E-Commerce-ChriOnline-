package client.utils;

import shared.Session;
import model.Utilisateur;

/**
 * Singleton class to manage the current user session on the client side.
 */
public class SessionManager {
    private static SessionManager instance;
    private Session currentSession;

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
}
