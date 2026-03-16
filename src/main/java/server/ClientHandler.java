package server;

import service.AuthService;
import shared.Reponse;
import shared.Requete;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuthService authService;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    public ClientHandler(Socket socket) {
        this.socket      = socket;
        this.authService = new AuthService();
    }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        System.out.println("[ClientHandler] Client connecté : " + clientAddr);

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                Requete requete;
                try {
                    requete = (Requete) in.readObject();
                } catch (Exception e) {
                    System.out.println("[ClientHandler] Client déconnecté : " + clientAddr);
                    break;
                }

                if (requete == null || requete.getType() == null) {
                    envoyer(new Reponse(false, "Requête invalide.", null));
                    continue;
                }

                System.out.println("[ClientHandler] Requête reçue : " + requete.getType() + " de " + clientAddr);

                Reponse reponse = dispatch(requete);
                envoyer(reponse);

                if (requete.getType() == shared.RequestType.LOGOUT) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("[ClientHandler] Erreur IO : " + e.getMessage());
        } finally {
            fermer();
        }
    }


    private Reponse dispatch(Requete requete) {
        return switch (requete.getType()) {
            case LOGIN  -> authService.login(requete);
            case LOGOUT -> authService.logout(requete);
            case REGISTER  -> authService.signup(requete);
            default -> {
                String token = requete.getTokenSession();
                if (!AuthService.isAuthenticated(token)) {
                    yield new Reponse(false, "Non authentifié. Veuillez vous connecter.", null);
                }
                // TODO: dispatch to other services (ProduitService, CommandeService…)
                yield new Reponse(false, "Fonctionnalité '" + requete.getType() + "' non encore implémentée.", null);
            }
        };
    }

    private void envoyer(Reponse reponse) {
        try {
            out.writeObject(reponse);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Impossible d'envoyer la réponse : " + e.getMessage());
        }
    }

    private void fermer() {
        try {
            if (out != null)    out.close();
            if (in  != null)    in.close();
            if (!socket.isClosed()) socket.close();
            System.out.println("[ClientHandler] Connexion fermée.");
        } catch (IOException e) {
            System.err.println("[ClientHandler] Erreur lors de la fermeture : " + e.getMessage());
        }
    }
}
