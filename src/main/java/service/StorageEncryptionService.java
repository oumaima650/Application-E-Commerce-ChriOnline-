package service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service gérant le chiffrement/déchiffrement des données en base de données
 * utilisant une clé unique stockée dans le KeyStore du serveur.
 */
public class StorageEncryptionService {
    private static final Logger logger = LogManager.getLogger(StorageEncryptionService.class);
    private static StorageEncryptionService instance;
    private SecretKey storageKey;

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private StorageEncryptionService() {
        loadKeyFromKeyStore();
    }

    public static synchronized StorageEncryptionService getInstance() {
        if (instance == null) {
            instance = new StorageEncryptionService();
        }
        return instance;
    }

    private void loadKeyFromKeyStore() {
        try {
            Properties config = new Properties();
            try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
                config.load(input);
            }

            String keystorePath = config.getProperty("server.keystore.path");
            String keystorePass = config.getProperty("server.keystore.password");
            String keystoreType = config.getProperty("server.keystore.type", "PKCS12");
            String keyAlias = "storagekey";

            KeyStore ks = KeyStore.getInstance(keystoreType);
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                ks.load(fis, keystorePass.toCharArray());
            }

            if (!ks.containsAlias(keyAlias)) {
                logger.error("ERREUR : La clé '{}' est absente du KeyStore !", keyAlias);
                return;
            }

            this.storageKey = (SecretKey) ks.getKey(keyAlias, keystorePass.toCharArray());
            
            // Fingerprint pour diagnostic (sans révéler la clé)
            byte[] encoded = this.storageKey.getEncoded();
            String fingerPrint = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(encoded, 4));
            logger.info("[AUDIT SECU] Clé de stockage '{}' chargée. Fingerprint: {}...", keyAlias, fingerPrint);

        } catch (Exception e) {
            logger.error("Erreur critique lors du chargement de la clé de stockage", e);
        }
    }

    /**
     * Chiffre une donnée avec la clé de stockage globale.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        if (storageKey == null) {
            logger.error("Le chiffrement a échoué : storageKey est null");
            return plaintext;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            logger.info("[AUDIT SECU] Opération : Chiffrement ALÉATOIRE (Standard) via StorageKey.");
            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, storageKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String result = Base64.getEncoder().encodeToString(combined);
            logger.debug("[STORAGE-SEC] Chiffrement réussi. Longueur ciphertext: {}", result.length());
            return result;
        } catch (Exception e) {
            logger.error("Erreur lors du chiffrement de la donnée", e);
            return plaintext;
        }
    }

    /**
     * Déchiffre une donnée avec la clé de stockage globale.
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return encryptedBase64;
        if (storageKey == null) {
            logger.error("Le déchiffrement a échoué : storageKey est null");
            return encryptedBase64;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length <= GCM_IV_LENGTH) return encryptedBase64;

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, storageKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            String result = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
            logger.debug("[STORAGE-SEC] Déchiffrement réussi.");
            return result;
        } catch (Exception e) {
            // Si le déchiffrement échoue, la donnée est peut-être déjà en clair ou corrompue
            return encryptedBase64;
        }
    }

    /**
     * Chiffre une donnée de manière déterministe (pour la recherche SQL).
     * Utilise un IV fixe au lieu d'un IV aléatoire.
     */
    public String encryptDeterministic(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        if (storageKey == null) return plaintext;

        try {
            logger.info("[AUDIT SECU] Opération : Chiffrement DÉTERMINISTE (Searchable) via StorageKey.");
            byte[] iv = new byte[GCM_IV_LENGTH]; 

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, storageKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("Erreur lors du chiffrement déterministe", e);
            return plaintext;
        }
    }

    /**
     * Déchiffre une donnée chiffrée de manière déterministe.
     */
    public String decryptDeterministic(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return encryptedBase64;
        if (storageKey == null) return encryptedBase64;

        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = new byte[GCM_IV_LENGTH]; 

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, storageKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedBase64;
        }
    }
}
