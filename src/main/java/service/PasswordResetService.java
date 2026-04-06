package service;

import dao.PasswordResetDAO;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class PasswordResetService {

    private final PasswordResetDAO dao = new PasswordResetDAO();
    private final EmailService emailService = new EmailService();
    private static final int EXPIRY_MINUTES = 10;

    /**
     * Generates a 6-digit OTP, hashes it, stores it, and sends it via email.
     */
    public boolean sendResetCode(String email) throws SQLException {
        // Generate 6-digit code (e.g., 123456)
        String code = generateOTP(6);
        
        // Hash the code before storing (using PasswordService which uses BCrypt)
        // Since PasswordService.hash expects strong pass validation, we use a custom hash for OTP
        // Or we can just use PasswordService.hash if the code length doesn't trigger the < 8 chars error
        // Let's use BCrypt directly or modify PasswordService.
        // Actually, PasswordService has a minimum length of 8. 
        // For a 6-digit OTP, we'll use a simple SHA-256 or just relax the length for OTPs.
        
        // Let's use a simple SHA-256 for the short-lived OTP as it's faster and length-independent.
        String codeOPTHash = hashOTP(code);
        
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);
        dao.upsertCode(email, codeOPTHash, expiresAt);
        
        // Send the email
        String subject = "Votre code de réinitialisation - ChriOnline";
        String content = "Bonjour,\n\n" +
                         "Vous avez demandé la réinitialisation de votre mot de passe.\n" +
                         "Votre code de vérification est : " + code + "\n\n" +
                         "Ce code expirera dans " + EXPIRY_MINUTES + " minutes.\n" +
                         "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.\n\n" +
                         "L'équipe ChriOnline.";
        
        return emailService.sendEmail(email, subject, content);
    }

    /**
     * Verifies if the provided code matches the one stored for the email.
     */
    public boolean verifyCode(String email, String userInputCode) throws SQLException {
        String storedHash = dao.findValidCodeHash(email);
        if (storedHash == null) return false;
        
        return storedHash.equals(hashOTP(userInputCode));
    }

    public void clearCode(String email) throws SQLException {
        dao.deleteCode(email);
    }

    private String generateOTP(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
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
}
