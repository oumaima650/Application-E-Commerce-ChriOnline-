package client;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import shared.Session;
import client.utils.SessionManager;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Singleton managing the secure TCP connection to the server using TLS 1.3.
 */
public class ClientSocket {
    private static final Logger logger = LogManager.getLogger(ClientSocket.class);
    private static ClientSocket instance;
    private SSLSocket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private final Properties config = new Properties();

    private ClientSocket() {
        loadConfig();
        connect();
    }

    private void loadConfig() {
        try (FileInputStream input = new FileInputStream(CONFIG_PATH)) {
            config.load(input);
        } catch (Exception e) {
            logger.error("[ClientSocket] Erreur chargement config : {}", e.getMessage());
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
            String host = "127.0.0.1";
            int port = Integer.parseInt(config.getProperty("server.port", "8443"));

            SSLSocketFactory ssf = getSSLSocketFactory();
            socket = (SSLSocket) ssf.createSocket(host, port);
            
            // Forcer TLS 1.3
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            logger.info("[ClientSocket] Connexion sécurisée TLS 1.3 établie sur le port {}", port);
        } catch (Exception e) {
            logger.error("[ClientSocket] Erreur de connexion sécurisée TLS : {}", e.getMessage());
            socket = null;
        }
    }

    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        String truststorePath = config.getProperty("client.truststore.path");
        String truststorePass = config.getProperty("client.truststore.password");
        String truststoreType = config.getProperty("client.truststore.type", "PKCS12");

        KeyStore ts = KeyStore.getInstance(truststoreType);
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            ts.load(fis, truststorePass.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
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

            // 3. HANDLE TOKEN EXPIRY
            if (res != null && "TOKEN_EXPIRED".equals(res.getMessage())) {
                logger.info("[ClientSocket] Access token expiré. Tentative de rafraîchissement...");

                if (session != null && session.getRefreshToken() != null) {
                    // Send Refresh Request
                    Requete refreshReq = new Requete(RequestType.REFRESH, new HashMap<>(), session.getRefreshToken());
                    Reponse refreshRes = executeRequest(refreshReq);

                    if (refreshRes != null && refreshRes.isSucces()) {
                        String newAccess = (String) refreshRes.getDonnees().get("accessToken");
                        String newRefresh = (String) refreshRes.getDonnees().get("refreshToken");

                        logger.info("[ClientSocket] Rafraîchissement réussi.");
                        SessionManager.getInstance().ouvrir(newAccess, newRefresh, session.getUtilisateur());

                        // Retry original request
                        req.setTokenSession(newAccess);
                        return executeRequest(req);
                    } else {
                        logger.error("[ClientSocket] Échec du rafraîchissement : {}", 
                                (refreshRes != null ? refreshRes.getMessage() : "null"));
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
            out.writeObject(req);
            out.flush();
            out.reset();
            return (Reponse) in.readObject();
        }
        throw new IOException("Socket non disponible");
    }

    private void handleError(Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
        logger.error("[ClientSocket] Erreur lors de l'envoi/réception : {}", errorMsg);
        close();
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("[ClientSocket] Erreur lors de la fermeture : {}", e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }
}
