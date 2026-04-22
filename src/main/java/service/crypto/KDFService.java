package service.crypto;

import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service pour la dérivation de clé (KDF).
 * Utilise Argon2id pour transformer un mot de passe en une clé binaire de 256 bits (KEK).
 */
public class KDFService {

    // Paramètres Argon2id recommandés (A ajuster selon les performances du serveur)
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
     * @param password Le mot de passe de l'utilisateur.
     * @param saltBase64 Le sel au format Base64.
     * @return La clé dérivée en tableau d'octets.
     */
    public static byte[] deriveKEK(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        
        // Argon2-jvm ne propose pas de sortie brute directe de longueur fixe facilement 
        // sans passer par le hash. On peut utiliser les octets du hash résultant.
        // On convertit le mot de passe en char[] pour plus de sécurité (effacement possible)
        char[] passwordChars = password.toCharArray();
        
        try {
            // Note: argon2.hash produit une chaîne formatée ($argon2id$v=19$m=65536...).
            // Pour une clé de dérivation brute, on peut extraire le hash binaire.
            // Cependant, la bibliothèque argon2-jvm est optimisée pour le hachage.
            // On utilise une approche stable : le hash brut.
            byte[] hash = argon2.rawHash(ITERATIONS, MEMORY, PARALLELISM, passwordChars, salt);
            
            // On s'assure d'avoir au moins 32 octets (KEY_LENGTH)
            byte[] kek = new byte[KEY_LENGTH];
            System.arraycopy(hash, 0, kek, 0, Math.min(hash.length, KEY_LENGTH));
            
            return kek;
        } finally {
            // Sécurité : effacer le mot de passe de la mémoire après usage
            argon2.wipeArray(passwordChars);
        }
    }
}
