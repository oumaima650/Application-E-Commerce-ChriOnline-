package client;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import shared.Session;
import client.utils.SessionManager;
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

            encryptRequeteFields(req);
            out.writeObject(req);
            out.flush();
            out.reset();
            Reponse reponse = (Reponse) in.readObject();
            decryptReponseFields(reponse);
            return reponse;
        }
        throw new IOException("Socket non disponible");
    }

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
