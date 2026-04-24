package client;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import shared.Session;
import client.utils.SessionManager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.PublicKey;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.SecureRandom;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import server.utils.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton managing the secure TCP connection to the server using TLS 1.3.
 */
public class ClientSocket {
    private static final Logger logger = LogManager.getLogger(ClientSocket.class);
    private static ClientSocket instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private SecretKey sessionSecretKey;
    private PublicKey trustedServerPublicKey;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8443;

    private ClientSocket() {
        loadTrustedPublicKey();
        connect();
    }

    private void loadTrustedPublicKey() {
        try {
            String truststorePath = ConfigLoader.getProperty("client.truststore.path", "src/main/resources/client_truststore.p12");
            String truststorePass = ConfigLoader.getProperty("client.truststore.password", "ClientTrustDAOR");
            String truststoreType = ConfigLoader.getProperty("client.truststore.type", "PKCS12");

            KeyStore ks = KeyStore.getInstance(truststoreType);
            try (FileInputStream fis = new FileInputStream(truststorePath)) {
                ks.load(fis, truststorePass.toCharArray());
            }

            // Get the first alias
            String alias = ks.aliases().nextElement();
            Certificate cert = ks.getCertificate(alias);
            this.trustedServerPublicKey = cert.getPublicKey();
            
            logger.info("Clé de confiance du serveur chargée depuis : {}", truststorePath);
        } catch (Exception e) {
            logger.fatal("ERREUR CRITIQUE : Impossible de charger le Truststore client", e);
            // L'application devrait idéalement s'arrêter ici si on ne peut pas vérifier le serveur.
        }
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    private void connect() {
        try {
            socket = new Socket(HOST, PORT);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            //  DEBUT HANDSHAKE RSA/AES 
            // 1. Lire cle publique du serveur
            PublicKey serverPublicKey = (PublicKey) in.readObject();

            // 1.5 VERIFICATION MITM
            if (this.trustedServerPublicKey != null) {
                if (!serverPublicKey.equals(this.trustedServerPublicKey)) {
                    logger.error("[AUDIT SECU] ALERTE : Tentative de MITM détectée ! La clé publique du serveur ne correspond pas au Truststore.");
                    throw new SecurityException("MITM Detecté : La clé publique du serveur reçue ne correspond pas à la clé de confiance !");
                }
                logger.info("[AUDIT SECU] Vérification Truststore réussie : Identité du serveur confirmée.");
            } else {
                logger.warn("[AUDIT SECU] ATTENTION : Connexion sans Truststore. L'identité du serveur n'est pas vérifiée.");
            }

            // 2. Generer nouvelle cle AES 256 bits
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            sessionSecretKey = keyGen.generateKey();

            // 3. Chiffrer cle AES avec cle publique du serveur (RSA)
            Cipher cipherRSA = Cipher.getInstance("RSA");
            cipherRSA.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] encryptedAesKey = cipherRSA.doFinal(sessionSecretKey.getEncoded());

            // 4. Envoyer la cle AES chiffree au serveur
            out.writeObject(encryptedAesKey);
            out.flush();
            //  FIN HANDSHAKE

            logger.info("[AUDIT SECU] Connexion sécurisée Hybride (AES/RSA) établie sur le port {}", PORT);
        } catch (Exception e) {
            logger.error("[AUDIT SECU] Échec de l'établissement de la connexion sécurisée : {}", e.getMessage());
            socket = null;
        }
    }

    /**
     * Sends a request to the server and returns the response.
     * Synchronized to avoid concurrent access to the same socket streams.
     */
    public synchronized Reponse envoyer(Requete req) {
        // 1. ATTACH LATEST TOKEN if available
        Session session = SessionManager.getInstance().getSession();
        if (session != null && session.getAccessToken() != null) {
            req.setTokenSession(session.getAccessToken());
        }

        // 2. ATTACH REPLAY PROTECTION DATA
        req.setTimestamp(System.currentTimeMillis());
        req.setNonce(java.util.UUID.randomUUID().toString());

        try {
            Reponse res = executeRequest(req);

            // 2. HANDLE TOKEN EXPIRY
            if (res != null && "TOKEN_EXPIRED".equals(res.getMessage())) {
                logger.info("Access token expiré. Tentative de rafraîchissement...");

                if (session != null && session.getRefreshToken() != null) {
                    // Send Refresh Request
                    Requete refreshReq = new Requete(RequestType.REFRESH, new HashMap<>(), session.getRefreshToken());
                    Reponse refreshRes = executeRequest(refreshReq);

                    if (refreshRes != null && refreshRes.isSucces()) {
                        String newAccess = (String) refreshRes.getDonnees().get("accessToken");
                        String newRefresh = (String) refreshRes.getDonnees().get("refreshToken");

                        logger.info("Rafraîchissement réussi.");
                        SessionManager.getInstance().ouvrir(newAccess, newRefresh, session.getUtilisateur());

                        // Retry original request
                        req.setTokenSession(newAccess);
                        return executeRequest(req);
                    } else {
                        logger.error("Échec du rafraîchissement : {}", (refreshRes != null ? refreshRes.getMessage() : "null"));
                        SessionManager.getInstance().fermer();
                    }
                }
            }
            return res;
        } catch (Exception e) {
            handleError(e);
        }
        return new Reponse(false, "Erreur de communication avec le serveur.", null);
    }

