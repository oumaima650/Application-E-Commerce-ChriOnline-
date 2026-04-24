package client.crypto;

import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service pour la dérivation de clé (KDF) côté client.
 * Utilise Argon2id pour transformer un mot de passe en une clé binaire de 256 bits (KEK).
 */
public class KDFService {

    // Paramètres Argon2id recommandés
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    private static final int KEY_LENGTH = 32; // 256 bits

    private static final Argon2Advanced argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);

    /**
     * Génère un sel aléatoire de 16 octets.
     */
    public static String generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Dérive une clé (KEK) à partir du mot de passe et du sel via Argon2id.
     */
    public static byte[] deriveKEK(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        char[] passwordChars = password.toCharArray();
        
        try {
            // Utilisation de la signature : iterations, memory, parallelism, password, charset, salt
            byte[] hash = argon2.rawHash(ITERATIONS, MEMORY, PARALLELISM, passwordChars, java.nio.charset.StandardCharsets.UTF_8, salt);
            
            // On s'assure d'avoir une clé de 32 octets (256 bits)
            byte[] kek = new byte[KEY_LENGTH];
            System.arraycopy(hash, 0, kek, 0, Math.min(hash.length, KEY_LENGTH));
            return kek;
        } catch (Exception e) {
            System.err.println("[KDF] Erreur critique lors de la dérivation de clé : " + e.getMessage());
            return new byte[KEY_LENGTH];
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }
}
