package client.utils;

/**
 * Singleton gérant la session utilisateur côté client.
 * Stocke l'ID de l'utilisateur, son token de session servant à l'authentification
 * auprès du serveur, ainsi que ses informations de profil de base.
 */
public class SessionManager {

    private static SessionManager instance;

    private String token;
    private int userId;
    private String email;
    private String userType;
    private String nom;
    private String prenom;
    private String telephone;

    private SessionManager() {
        // Constructeur privé pour le singleton
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Ouvre une session avec les informations fournies par le serveur.
     */
    public void ouvrir(String token, int userId, String email, String userType) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.userType = userType;
        System.out.println("[SessionManager] Session ouverte pour ID: " + userId + " (" + userType + ")");
    }

    /** Complète le profil après la connexion. */
    public void setProfile(String nom, String prenom, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }

    /**
     * Ferme la session en cours.
     */
    public void fermer() {
        this.token = null;
        this.userId = -1;
        this.email = null;
        this.userType = null;
        System.out.println("[SessionManager] Session fermée.");
    }

    public boolean estConnecte() {
        return token != null && userId > 0;
    }

    // Getters
    public String getToken() { return token; }
    public int getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getTelephone() { return telephone; }
}
