package service;

import dao.ClientDAO;
import dao.UtilisateurDAO;
import model.Client;
import model.enums.TypeEtulisateur;
import shared.Reponse;
import shared.Requete;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class AuthService {

    private static final ConcurrentHashMap<String, Integer> sessions = new ConcurrentHashMap<>();

    public Reponse login(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")) {
                return new Reponse(false, "Champs manquants : email et motDePasse requis.", null);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");

            int userId = UtilisateurDAO.verifyLogInInformations(email, motDePasse);

            if (userId == -1) {
                return new Reponse(false, "Aucun compte trouvé avec cet email.", null);
            }else if (userId == 0) {
                return new Reponse(false, "Mot de passe incorrect.", null);
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, userId);

            TypeEtulisateur type = UtilisateurDAO.userType(userId);

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("token" ,token);
            donnees.put("userId",userId);
            donnees.put("typeUtilisateur", type.name());

            System.out.println("[AuthService] Login OK — userId=" + userId + " type=" + type + " token=" + token);
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

            if (UtilisateurDAO.userExist(email)) {
                return new Reponse(false, "Email déjà utilisé : " + email, null);
            }

            ClientDAO clientDAO = new ClientDAO();
            Client client = clientDAO.create(email, motDePasse, nom, prenom, telephone);

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("userId",    client.getIdUtilisateur());
            donnees.put("email",     client.getEmail());
            donnees.put("nom",       client.getNom());
            donnees.put("prenom",    client.getPrenom());
            donnees.put("createdAt", client.getCreatedAt().toString());

            System.out.println("[AuthService] Signup OK — userId=" + client.getIdUtilisateur());
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

        if (sessions.remove(token) != null) {
            System.out.println("[AuthService] Logout OK — token=" + token);
            return new Reponse(true, "Déconnexion réussie.", null);
        } else {
            return new Reponse(false, "Token invalide ou déjà expiré.", null);
        }
    }


    public static int getUserIdFromToken(String token) {
        if (token == null) return -1;
        return sessions.getOrDefault(token, -1);
    }

    public static boolean isAuthenticated(String token) {
        return getUserIdFromToken(token) > 0;
    }
}
