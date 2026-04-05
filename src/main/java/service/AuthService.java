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

import dao.RefreshTokenDAO;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class AuthService {

    public Reponse login(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")) {
                return new Reponse(false, "Champs manquants : email et motDePasse requis.", null);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");
            String recaptchaToken = (String) params.get("recaptchaToken");

            // --- reCAPTCHA Verification ---
            RecaptchaService recaptchaService = new RecaptchaService();
            if (!recaptchaService.verify(recaptchaToken)) {
                return new Reponse(false, "Vérification reCAPTCHA échouée. Veuillez réessayer.", null);
            }

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

            String role = (user instanceof Client) ? "CLIENT" : "ADMIN";
            String sessionId = UUID.randomUUID().toString();

            // Generate JWT Access Token (15m)
            String accessToken = JWTService.generateAccessToken(String.valueOf(userId), role, sessionId);

            // Generate Refresh Token (7 days)
            String refreshToken = JWTService.generateRefreshToken();
            RefreshTokenDAO refreshTokenDAO = new RefreshTokenDAO();
            refreshTokenDAO.save(userId, refreshToken, LocalDateTime.now().plusDays(7));

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("accessToken", accessToken);
            donnees.put("refreshToken", refreshToken);
            donnees.put("utilisateur", user);
            donnees.put("typeUtilisateur", role);

            System.out.println("[AuthService] Login OK — email=" + email + " userId=" + userId);
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
            String recaptchaToken = (String) params.get("recaptchaToken");

            // --- reCAPTCHA Verification ---
            RecaptchaService recaptchaService = new RecaptchaService();
            if (!recaptchaService.verify(recaptchaToken)) {
                return new Reponse(false, "Vérification reCAPTCHA échouée. Veuillez réessayer.", null);
            }

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

    public Reponse refresh(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("refreshToken")) {
                return new Reponse(false, "Refresh token manquant.", null);
            }

            String rawRefreshToken = (String) params.get("refreshToken");
            RefreshTokenDAO dao = new RefreshTokenDAO();
            RefreshTokenDAO.RefreshTokenInfo info = dao.findByToken(rawRefreshToken);

            if (info == null) {
                return new Reponse(false, "INVALID_TOKEN", null);
            }

            if (info.isRevoked()) {
                return new Reponse(false, "TOKEN_REVOKED", null);
            }

            if (info.isUsed()) {
                // ALARM: REUSE DETECTED
                dao.revokeAllForUser(info.userId());
                return new Reponse(false, "REFRESH_TOKEN_REUSE_DETECTED", null);
            }

            if (info.expiresAt().isBefore(LocalDateTime.now())) {
                return new Reponse(false, "TOKEN_EXPIRED", null);
            }

            // ROTATION
            dao.markAsUsed(info.id());

            Utilisateur user = UtilisateurDAO.findById(info.userId());
            String role = (user instanceof Client) ? "CLIENT" : "ADMIN";
            String newSessionId = UUID.randomUUID().toString();

            String newAccessToken = JWTService.generateAccessToken(String.valueOf(user.getIdUtilisateur()), role, newSessionId);
            String newRefreshToken = JWTService.generateRefreshToken();
            dao.save(user.getIdUtilisateur(), newRefreshToken, LocalDateTime.now().plusDays(7));

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("accessToken", newAccessToken);
            donnees.put("refreshToken", newRefreshToken);

            System.out.println("[AuthService] Token rotated for userId=" + info.userId());
            return new Reponse(true, "Token rafraîchi.", donnees);

        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du refresh : " + e.getMessage());
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse logout(Requete requete) {
        // userId and sessionId were injected into params by ClientHandler after JWT validation
        Map<String, Object> params = requete.getParametres();
        if (params != null && params.containsKey("refreshToken")) {
            String rawToken = (String) params.get("refreshToken");
            try {
                RefreshTokenDAO dao = new RefreshTokenDAO();
                RefreshTokenDAO.RefreshTokenInfo info = dao.findByToken(rawToken);
                if (info != null && info.userId() == (int) params.get("userId")) {
                    // In a full implementation, you'd mark this specific token as revoked
                    // For now, let's just log it.
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("[AuthService] Logout requested");
        return new Reponse(true, "Déconnexion réussie.", null);
    }

    public Reponse logoutAll(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("userId")) {
                return new Reponse(false, "Contexte utilisateur manquant.", null);
            }

            int userId = (int) params.get("userId");
            RefreshTokenDAO dao = new RefreshTokenDAO();
            dao.revokeAllForUser(userId);

            System.out.println("[AuthService] Global logout for userId=" + userId);
            return new Reponse(true, "Déconnexion de tous les appareils réussie.", null);
        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du logoutAll : " + e.getMessage());
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public static int getUserIdFromToken(String token) {
        try {
            if (token == null || token.isBlank()) return -1;
            JWTService.TokenClaims claims = JWTService.verifyAccessToken(token);
            return Integer.parseInt(claims.userId());
        } catch (Exception e) {
            return -1;
        }
    }
    // Legacy helpers removed - Use ClientHandler/JWTService for validation
}
