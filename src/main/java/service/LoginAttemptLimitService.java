package service;

import dao.LoginSecurityDAO;
import dao.SecurityAuditDAO;
import model.LoginSecurityState;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class LoginAttemptLimitService {

    private final LoginSecurityDAO securityDAO = new LoginSecurityDAO();
    private final SecurityAuditDAO auditDAO = new SecurityAuditDAO();
    private final EmailService emailService = new EmailService();

    // L'envoi d'emails réels est désormais géré par le EmailService officiel
    // pour les niveaux 2 et 3.

    // Configuration des seuils
    private static final int L1_MAX_ATTEMPTS = 5;
    private static final int L2_MAX_ATTEMPTS = 3;
    private static final int L3_MAX_ATTEMPTS = 2;

    private static final int L1_BLOCK_MINUTES = 10;
    private static final int L2_BLOCK_MINUTES = 30;
    private static final int L3_BLOCK_HOURS = 24;

    /**
     * Vérifie si un identifiant (IP ou Email) est actuellement bloqué.
     */
    public CheckResult checkAccess(String identifier, String type) {
        try {
            LoginSecurityState state = securityDAO.findByIdentifier(identifier, type);
            if (state == null) return new CheckResult(true, null);

            if (state.isBlocked()) {
                String message = getBlockMessage(state);
                return new CheckResult(false, message);
            }

            if (state.isMustResetPassword()) {
                return new CheckResult(false, "Veuillez réinitialiser votre mot de passe pour continuer vos achats.");
            }

            return new CheckResult(true, null);
        } catch (SQLException e) {
            e.printStackTrace();
            return new CheckResult(true, null); // En cas d'erreur DB, on laisse passer par défaut
        }
    }

    /**
     * Gère un échec de connexion.
     */
    public String registerFailure(String ip, String email, boolean captchaValide) {
        // Logging
        int level = 1;
        try {
            LoginSecurityState ipState = getOrCreateState(ip, "IP");
            LoginSecurityState emailState = getOrCreateState(email, "EMAIL");
            level = Math.max(ipState.getCurrentLevel(), emailState.getCurrentLevel());

            auditDAO.log(ip, email, level, "FAILED", captchaValide);

            if (!captchaValide) {
                return "CAPTCHA invalide. Veuillez réessayer.";
            }

            // Incrémentation et gestion des niveaux pour IP et Email
            processFailure(ipState);
            processFailure(emailState);

            securityDAO.upsert(ipState);
            securityDAO.upsert(emailState);

            // Retourner le message correspondant au niveau de l'email (prioritaire pour l'utilisateur)
            return getBlockMessage(emailState);

        } catch (SQLException e) {
            e.printStackTrace();
            return "Identifiants incorrects.";
        }
    }

    /**
     * Gère un succès de connexion (Reset complet).
     */
    public void registerSuccess(String ip, String email, boolean captchaValide) {
        try {
            securityDAO.reset(ip, "IP");
            securityDAO.reset(email, "EMAIL");
            auditDAO.log(ip, email, 1, "SUCCESS", captchaValide);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void processFailure(LoginSecurityState state) {
        state.setCurrentAttempts(state.getCurrentAttempts() + 1);
        state.setLastAttemptAt(LocalDateTime.now());

        int max = switch (state.getCurrentLevel()) {
            case 1 -> L1_MAX_ATTEMPTS;
            case 2 -> L2_MAX_ATTEMPTS;
            case 3 -> L3_MAX_ATTEMPTS;
            default -> 1;
        };

        if (state.getCurrentAttempts() >= max) {
            applyBlock(state);
        }
    }

    private void applyBlock(LoginSecurityState state) {
        int minutes = 0;
        int level = state.getCurrentLevel();

        switch (level) {
            case 1 -> {
                minutes = L1_BLOCK_MINUTES;
                state.setCurrentLevel(2);
            }
            case 2 -> {
                minutes = L2_BLOCK_MINUTES;
                state.setCurrentLevel(3);
                
                if (state.getIdentifier().contains("@")) {
                    emailService.sendEmail(state.getIdentifier(), 
                        "Tentative suspecte sur votre compte", 
                        "Bonjour,\n\nUne activité suspecte a été détectée sur votre compte ChriOnline. " +
                        "Par sécurité, l'accès est temporairement suspendu.\n\n" +
                        "Si vous n'êtes pas à l'origine de cette tentative, nous vous conseillons de réinitialiser votre mot de passe.\n\n" +
                        "L'équipe Sécurité ChriOnline.");
                }
                System.out.println("[SECURITY] Tentative suspecte sur " + state.getIdentifier() + " (Niveau 2 atteint)");
            }
            case 3 -> {
                minutes = L3_BLOCK_HOURS * 60;
                state.setCurrentLevel(4);
                state.setMustResetPassword(true);

                if (state.getIdentifier().contains("@")) {
                    emailService.sendEmail(state.getIdentifier(), 
                        "ALERTE SÉCURITÉ CRITIQUE", 
                        "Bonjour,\n\nVotre compte a été suspendu suite à de multiples échecs répétés.\n\n" +
                        "Veuillez réinitialiser votre mot de passe pour retrouver l'accès.\n\n" +
                        "L'équipe Sécurité ChriOnline.");
                }
                System.out.println("[SECURITY] ALERTE CRITIQUE sur " + state.getIdentifier() + " (Niveau 3 atteint)");
            }
        }

        state.setCurrentAttempts(0);
        if (minutes > 0) {
            state.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
        }
    }

    private String getBlockMessage(LoginSecurityState state) {
        if (!state.isBlocked() && !state.isMustResetPassword()) return "Identifiants incorrects.";

        boolean isIP = "IP".equals(state.getType());
        String subject = isIP ? "Votre connexion est suspendue" : "Votre compte est suspendu";

        return switch (state.getCurrentLevel()) {
            case 2 -> "Trop de tentatives. Réessayez dans 10 min ou réinitialisez votre mot de passe.";
            case 3 -> subject + " pour 30 minutes par sécurité. Réessayez aprés ou réinitialisez votre mot de passe.";
            case 4 -> state.isMustResetPassword() ? 
                      "Veuillez réinitialiser votre mot de passe pour continuer vos achats." :
                      subject + " pour 24h par sécurité.";
            default -> "Identifiants incorrects.";
        };
    }

    /**
     * Resets the security state for an email after a successful password reset.
     * @param email The user's email.
     */
    public void completePasswordReset(String email) {
        try {
            securityDAO.reset(email, "EMAIL");
            auditDAO.log(null, email, 0, "PASSWORD_RESET_SUCCESS", true);
            System.out.println("[SecurityService] État de sécurité réinitialisé pour " + email + " (Password Reset)");
        } catch (Exception e) {
            System.err.println("[SecurityService] Erreur lors du reset après password change : " + e.getMessage());
        }
    }

    private LoginSecurityState getOrCreateState(String identifier, String type) throws SQLException {
        LoginSecurityState state = securityDAO.findByIdentifier(identifier, type);
        if (state == null) {
            state = new LoginSecurityState(identifier, type);
        }
        return state;
    }

    public static class CheckResult {
        public final boolean allowed;
        public final String message;

        public CheckResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
    }
}
