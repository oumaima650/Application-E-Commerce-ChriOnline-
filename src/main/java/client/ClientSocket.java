package client;

import shared.Requete;
import shared.Reponse;

import java.io.*;
import java.net.Socket;

/**
 * Singleton managing the TCP connection to the server.
 */
public class ClientSocket {
    private static ClientSocket instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private static final String HOST = "localhost";
    private static final int PORT = 5555;

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
            System.out.println("[ClientSocket] Connecté au serveur " + HOST + ":" + PORT);
        } catch (IOException e) {
            System.err.println("[ClientSocket] Erreur de connexion : " + e.getMessage());
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
            System.err.println("[ClientSocket] Erreur lors de l'envoi/réception : " + e.getMessage());
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
