package shared;

import model.Utilisateur;
import java.io.Serializable;

/**
 * Represents an active session between the client and the server.
 * Contains the authentication token and the basic user information.
 */
public class Session implements Serializable {
    private String token;
    private Utilisateur utilisateur;

    public Session() {}

    public Session(String token, Utilisateur utilisateur) {
        this.token = token;
        // Security: Ensure password is not stored in the session object
        if (utilisateur != null) {
            utilisateur.setMotDePasse(null);
        }
        this.utilisateur = utilisateur;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) {
        if (utilisateur != null) {
            utilisateur.setMotDePasse(null);
        }
        this.utilisateur = utilisateur;
    }

    @Override
    public String toString() {
        return "Session{" +
                "token='" + token + '\'' +
                ", user=" + (utilisateur != null ? utilisateur.getEmail() : "null") +
                '}';
    }
}
