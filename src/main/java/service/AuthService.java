package service;

import dao.ClientDAO;
import dao.UtilisateurDAO;
import dao.ConnexionBDD;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    private final LoginAttemptLimitService securityService;
    // [WHITELIST IP ADMIN] Instance du SecurityManager pour vérifier l'IP lors du login admin
    private final SecurityManager securityManager;
    private static final long ANTI_TIMING_DELAY_MS = 200;


    public AuthService(LoginAttemptLimitService securityService, SecurityManager securityManager) {
        this.securityService = securityService;
        // [WHITELIST IP ADMIN] Réutilisation de l'instance existante (whitelist déjà chargée)
        this.securityManager = securityManager;
    }

    public Reponse login(Requete requete) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> params = requete.getParametres();

            if (params == null || !params.containsKey("email") || !params.containsKey("motDePasse")) {
                return applyTimingDelay(new Reponse(false, "Champs manquants : email et motDePasse requis.", null), startTime);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");

            String clientIp = (String) params.getOrDefault("clientIp", "UNKNOWN");
            String recaptchaToken = (String) params.get("recaptchaToken");
            
            // --- reCAPTCHA Verification (Via SecurityManager) ---
            boolean captchaValide = securityManager.verifyRecaptcha(recaptchaToken);


            UtilisateurDAO.LoginData loginData = UtilisateurDAO.getLoginData(email);

            if (loginData == null || !PasswordService.verify(motDePasse, loginData.hash()) || !captchaValide) {
                String failureMsg = securityService.registerFailure(clientIp, email, captchaValide);
                return applyTimingDelay(new Reponse(false, failureMsg, null), startTime);
            }

            int userId = loginData.id();
            Utilisateur user = UtilisateurDAO.findById(userId);


            // --- [WHITELIST IP ADMIN] Étape 3 : Vérification IP pour les ADMINS uniquement ---
            // Si l'utilisateur est un ADMIN et que son IP n'est pas dans la whitelist → rejet immédiat
            // Les CLIENTs ne sont PAS affectés par ce contrôle (flux inchangé)
            if (!(user instanceof Client)) {
                if (!securityManager.isIpAuthorized(clientIp)) {
                    logger.error("LOGIN ADMIN REJETÉ - IP non autorisée : {} | Email : {}", clientIp, email);
                    return applyTimingDelay(new Reponse(false, "IP_NOT_AUTHORIZED", null), startTime);
                }
            }
            // --- Fin vérification IP Admin ---

            if (user instanceof Client) {
                String statut = ((Client) user).getStatut();
                if ("BANNI".equals(statut)) {
                    return applyTimingDelay(new Reponse(false, "Ce compte a été banni par l'administrateur.", null), startTime);
                }
                if ("EN_ATTENTE".equals(statut)) {
                    securityManager.sendTwoFactorCode(email);
                    return applyTimingDelay(new Reponse(false, "SIGNUP_VERIFICATION_REQUIRED", null), startTime);
                }

            }

            // --- 2FA Check (Via SecurityManager) ---
            if (loginData.twoFactorEnabled()) {
                if (securityManager.sendTwoFactorCode(email)) {
                    System.out.println("[AuthService] 2FA required for email=" + email);
                    return applyTimingDelay(new Reponse(false, "2FA_REQUIRED", null), startTime);
                } else {
                    return applyTimingDelay(new Reponse(false, "Erreur lors de l'envoi du code 2FA.", null), startTime);
                }
            }


            securityService.registerSuccess(clientIp, email, captchaValide);
            return applyTimingDelay(generateAuthReponse(user), startTime);

        } catch (SQLException e) {
            System.err.println("[AuthService] Erreur SQL lors du login : " + e.getMessage());
            return applyTimingDelay(new Reponse(false, "Erreur serveur lors de la connexion.", null), startTime);
        }
    }

    private Reponse generateAuthReponse(Utilisateur user) throws SQLException {
        int userId = user.getIdUtilisateur();
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
        
        // --- CLIENT-SIDE DECRYPTION DATA ---
        if (user.getEncryptionSalt() != null) {
            donnees.put("encryptionSalt", user.getEncryptionSalt());
            donnees.put("wrappedDek", user.getWrappedDek());
        }

        return new Reponse(true, "Connexion réussie.", donnees);
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
                    || !params.containsKey("nom") || !params.containsKey("prenom") || !params.containsKey("telephone")) {
                return new Reponse(false, "Champs manquants : email, motDePasse, nom, prenom, telephone requis.", null);
            }

            String email      = (String) params.get("email");
            String motDePasse = (String) params.get("motDePasse");
            String nom        = (String) params.get("nom");
            String prenom     = (String) params.get("prenom");
            String telephone  = (String) params.get("telephone");
            String dobString  = (String) params.get("dateNaissance"); // ISO format YYYY-MM-DD
            String recaptchaToken = (String) params.get("recaptchaToken");

            // java.time.LocalDate dateNaissance = java.time.LocalDate.parse(dobString); // Supprimé car date chiffrée
 
            // --- reCAPTCHA Verification (Via SecurityManager) ---
            if (!securityManager.verifyRecaptcha(recaptchaToken)) {
                return new Reponse(false, "Vérification reCAPTCHA échouée. Veuillez réessayer.", null);
            }


            /* Supprimé pour Zero-Knowledge : le serveur ne peut plus vérifier l'âge
            if (java.time.Period.between(dateNaissance, java.time.LocalDate.now()).getYears() < 16) {
                return new Reponse(false, "Vous devez avoir au moins 16 ans pour vous inscrire.", null);
            }
            */

            // --- Identity-based Password Safety Check ---
            // if (containsIdentityInfo(motDePasse, nom, prenom, dateNaissance)) { // Supprimé car date chiffrée

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

            String encryptionSalt = (String) params.get("encryptionSalt");
            String wrappedDek = (String) params.get("wrappedDek");

            if (encryptionSalt == null || wrappedDek == null || encryptionSalt.isBlank() || wrappedDek.isBlank()) {
                return new Reponse(false, "Données de chiffrement manquantes : encryptionSalt et wrappedDek requis.", null);
            }

            // --- 2-Step Signup Verification ---
            // We create the account as 'EN_ATTENTE' and send verification code
            String hashedPw = PasswordService.hash(motDePasse);
            ClientDAO clientDAO = new ClientDAO();
            Client client = clientDAO.create(email, hashedPw, encryptionSalt, wrappedDek, nom, prenom, telephone, dobString);

            if (securityManager.sendTwoFactorCode(email)) {
                System.out.println("[AuthService] Signup pending verification for email=" + email);
                return new Reponse(false, "SIGNUP_VERIFICATION_REQUIRED", null);
            } else {
                // Cleanup: Delete the account if the initial email fails to avoid ghost accounts
                try {
                    UtilisateurDAO.delete(client.getIdUtilisateur());
                } catch (SQLException ex) {
                    System.err.println("[AuthService] Échec du nettoyage après erreur email : " + ex.getMessage());
                }
                return new Reponse(false, "Échec de l'envoi de l'e-mail de vérification. Compte non créé. Veuillez réessayer.", null);
            }

        } catch (Exception e) {
            System.err.println("[AuthService] Erreur lors du signup : " + e.getMessage());
            return new Reponse(false, "Erreur serveur lors de l'inscription.", null);
        }
    }

    private boolean containsIdentityInfo(String password, String nom, String prenom, java.time.LocalDate dob) {
        String p = password.toLowerCase();
        String n = nom.toLowerCase();
        String pr = prenom.toLowerCase();
        String year = String.valueOf(dob.getYear());
        String day = String.format("%02d", dob.getDayOfMonth());
        String month = String.format("%02d", dob.getMonthValue());

        return p.contains(n) || p.contains(pr) || p.contains(year) || p.contains(day) || p.contains(month);
    }

    public Reponse handleVerifySignup(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("email") || !params.containsKey("code")) {
                return new Reponse(false, "Email et code requis.", null);
            }
            String email = (String) params.get("email");
            String code = (String) params.get("code");

            TwoFactorAuthService.VerificationResult result = securityManager.verifyTwoFactorCode(email, code);
            if (result.success()) {
                // Activate account
                String sql = "UPDATE Client SET statut = 'ACTIF' WHERE IdUtilisateur = (SELECT IdUtilisateur FROM Utilisateur WHERE email = ?)";
                try (java.sql.Connection conn = ConnexionBDD.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                
                UtilisateurDAO.LoginData loginData = UtilisateurDAO.getLoginData(email);
                Utilisateur user = UtilisateurDAO.findById(loginData.id());
                
                System.out.println("[AuthService] Signup verified OK — userId=" + user.getIdUtilisateur());
                new NotificationService().notifierAdmins("Nouveau client inscrit : " + ((Client)user).getPrenom() + " " + ((Client)user).getNom() + " (" + email + ")");
                
                return generateAuthReponse(user);
            } else {
                return new Reponse(false, result.message(), null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur serveur lors de la vérification.", null);
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

            if (info == null) return new Reponse(false, "INVALID_TOKEN", null);
            if (info.isRevoked()) return new Reponse(false, "TOKEN_REVOKED", null);
            if (info.isUsed()) {
                dao.revokeAllForUser(info.userId());
                return new Reponse(false, "REFRESH_TOKEN_REUSE_DETECTED", null);
            }
            if (info.expiresAt().isBefore(LocalDateTime.now())) return new Reponse(false, "TOKEN_EXPIRED", null);

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

            if (securityManager.sendPasswordResetCode(email)) {
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
            if (params == null || !params.containsKey("email") || !params.containsKey("code") || !params.containsKey("newPassword")) {
                return new Reponse(false, "Données manquantes.", null);
            }

            String email = (String) params.get("email");
            String code = (String) params.get("code");
            String newPassword = (String) params.get("newPassword");

            if (!securityManager.verifyPasswordResetCode(email, code)) {
                return new Reponse(false, "Code invalide ou expiré.", null);
            }

            PasswordService.ValidationResult strength = PasswordService.validateStrength(newPassword);
            if (!strength.isValid()) return new Reponse(false, strength.message(), null);

            String hashedPw = PasswordService.hash(newPassword);

            // Récupérer le nouveau sel + DEK wrappée envoyés par le client
            String newEncryptionSalt = (String) params.get("newEncryptionSalt");
            String newWrappedDek     = (String) params.get("newWrappedDek");

            boolean updated;
            if (newEncryptionSalt != null && !newEncryptionSalt.isBlank()
                    && newWrappedDek != null && !newWrappedDek.isBlank()) {
                // Mise à jour atomique : hash + nouveau sel + nouvelle DEK wrappée
                updated = UtilisateurDAO.updatePasswordAndEncryption(email, hashedPw, newEncryptionSalt, newWrappedDek);
            } else {
                // Fallback si le client n'a pas fourni les données de chiffrement
                updated = UtilisateurDAO.updatePassword(email, hashedPw);
            }

            if (updated) {
                securityManager.clearPasswordResetCode(email);
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
            if (token == null || token.isBlank()) return -1;
            return Integer.parseInt(JWTService.verifyAccessToken(token).userId());
        } catch (Exception e) {
            return -1;
        }
    }

    public Reponse handleVerify2FALogin(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("email") || !params.containsKey("code")) {
                return new Reponse(false, "Email et code requis.", null);
            }
            String email = (String) params.get("email");
            String code = (String) params.get("code");

            TwoFactorAuthService.VerificationResult result = securityManager.verifyTwoFactorCode(email, code);
            if (result.success()) {
                UtilisateurDAO.LoginData loginData = UtilisateurDAO.getLoginData(email);
                Utilisateur user = UtilisateurDAO.findById(loginData.id());
                
                String clientIp = (String) params.getOrDefault("clientIp", "UNKNOWN");
                securityService.registerSuccess(clientIp, email, true);
                
                return generateAuthReponse(user);
            } else {
                return new Reponse(false, result.message(), null);
            }
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse handleToggle2FA(Requete requete) {
        try {
            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("enabled")) {
                return new Reponse(false, "Status requis.", null);
            }
            boolean enabled = (boolean) params.get("enabled");
            String code = (String) params.get("code"); // Only required if enabling

            int userId = getUserIdFromToken(requete.getTokenSession());
            if (userId == -1) return new Reponse(false, "Non autorisé.", null);

            Utilisateur user = UtilisateurDAO.findById(userId);
            if (user == null) return new Reponse(false, "Utilisateur introuvable.", null);

            if (enabled) {
                if (code == null) return new Reponse(false, "Code de confirmation requis.", null);
                TwoFactorAuthService.VerificationResult result = securityManager.verifyTwoFactorCode(user.getEmail(), code);
                if (!result.success()) return new Reponse(false, result.message(), null);
            }

            if (UtilisateurDAO.updateTwoFactorStatus(user.getEmail(), enabled)) {
                return new Reponse(true, enabled ? "2FA activé avec succès." : "2FA désactivé avec succès.", null);
            } else {
                return new Reponse(false, "Erreur lors de la mise à jour.", null);
            }
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse handleGenerate2FACode(Requete requete) {
        try {
            int userId = getUserIdFromToken(requete.getTokenSession());
            if (userId == -1) return new Reponse(false, "Non autorisé.", null);

            Utilisateur user = UtilisateurDAO.findById(userId);
            if (user == null) return new Reponse(false, "Utilisateur introuvable.", null);

            if (securityManager.sendTwoFactorCode(user.getEmail())) {
                return new Reponse(true, "Un code de confirmation a été envoyé à votre email.", null);
            } else {
                return new Reponse(false, "Erreur lors de l'envoi de l'e-mail.", null);
            }
        } catch (SQLException e) {
            return new Reponse(false, "Erreur serveur.", null);
        }
    }

    public Reponse handleChangePassword(Requete requete) {
        try {
            int userId = getUserIdFromToken(requete.getTokenSession());
            if (userId == -1) return new Reponse(false, "Non autorisé.", null);

            Map<String, Object> params = requete.getParametres();
            if (params == null || !params.containsKey("newPassword") || !params.containsKey("newSalt") || !params.containsKey("newWrappedDek")) {
                return new Reponse(false, "Paramètres manquants.", null);
            }

            String newPassword   = (String) params.get("newPassword");
            String newSalt       = (String) params.get("newSalt");
            String newWrappedDek = (String) params.get("newWrappedDek");

            // Vérification de sécurité sur le mot de passe (server-side backup)
            PasswordService.ValidationResult strength = PasswordService.validateStrength(newPassword);
            if (!strength.isValid()) {
                return new Reponse(false, strength.message(), null);
            }

            String hashedPw = PasswordService.hash(newPassword);
            if (UtilisateurDAO.updatePasswordAndEncryption(userId, hashedPw, newSalt, newWrappedDek)) {
                return new Reponse(true, "Mot de passe et clés mis à jour avec succès.", null);
            } else {
                return new Reponse(false, "Erreur lors de la mise à jour en base de données.", null);
            }
        } catch (Exception e) {
            System.err.println("[AuthService] Erreur changement mot de passe : " + e.getMessage());
            return new Reponse(false, "Erreur serveur lors du changement de mot de passe.", null);
        }
    }
}