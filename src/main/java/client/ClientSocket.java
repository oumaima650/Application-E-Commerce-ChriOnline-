package client;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import shared.Session;
import client.utils.SessionManager;
import java.util.Map;
import java.util.HashMap;

import client.utils.SSLSocketFactoryBuilder;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Singleton managing the secure TCP connection to the server using TLS 1.3.
 */
public class ClientSocket {
    private static ClientSocket instance;
    private SSLSocket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
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
            SSLSocketFactory factory = SSLSocketFactoryBuilder.build();
            socket = (SSLSocket) factory.createSocket(HOST, PORT);
            
            // Enforce TLS 1.3 only
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});
            socket.startHandshake();

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("[ClientSocket] Connexion sécurisée établie via TLS 1.3 sur le port " + PORT);
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
                        System.err.println("[ClientSocket] Échec du rafraîchissement : " + (refreshRes != null ? refreshRes.getMessage() : "null"));
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
        System.err.println("[ClientSocket] Erreur lors de l'envoi/réception : " + errorMsg);
        e.printStackTrace();
        close();
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }
}
