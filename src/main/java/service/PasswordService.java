package service;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;

/**
 * Service to handle secure password hashing using BCrypt and strength validation.
 */
public class PasswordService {

    private static final int BCRYPT_SALT_ROUNDS = 12;
    private static final int MIN_LENGTH = 8;

    private static final Pattern HAS_UPPER    = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT    = Pattern.compile(".*[0-9].*");
    private static final Pattern HAS_SPECIAL  = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*");

    public record ValidationResult(boolean isValid, String message) {}

    /**
     * Hashes a plain text password using BCrypt with cost factor 12.
     * 
     * @param plainPassword The password to hash.
     * @return The BCrypt hash string.
     * @throws IllegalArgumentException if password validation fails.
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        }
        if (plainPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_SALT_ROUNDS));
    }

    /**
     * Verifies a plain text password against a stored BCrypt hash.
     * 
     * @param plainPassword The password to check.
     * @param storedHash The hash retrieved from the database.
     * @return true if matches, false otherwise (including null arguments).
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            System.err.println("[PasswordService] Erreur lors de la vérification : " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a password meets the required complexity rules.
     */
    public static ValidationResult validateStrength(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return new ValidationResult(false, "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }
        if (!HAS_UPPER.matcher(password).matches()) {
            return new ValidationResult(false, "Le mot de passe doit contenir au moins une lettre majuscule.");
        }
        if (!HAS_DIGIT.matcher(password).matches()) {
            return new ValidationResult(false, "Le mot de passe doit contenir au moins un chiffre.");
        }
        if (!HAS_SPECIAL.matcher(password).matches()) {
            return new ValidationResult(false, "Le mot de passe doit contenir au moins un caractère spécial.");
        }
        return new ValidationResult(true, "Mot de passe conforme.");
    }
}
