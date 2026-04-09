package service;

import dao.ClientDAO;
import dao.UtilisateurDAO;
import model.Client;
import model.Utilisateur;
import shared.Reponse;
import shared.Requete;
import dao.RefreshTokenDAO;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private final LoginAttemptLimitService securityService;
    private static final long ANTI_TIMING_DELAY_MS = 200;
    private final PasswordResetService passwordResetService = new PasswordResetService();

    public AuthService(LoginAttemptLimitService securityService) {
        this.securityService = securityService;
    }

    public Reponse login(Requete requete) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")) {
                return applyTimingDelay(new Reponse(false, "Champs manquants : email et motDePasse requis.", null),
                        startTime);
            }

            String email = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");

            String clientIp = (String) params.getOrDefault("clientIp", "UNKNOWN");
            String recaptchaToken = (String) params.get("recaptchaToken");

            // --- reCAPTCHA Verification ---
            RecaptchaService recaptchaService = new RecaptchaService();
            boolean captchaValide = recaptchaService.verify(recaptchaToken);

            UtilisateurDAO.LoginData loginData = UtilisateurDAO.getLoginData(email);

            if (loginData == null || !PasswordService.verify(motDePasse, loginData.hash()) || !captchaValide) {
                String failureMsg = securityService.registerFailure(clientIp, email, captchaValide);
                return applyTimingDelay(new Reponse(false, failureMsg, null), startTime);
            }

            int userId = loginData.id();
            Utilisateur user = UtilisateurDAO.findById(userId);

            if (user instanceof Client && "BANNI".equals(((Client) user).getStatut())) {
                return applyTimingDelay(new Reponse(false, "Ce compte a été banni par l'administrateur.", null),
                        startTime);
            }

            securityService.registerSuccess(clientIp, email, captchaValide);

            String role = (user instanceof Client) ? "CLIENT" : "ADMIN";
            String sessionId = UUID.randomUUID().toString();

            String accessToken = JWTService.generateAccessToken(String.valueOf(userId), role, sessionId);
            String refreshToken = JWTService.generateRefreshToken();

            RefreshTokenDAO refreshTokenDAO = new RefreshTokenDAO();
            refreshTokenDAO.save(userId, refreshToken, LocalDateTime.now().plusDays(7));

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("accessToken", accessToken);
            donnees.put("refreshToken", refreshToken);
            donnees.put("utilisateur", user);
            donnees.put("typeUtilisateur", role);

            System.out.println("[AuthService] Login OK — email=" + email + " userId=" + userId);
            return applyTimingDelay(new Reponse(true, "Connexion réussie.", donnees), startTime);

        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du login : " + e.getMessage());
            return applyTimingDelay(new Reponse(false, "Erreur serveur lors de la connexion.", null), startTime);
        }
    }

    private Reponse applyTimingDelay(Reponse reponse, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < ANTI_TIMING_DELAY_MS) {
            try {
                Thread.sleep(ANTI_TIMING_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return reponse;
    }

    public Reponse signup(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")
                    || !params.containsKey("nom") || !params.containsKey("prenom")
                    || !params.containsKey("telephone")) {
                return new Reponse(false, "Champs manquants : email, motDePasse, nom, prenom, telephone requis.", null);
            }

            String email = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");
            String nom = (String) params.get("nom");
            String prenom = (String) params.get("prenom");
            String telephone = (String) params.get("telephone");
            String recaptchaToken = (String) params.get("recaptchaToken");

            // --- reCAPTCHA Verification ---
            RecaptchaService recaptchaService = new RecaptchaService();
            if (!recaptchaService.verify(recaptchaToken)) {
                return new Reponse(false, "Vérification reCAPTCHA échouée. Veuillez réessayer.", null);
            }

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

            String hashedPw = PasswordService.hash(motDePasse);
            ClientDAO clientDAO = new ClientDAO();
            Client client = clientDAO.create(email, hashedPw, nom, prenom, telephone);

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("utilisateur", client);

            System.out.println("[AuthService] Signup OK — userId=" + client.getIdUtilisateur());
            new NotificationService()
                    .notifierAdmins("Nouveau client inscrit : " + prenom + " " + nom + " (" + email + ")");

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

            if (info == null)
                return new Reponse(false, "INVALID_TOKEN", null);
            if (info.isRevoked())
                return new Reponse(false, "TOKEN_REVOKED", null);
            if (info.isUsed()) {
                dao.revokeAllForUser(info.userId());
                return new Reponse(false, "REFRESH_TOKEN_REUSE_DETECTED", null);
            }
            if (info.expiresAt().isBefore(LocalDateTime.now()))
                return new Reponse(false, "TOKEN_EXPIRED", null);

            dao.markAsUsed(info.id());

            Utilisateur user = UtilisateurDAO.findById(info.userId());
            String role = (user instanceof Client) ? "CLIENT" : "ADMIN";
            String newSessionId = UUID.randomUUID().toString();

            String newAccessToken = JWTService.generateAccessToken(String.valueOf(user.getIdUtilisateur()), role,
                    newSessionId);
            String newRefreshToken = JWTService.generateRefreshToken();
            dao.save(user.getIdUtilisateur(), newRefreshToken, LocalDateTime.now().plusDays(7));

            Map<String, Object> donnees = new HashMap<>();
            donnees.put("accessToken", newAccessToken);
            donnees.put("refreshToken", newRefreshToken);

            return new Reponse(true, "Token rafraîchi.", donnees);

        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse logout(Requete requete) {
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
            new RefreshTokenDAO().revokeAllForUser(userId);

            return new Reponse(true, "Déconnexion de tous les appareils réussie.", null);
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse handleRequestReset(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("email")) {
                return new Reponse(false, "Email requis.", null);
            }
            String email = (String) params.get("email");

            if (!UtilisateurDAO.userExist(email)) {
                return new Reponse(false, "Aucun compte associé à cet email.", null);
            }

            if (passwordResetService.sendResetCode(email)) {
                return new Reponse(true, "Un code de vérification a été envoyé à votre e-mail.", null);
            } else {
                return new Reponse(false, "Erreur lors de l'envoi de l'e-mail.", null);
            }
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse handleConfirmReset(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("email") || !params.containsKey("code")
                    || !params.containsKey("newPassword")) {
                return new Reponse(false, "Données manquantes.", null);
            }

            String email = (String) params.get("email");
            String code = (String) params.get("code");
            String newPassword = (String) params.get("newPassword");

            if (!passwordResetService.verifyCode(email, code)) {
                return new Reponse(false, "Code invalide ou expiré.", null);
            }

            PasswordService.ValidationResult strength = PasswordService.validateStrength(newPassword);
            if (!strength.isValid())
                return new Reponse(false, strength.message(), null);

            String hashedPw = PasswordService.hash(newPassword);
            if (UtilisateurDAO.updatePassword(email, hashedPw)) {
                passwordResetService.clearCode(email);
                securityService.completePasswordReset(email);
                return new Reponse(true, "Mot de passe modifié. Compte débloqué.", null);
            } else {
                return new Reponse(false, "Erreur lors de la mise à jour.", null);
            }
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public static int getUserIdFromToken(String token) {
        try {
            if (token == null || token.isBlank())
                return -1;
            return Integer.parseInt(JWTService.verifyAccessToken(token).userId());
        } catch (Exception e) {
            return -1;
        }
    }
}
