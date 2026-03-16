package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServeurTCP {

    private static final int PORT = 5555;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  Serveur TCP ChriOnline — port " + PORT);
        System.out.println("═══════════════════════════════════════════");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[ServeurTCP] En attente de connexions...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[ServeurTCP] Nouvelle connexion : "
                        + clientSocket.getInetAddress().getHostAddress());

                Thread thread = new Thread(new ClientHandler(clientSocket));
                thread.setDaemon(true);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("[ServeurTCP] Erreur fatale : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