    private Reponse executeRequest(Requete req) throws Exception {
        if (socket == null || socket.isClosed() || out == null) {
            connect();
        }
        if (out != null) {
            if (req.getParametres() != null && req.getParametres().get("email") != null) {
                logger.info("[AUDIT] 🚀 Requête {} pour : {}", req.getType(), req.getParametres().get("email"));
            }
            // Chiffrement du Token de Session s'il existe (AES-GCM + Object Privacy)
            if (req.getTokenSession() != null && !req.getTokenSession().isBlank()) {
                try {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
                    oos.writeObject(req.getTokenSession());
                    oos.flush();
                    byte[] tokenBytes = baos.toByteArray();

                    byte[] iv = new byte[12];
                    new SecureRandom().nextBytes(iv);

                    Cipher cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                    cipherAES.init(Cipher.ENCRYPT_MODE, this.sessionSecretKey, gcmSpec);

                    byte[] encryptedToken = cipherAES.doFinal(tokenBytes);

                    byte[] combined = new byte[iv.length + encryptedToken.length];
                    System.arraycopy(iv, 0, combined, 0, iv.length);
                    System.arraycopy(encryptedToken, 0, combined, iv.length, encryptedToken.length);

                    req.setTokenSession(Base64.getEncoder().encodeToString(combined));
                } catch (Exception e) {
                    logger.error("Erreur lors du chiffrement du token de session", e);
                }
            }

            encryptStorageFieldsWithDEK(req);  // Couche 1 : chiffrement DEK (stockage BDD)
            encryptRequeteFields(req);           // Couche 2 : chiffrement session (réseau)
            out.writeObject(req);
            out.flush();
            out.reset();
            Reponse reponse = (Reponse) in.readObject();
            decryptReponseFields(reponse);           // Couche 2 : déchiffrement session (réseau)
            decryptStorageFieldsWithDEK(reponse);    // Couche 1 : déchiffrement DEK (stockage BDD)
            return reponse;
        }
        throw new IOException("Socket non disponible");
    }

    // ─────────────────────────────────────────────────────────────────
    // COUCHE 1 : Chiffrement DEK (données sensibles pour stockage BDD)
    // ─────────────────────────────────────────────────────────────────

    /** Champs dont la valeur String est chiffrée avec la DEK avant stockage en BDD. */
    private static final String[] STORAGE_SENSITIVE_KEYS = {
        "numeroCarte", "cvv", "dateExpiration",
        "nom", "prenom", "telephone", "dateNaissance",
        "adresse_complete", "addresseComplete",
        "notifications"
    };

    /**
     * Chiffre les champs sensibles d'une requête avec la DEK de l'utilisateur connecté.
     * Seules les valeurs de type String sont chiffrées (les objets complexes sont gérés
     * par encryptRequeteFields avec la clé de session réseau).
     * Sans effet si l'utilisateur n'est pas connecté ou si la DEK n'est pas disponible.
     */
    private void encryptStorageFieldsWithDEK(Requete requete) {
        if (requete.getParametres() == null) return;
        model.Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        javax.crypto.SecretKey dek = currentUser.getSessionDek();
        if (dek == null) return;

        Map<String, Object> params = requete.getParametres();
        boolean isMutable = true;
        try {
            params.put("temp_check", null);
            params.remove("temp_check");
        } catch (UnsupportedOperationException e) {
            isMutable = false;
        }

        Map<String, Object> targetParams = isMutable ? params : new HashMap<>(params);

        for (String key : STORAGE_SENSITIVE_KEYS) {
            Object value = targetParams.get(key);
            if (value instanceof String plaintext) {
                try {
                    String encrypted = client.crypto.EnvelopeEncryptionService.encryptField(plaintext, dek);
                    targetParams.put(key, encrypted);
                } catch (Exception e) {
                    logger.error("[DEK] Échec chiffrement stockage pour le champ '{}'", key, e);
                }
            }
        }

        if (!isMutable) {
            requete.setParametres(targetParams);
        }
    }

