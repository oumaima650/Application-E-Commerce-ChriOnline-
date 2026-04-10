package service;

import dao.TwoFactorAuthDAO;
import dao.UtilisateurDAO;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TwoFactorAuthService {

    private final TwoFactorAuthDAO dao = new TwoFactorAuthDAO();
    private final EmailService emailService = new EmailService();
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    public boolean send2FACode(String email) throws SQLException {
        String code = generateOTP(6);
        String codeHash = hashOTP(code);
        
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
        dao.upsertCode(email, codeHash, expiresAt);
        
        String subject = "🔑 Votre code de sécurité ChriOnline : " + code;
        String htmlContent = "<html><body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 40px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);'>" +
            "<div style='text-align: center; margin-bottom: 32px;'>" +
            "<h1 style='color: #1e293b; font-size: 24px; margin: 0;'>ChriOnline</h1>" +
            "</div>" +
            "<p style='color: #475569; font-size: 16px; line-height: 24px;'>" +
            "Bonjour,<br><br>Vous avez demandé un code de vérification pour sécuriser votre accès. Veuillez utiliser le code ci-dessous :" +
            "</p>" +
            "<div style='background-color: #f1f5f9; border-radius: 8px; padding: 24px; text-align: center; margin: 32px 0;'>" +
            "<span style='font-family: monospace; font-size: 36px; font-weight: bold; color: #db2777; letter-spacing: 8px;'>" + code + "</span>" +
            "</div>" +
            "<p style='color: #64748b; font-size: 14px; text-align: center;'>" +
            "Ce code est à usage unique et expirera dans <b>" + EXPIRY_MINUTES + " minutes</b>." +
            "</p>" +
            "<div style='border-top: 1px solid #e2e8f0; margin-top: 32px; padding-top: 24px; color: #94a3b8; font-size: 12px; text-align: center;'>" +
            "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail en toute sécurité.<br>© 2026 ChriOnline. Tous droits réservés." +
            "</div>" +
            "</div>" +
            "</body></html>";
        
        return emailService.sendHtmlEmail(email, subject, htmlContent);
    }

    /**
     * Verifies the provided code.
     * @return true if valid, false otherwise (handling attempts and expiry).
     */
    public VerificationResult verifyCode(String email, String userInputCode) throws SQLException {
        TwoFactorAuthDAO.CodeInfo info = dao.findCode(email);
        
        if (info == null) {
            return new VerificationResult(false, "Aucun code en cours. Veuillez recommencer.");
        }
        
        if (info.expiresAt().isBefore(LocalDateTime.now())) {
            dao.deleteCode(email);
            return new VerificationResult(false, "Code expiré. Veuillez en demander un nouveau.");
        }
        
        if (info.attempts() >= MAX_ATTEMPTS) {
            dao.deleteCode(email);
            return new VerificationResult(false, "Trop de tentatives. Code invalidé. Veuillez recommencer.");
        }
        
        if (info.hash().equals(hashOTP(userInputCode))) {
            dao.deleteCode(email);
            return new VerificationResult(true, "Code valide.");
        } else {
            dao.incrementAttempts(email);
            if (info.attempts() + 1 >= MAX_ATTEMPTS) {
                dao.deleteCode(email);
                return new VerificationResult(false, "Code incorrect. Trop de tentatives, le code a expiré.");
            }
            return new VerificationResult(false, "Code incorrect. Tentatives restantes : " + (MAX_ATTEMPTS - (info.attempts() + 1)));
        }
    }

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private String generateOTP(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private String hashOTP(String code) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    public record VerificationResult(boolean success, String message) {}
}
