package client.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Service pour la gestion de l'Envelope Encryption côté client.
 */
public class EnvelopeEncryptionService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Génère une nouvelle clé de données (DEK) AES-256 robuste.
     */
    public static SecretKey generateDEK() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES);
        keyGen.init(256);
        return keyGen.generateKey();
    }

    /**
     * Chiffre ("Wrappe") la DEK avec la KEK.
     * @param dek La clé de données en clair.
     * @param kek La clé maîtresse dérivée du mot de passe.
     * @return La version chiffrée en Base64.
     */
    public static String wrapDEK(SecretKey dek, byte[] kek) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKeySpec kekSpec = new SecretKeySpec(kek, AES);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, kekSpec, spec);
        byte[] encryptedDek = cipher.doFinal(dek.getEncoded());

        // Combinaison IV + Données chiffrées
        byte[] combined = new byte[iv.length + encryptedDek.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedDek, 0, combined, iv.length, encryptedDek.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Déchiffre ("Unwrappe") la DEK avec la KEK.
     */
    public static SecretKey unwrapDEK(String wrappedDekBase64, byte[] kek) throws Exception {
        byte[] combined = Base64.getDecoder().decode(wrappedDekBase64);
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedDek = new byte[combined.length - GCM_IV_LENGTH];
        
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, encryptedDek, 0, encryptedDek.length);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKeySpec kekSpec = new SecretKeySpec(kek, AES);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, kekSpec, spec);
        byte[] decryptedKey = cipher.doFinal(encryptedDek);

        return new SecretKeySpec(decryptedKey, AES);
    }

    // ─────────────────────────────────────────────────────────────
    // Chiffrement de champs individuels (stockage BDD Zero-Knowledge)
    // ─────────────────────────────────────────────────────────────

    /**
     * Chiffre une valeur String avec la DEK (AES-GCM).
     * Utilisé pour chiffrer les données sensibles AVANT de les envoyer au serveur pour stockage.
     * Format du résultat : Base64(IV[12] + CipherText + GCM_Tag[16])
     *
     * @param plaintext La valeur en clair à chiffrer.
     * @param dek       La clé de données (sessionDek) de l'utilisateur.
     * @return La valeur chiffrée encodée en Base64.
     */
    public static String encryptField(String plaintext, SecretKey dek) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, dek, spec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Déchiffre une valeur chiffrée par encryptField() avec la DEK.
     * Utilisé pour déchiffrer les données sensibles reçues du serveur.
     *
     * @param encryptedBase64 La valeur chiffrée en Base64.
     * @param dek             La clé de données (sessionDek) de l'utilisateur.
     * @return La valeur en clair.
     */
    public static String decryptField(String encryptedBase64, SecretKey dek) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        if (combined.length <= GCM_IV_LENGTH) throw new IllegalArgumentException("Données chiffrées trop courtes.");

        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, dek, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }
}
