package service;

import dao.ClientDAO;
import dao.UtilisateurDAO;
import service.NotificationService;
import model.Client;
import model.Utilisateur;
import model.enums.TypeEtulisateur;
import shared.Reponse;
import shared.Requete;
import server.utils.SessionManager;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class AuthService {

    public Reponse login(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")) {
                return new Reponse(false, "Champs manquants : email et motDePasse requis.", null);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");

            UtilisateurDAO.LoginData loginData = UtilisateurDAO.getLoginData(email);

            if (loginData == null) {
                return new Reponse(false, "Aucun compte trouvé avec cet email.", null);
            }

            if (!PasswordService.verify(motDePasse, loginData.hash())) {
                return new Reponse(false, "Mot de passe incorrect.", null);
            }

            int userId = loginData.id();
            Utilisateur user = UtilisateurDAO.findById(userId);

            // Bloquer si le client est banni
            if (user instanceof Client && "BANNI".equals(((Client) user).getStatut())) {
                return new Reponse(false, "Ce compte a été banni par l'administrateur.", null);
            }

            String token = SessionManager.getInstance().creerSession(user);

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("token" ,token);
            donnees.put("utilisateur", user); // This will include all info (nom, prenom for Client)
            donnees.put("typeUtilisateur", (user instanceof Client) ? "CLIENT" : "ADMIN");

            System.out.println("[AuthService] Login OK — email=" + email + " token=" + token);
            return new Reponse(true, "Connexion réussie.", donnees);

        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du login : " + e.getMessage());
            return new Reponse(false, "Erreur serveur lors de la connexion.", null);
        }
    }

    public Reponse signup(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null
                    || !params.containsKey("email")
                    || !params.containsKey("motDePasse")
                    || !params.containsKey("nom")
                    || !params.containsKey("prenom")
                    || !params.containsKey("telephone")) {
                return new Reponse(false, "Champs manquants : email, motDePasse, nom, prenom, telephone requis.", null);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");
            String nom        = (String) params.get("nom");
            String prenom     = (String) params.get("prenom");
            String telephone  = (String) params.get("telephone");

            // Password strength validation
            PasswordService.ValidationResult strength = PasswordService.validateStrength(motDePasse);
            if (!strength.isValid()) {
                return new Reponse(false, strength.message(), null);
            }

            if (UtilisateurDAO.userExist(email)) {
                return new Reponse(false, "Email déjà utilisé : " + email, null);
            }

            if (ClientDAO.isTelephoneExist(telephone)) {
                return new Reponse(false, "Numéro de téléphone déjà utilisé : " + telephone, null);
            }

            // Hash password before saving
            String hashedPw = PasswordService.hash(motDePasse);
            ClientDAO clientDAO = new ClientDAO();
            Client client = clientDAO.create(email, hashedPw, nom, prenom, telephone);

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("utilisateur", client);

            System.out.println("[AuthService] Signup OK — userId=" + client.getIdUtilisateur());
            
            // Notification aux admins
            new NotificationService().notifierAdmins("Nouveau client inscrit : " + prenom + " " + nom + " (" + email + ")");
            
            return new Reponse(true, "Compte créé avec succès.", donnees);

        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du signup : " + e.getMessage());
            return new Reponse(false, "Erreur serveur lors de l'inscription.", null);
        }
    }

    public Reponse logout(Requete requete) {
        String token = requete.getTokenSession();

        if (token == null || token.isEmpty()) {
            return new Reponse(false, "Aucun token de session fourni.", null);
        }

        SessionManager.getInstance().fermerSession(token);
        System.out.println("[AuthService] Logout OK — token=" + token);
        return new Reponse(true, "Déconnexion réussie.", null);
    }

    public static int getUserIdFromToken(String token) {
        if (!SessionManager.getInstance().validerToken(token)) {
            return -1;
        }
        Utilisateur user = SessionManager.getInstance().getUtilisateur(token);
        return (user != null) ? user.getIdUtilisateur() : -1;
    }

    public static boolean isAuthenticated(String token) {
        return SessionManager.getInstance().validerToken(token);
    }
}
