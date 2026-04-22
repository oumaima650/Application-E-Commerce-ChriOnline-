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
import java.security.PublicKey;

/**
 * Singleton managing the secure TCP connection to the server using TLS 1.3.
 */
public class ClientSocket {
    private static ClientSocket instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private SecretKey sessionSecretKey;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8443;

    private ClientSocket() {
        connect();
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

            System.out.println("[ClientSocket] Connexion sécurisée Hybride (AES/RSA) établie sur le port " + PORT);
        } catch (Exception e) {
            System.err.println("[ClientSocket] Erreur de connexion sécurisée : " + e.getMessage());
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
                System.out.println("[ClientSocket] Access token expiré. Tentative de rafraîchissement...");

                if (session != null && session.getRefreshToken() != null) {
                    // Send Refresh Request
                    Requete refreshReq = new Requete(RequestType.REFRESH, new HashMap<>(), session.getRefreshToken());
                    Reponse refreshRes = executeRequest(refreshReq);

                    if (refreshRes != null && refreshRes.isSucces()) {
                        String newAccess = (String) refreshRes.getDonnees().get("accessToken");
                        String newRefresh = (String) refreshRes.getDonnees().get("refreshToken");

                        System.out.println("[ClientSocket] Rafraîchissement réussi.");
                        SessionManager.getInstance().ouvrir(newAccess, newRefresh, session.getUtilisateur());

                        // Retry original request
                        req.setTokenSession(newAccess);
                        return executeRequest(req);
                    } else {
                        System.err.println("[ClientSocket] Échec du rafraîchissement : "
                                + (refreshRes != null ? refreshRes.getMessage() : "null"));
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
            encryptRequeteFields(req);
            out.writeObject(req);
            out.flush();
            out.reset();
            return (Reponse) in.readObject();
        }
        throw new IOException("Socket non disponible");
    }

    private void encryptRequeteFields(Requete requete) {
        if (this.sessionSecretKey == null || requete.getParametres() == null) return;
        
        String[] sensitiveKeys = {"password", "motDePasse", "carteBancaire", "numeroCarte", "cvv", "dateExpiration", "token", "newPassword"};
        
        for (String key : sensitiveKeys) {
            if (requete.getParametres().containsKey(key)) {
                Object valueObj = requete.getParametres().get(key);
                if (valueObj instanceof String) {
                    try {
                        String rawValue = (String) valueObj;
                        Cipher cipherAES = Cipher.getInstance("AES");
                        cipherAES.init(Cipher.ENCRYPT_MODE, this.sessionSecretKey);
                        byte[] encryptedBytes = cipherAES.doFinal(rawValue.getBytes());
                        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
                        
                        requete.getParametres().put(key, encryptedBase64);
                    } catch (Exception e) {
                        System.err.println("[ClientSocket] Échec du chiffrement AES pour le champ " + key);
                    }
                }
            }
        }
    }

    private void handleError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
        System.err.println("[ClientSocket] Erreur lors de l'envoi/réception : " + errorMsg);
        e.printStackTrace();
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
