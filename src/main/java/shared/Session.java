package shared;

import model.Utilisateur;
import java.io.Serializable;

/**
 * Represents an active session between the client and the server.
 * Contains the authentication token and the basic user information.
 */
public class Session implements Serializable {
    private String accessToken;
    private String refreshToken;
    private Utilisateur utilisateur;

    public Session() {}

    public Session(String accessToken, String refreshToken, Utilisateur utilisateur) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        // Security: Ensure password is not stored in the session object
        if (utilisateur != null) {
            utilisateur.setMotDePasse(null);
        }
        this.utilisateur = utilisateur;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

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
                "accessToken='" + (accessToken != null ? "exists" : "null") + '\'' +
                ", refreshToken='" + (refreshToken != null ? "exists" : "null") + '\'' +
                ", user=" + (utilisateur != null ? utilisateur.getEmail() : "null") +
                '}';
    }
}