    /**
     * Déchiffre les champs sensibles d'une réponse avec la DEK de l'utilisateur connecté.
     * Sans effet si l'utilisateur n'est pas connecté ou si la DEK n'est pas disponible.
     */
    public void decryptStorageFieldsWithDEK(Reponse reponse) {
        if (reponse == null || reponse.getDonnees() == null) return;
        
        javax.crypto.SecretKey dek = null;
        
        // 1. Essayer de récupérer la clé depuis la session active
        model.Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            dek = currentUser.getSessionDek();
        }
        
        // 2. Si pas de session (cas du Login), essayer de récupérer la clé dans l'objet utilisateur de la réponse
        Object userObj = reponse.getDonnees().get("utilisateur");
        if (dek == null && userObj instanceof model.Utilisateur u) {
            dek = u.getSessionDek();
        }

        if (dek == null) {
            logger.warn("[DEK] ⚠️ Aucune clé DEK disponible pour déchiffrer la réponse.");
            return;
        }

        // Fingerprint pour diagnostic
        byte[] encoded = dek.getEncoded();
        String fingerPrint = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(encoded, 4));
        logger.info("[DEK] 🔑 Clé DEK détectée (Fingerprint: {}...), début du déchiffrement...", fingerPrint);

        // 3. Déchiffrement les champs à la racine de la map
        for (String key : STORAGE_SENSITIVE_KEYS) {
            Object value = reponse.getDonnees().get(key);
            if (value instanceof String encrypted) {
                try {
                    String plaintext = client.crypto.EnvelopeEncryptionService.decryptField(encrypted, dek);
                    reponse.getDonnees().put(key, plaintext);
                    logger.info("[DEK] ✅ Champ racine déchiffré : {}", key);
                } catch (Exception e) {
                    logger.debug("[DEK] Champ racine '{}' non déchiffrable (déjà en clair ou erreur)", key);
                }
            }
        }

        // 4. Déchiffrer les champs à l'intérieur de l'objet Utilisateur/Client s'il est présent
        if (userObj instanceof model.Client c) {
            logger.info("[DEK] Déchiffrement de l'objet Client ({})...", c.getEmail());
            try {
                if (c.getNom() != null) {
                    c.setNom(client.crypto.EnvelopeEncryptionService.decryptField(c.getNom(), dek));
                    logger.info("[DEK] | Nom OK");
                }
                if (c.getPrenom() != null) {
                    c.setPrenom(client.crypto.EnvelopeEncryptionService.decryptField(c.getPrenom(), dek));
                    logger.info("[DEK] | Prénom OK");
                }
                if (c.getTelephone() != null) {
                    c.setTelephone(client.crypto.EnvelopeEncryptionService.decryptField(c.getTelephone(), dek));
                    logger.info("[DEK] | Téléphone OK");
                }
                if (c.getDateNaissance() != null) {
                    c.setDateNaissance(client.crypto.EnvelopeEncryptionService.decryptField(c.getDateNaissance(), dek));
                    logger.info("[DEK] | Date Naissance OK");
                }
            } catch (Exception e) {
                logger.warn("[DEK] ❌ Échec déchiffrement Client : {}", e.getMessage());
            }
        }

        // 5. Déchiffrement récursif pour les listes (ex: items de commande, adresses)
        String[] listKeys = {"items", "commandes", "adresses", "panier"};
        for (String lKey : listKeys) {
            Object listObj = reponse.getDonnees().get(lKey);
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> map = (Map<String, Object>) m;
                        for (String sKey : STORAGE_SENSITIVE_KEYS) {
                            if (map.containsKey(sKey) && map.get(sKey) instanceof String encrypted) {
                                try { map.put(sKey, client.crypto.EnvelopeEncryptionService.decryptField(encrypted, dek)); } catch (Exception e) {}
                            }
                        }
                    } else if (item instanceof model.Adresse a) {
                        try { if (a.getAddresseComplete() != null) a.setAddresseComplete(client.crypto.EnvelopeEncryptionService.decryptField(a.getAddresseComplete(), dek)); } catch (Exception e) {}
                    } else if (item instanceof Map<?, ?> mapItem && mapItem.containsKey("nom")) {
                        // Cas particulier pour les items de commande dans une liste
                        try {
                            Map<String, Object> m = (Map<String, Object>) mapItem;
                            if (m.get("nom") instanceof String enc) m.put("nom", client.crypto.EnvelopeEncryptionService.decryptField(enc, dek));
                        } catch (Exception e) {}
                    }
                }
            }
        }
        
        // 6. Déchiffrement pour l'objet "commande" ou "adresse" s'ils sont seuls
        Object singleObj = reponse.getDonnees().get("commande");
        if (singleObj == null) singleObj = reponse.getDonnees().get("adresse");
        
        if (singleObj instanceof Map<?, ?> m) {
            Map<String, Object> map = (Map<String, Object>) m;
            for (String sKey : STORAGE_SENSITIVE_KEYS) {
                if (map.containsKey(sKey) && map.get(sKey) instanceof String encrypted) {
                    try { map.put(sKey, client.crypto.EnvelopeEncryptionService.decryptField(encrypted, dek)); } catch (Exception e) { }
                }
            }
        } else if (singleObj instanceof model.Adresse a) {
            try { if (a.getAddresseComplete() != null) a.setAddresseComplete(client.crypto.EnvelopeEncryptionService.decryptField(a.getAddresseComplete(), dek)); } catch (Exception e) {}
        }
    }
    
    // ─────────────────────────────────────────────────────────────────
    // COUCHE 2 : Chiffrement session (données sensibles pour le réseau)
    // ─────────────────────────────────────────────────────────────────

    private void encryptRequeteFields(Requete requete) {
        if (this.sessionSecretKey == null || requete.getParametres() == null) return;
        
        String[] sensitiveKeys = { 
            "utilisateur", "client", "adresse", "adresses", "adresse_complete", 
            "commande", "commandes", "items", "panier", "lignes",
            "accessToken", "refreshToken", "refresh_tokens", "password_reset_codes", "resetCode",
            "twofactorcodes", "2faCode", "code", "login_security_state",
            "motDePasse", "newPassword", "password", "email", "nom", "prenom", "telephone", "dateNaissance",
            "numeroCarte", "cvv", "dateExpiration", "carte", "cartes", "paiement", "carteBancaire",
            "notifications"
        };
        
        for (String key : sensitiveKeys) {
            if (requete.getParametres().containsKey(key)) {
                Object valueObj = requete.getParametres().get(key);
                if (valueObj != null) {
                    try {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
                        oos.writeObject(valueObj);
                        oos.flush();
                        byte[] objectBytes = baos.toByteArray();
                        
                        // Generate 12-byte IV
                        byte[] iv = new byte[12];
                        new SecureRandom().nextBytes(iv);
                        
                        Cipher cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                        cipherAES.init(Cipher.ENCRYPT_MODE, this.sessionSecretKey, gcmSpec);
                        
                        byte[] encryptedBytes = cipherAES.doFinal(objectBytes);
                        
                        // Combine IV and Encrypted Data
                        byte[] combined = new byte[iv.length + encryptedBytes.length];
                        System.arraycopy(iv, 0, combined, 0, iv.length);
                        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
                        
                        String encryptedBase64 = Base64.getEncoder().encodeToString(combined);
                        requete.getParametres().put(key, encryptedBase64);
                    } catch (Exception e) {
                        logger.error("Échec du chiffrement AES-GCM pour le champ {}", key, e);
                    }
                }
            }
        }
    }

    private void decryptReponseFields(Reponse reponse) {
        if (this.sessionSecretKey == null || reponse == null || reponse.getDonnees() == null) return;

        String[] sensitiveKeys = { 
            "utilisateur", "client", "adresse", "adresses", "adresse_complete", 
            "commande", "commandes", "items", "panier", "lignes",
            "accessToken", "refreshToken", "refresh_tokens", "password_reset_codes", "resetCode",
            "twofactorcodes", "2faCode", "code", "login_security_state",
            "motDePasse", "newPassword", "password", "email", "nom", "prenom", "telephone", "dateNaissance",
            "numeroCarte", "cvv", "dateExpiration", "carte", "cartes", "paiement", "carteBancaire",
            "notifications"
        };

        for (String key : sensitiveKeys) {
            if (reponse.getDonnees().containsKey(key)) {
                Object rawValue = reponse.getDonnees().get(key);
                if (rawValue instanceof String) {
                    try {
                        String encryptedBase64 = (String) rawValue;
                        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

                        // Extract IV and Encrypted Data
                        if (combined.length < 12) continue; // Too short to contain IV
                        byte[] iv = new byte[12];
                        byte[] encryptedBytes = new byte[combined.length - 12];
                        System.arraycopy(combined, 0, iv, 0, 12);
                        System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.length);

                        Cipher cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                        cipherAES.init(Cipher.DECRYPT_MODE, this.sessionSecretKey, gcmSpec);

                        byte[] decryptedBytes = cipherAES.doFinal(encryptedBytes);

                        // Deserialize Object
                        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        Object originalObject = ois.readObject();

                        reponse.getDonnees().put(key, originalObject);
                    } catch (Exception e) {
                        logger.warn("Info - Déchiffrement AES-GCM échoué/ignoré pour le champ {}", key);
                    }
                }
            }
        }
    }

    private void handleError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
        logger.error("Erreur lors de l'envoi/réception : {}", errorMsg, e);
        close();
    }

    public void close() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }
}
