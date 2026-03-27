package server.utils;

import model.Utilisateur;
import shared.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton class to manage active user sessions on the server.
 */
public class SessionManager {
    private static SessionManager instance;
    private final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expirationTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Session duration: 30 minutes (in milliseconds)
    private static final long SESSION_DURATION_MS = 30 * 60 * 1000L;

    private SessionManager() {
        // Background task to clean up expired sessions every 10 minutes
        scheduler.scheduleAtFixedRate(this::cleanExpiredSessions, 10, 10, TimeUnit.MINUTES);
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = expirationTimes.entrySet().iterator();
        int count = 0;
        
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now > entry.getValue()) {
                String token = entry.getKey();
                it.remove(); // Remove from expirationTimes
                activeSessions.remove(token); // Remove from activeSessions
                count++;
            }
        }
        if (count > 0) {
            System.out.println("[Server Session] Nettoyage automatique : " + count + " sessions expirées supprimées.");
        }
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Creates a new session and returns the authentication token.
     */
    public String creerSession(Utilisateur user) {
        if (user == null) return null;
        
        String token = UUID.randomUUID().toString();
        // Create a new session object (using null for refreshToken as this is legacy)
        Session session = new Session(token, null, user);
        
        activeSessions.put(token, session);
        expirationTimes.put(token, System.currentTimeMillis() + SESSION_DURATION_MS);
        
        System.out.println("[Server Session] Session créée pour : " + user.getEmail() + " (expire dans 30 min)");
        return token;
    }

    /**
     * Validates a session token and checks for expiration.
     */
    public boolean validerToken(String token) {
        if (token == null || !activeSessions.containsKey(token)) {
            return false;
        }

        long expiry = expirationTimes.getOrDefault(token, 0L);
        if (System.currentTimeMillis() > expiry) {
            System.out.println("[Server Session] Session expirée pour le token : " + token);
            fermerSession(token);
            return false;
        }

        // Optional: Extend session on activity (sliding window)
        expirationTimes.put(token, System.currentTimeMillis() + SESSION_DURATION_MS);
        return true;
    }

    /**
     * Retrieves the user associated with a token.
     */
    public Utilisateur getUtilisateur(String token) {
        if (!validerToken(token)) return null;
        Session session = activeSessions.get(token);
        return (session != null) ? session.getUtilisateur() : null;
    }

    /**
     * Removes a session (logout).
     */
    public void fermerSession(String token) {
        if (token != null) {
            expirationTimes.remove(token);
            Session s = activeSessions.remove(token);
            if (s != null) {
                System.out.println("[Server Session] Session fermée pour : " + s.getUtilisateur().getEmail());
            }
        }
    }
}
