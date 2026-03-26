package client;

import shared.Requete;
import shared.Reponse;

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
        try {
            if (socket == null || socket.isClosed() || out == null) {
                connect();
            }
            
            if (out != null) {
                out.writeObject(req);
                out.flush();
                out.reset();
                
                return (Reponse) in.readObject();
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            System.err.println("[ClientSocket] Erreur lors de l'envoi/réception : " + errorMsg);
            e.printStackTrace();
            // Attempt to reconnect for the next request
            close();
        }
        return new Reponse(false, "Erreur de communication avec le serveur.", null);
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
