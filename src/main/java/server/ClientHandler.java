package server;

import service.AuthService;
import service.CarteBancaireService;
import service.NotificationService;
import service.PaiementService;
import shared.Reponse;
import shared.Requete;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuthService authService;
    private final CarteBancaireService carteBancaireService;
    private final NotificationService notificationService;
    private final PaiementService paiementService;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    public ClientHandler(Socket socket) {
        this.socket      = socket;
        this.authService = new AuthService();
        this.carteBancaireService = new CarteBancaireService();
        this.notificationService = new NotificationService();
        this.paiementService = new PaiementService();
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
            
            // Cart Operations
            case ADD_TO_CART, REMOVE_FROM_CART, GET_CART, CLEAR_CART, UPDATE_QUANTITY_CART -> {
                String token = requete.getTokenSession();
                int userId = AuthService.getUserIdFromToken(token);
                
                // --- DEBUG HACK for testing the Cart UI ---
                if ("DEBUG".equals(token)) userId = 7;
                
                if (userId <= 0) {
                    yield new Reponse(false, "Non authentifié. Veuillez vous connecter.", null);
                }
                
                // Inject the authenticated userId into the request parameters
                if (requete.getParametres() == null) {
                    requete.setParametres(new java.util.HashMap<>());
                }
                requete.getParametres().put("idClient", userId);
                
                service.PanierService panierService = new service.PanierService();
                
                yield switch (requete.getType()) {
                    case ADD_TO_CART -> panierService.ajouter(requete);
                    case REMOVE_FROM_CART -> panierService.supprimer(requete);
                    case CLEAR_CART -> panierService.vider(requete);
                    case GET_CART -> panierService.afficher(requete);
                    case UPDATE_QUANTITY_CART -> panierService.modifierQuantite(requete);
                    default -> new Reponse(false, "Opération panier non supportée.", null);
                };
            }
            
            default -> {
                String token = requete.getTokenSession();
                int userId = -1;
                
                if (token != null) {
                    userId = AuthService.getUserIdFromToken(token);
                }
                
                if (userId <= 0 && requete.getType() != shared.RequestType.LOGIN && requete.getType() != shared.RequestType.REGISTER) {
                    yield new Reponse(false, "Non authentifié. Veuillez vous connecter.", null);
                }
                
                if (requete.getParametres() == null) {
                    requete.setParametres(new java.util.HashMap<>());
                }
                
                // Injecter l'ID utilisateur sécurisé depuis le token pour les requêtes qui en ont besoin
                requete.getParametres().put("idClient", userId);
                requete.getParametres().put("idUtilisateur", userId);
                
                yield switch (requete.getType()) {
                    case ADD_CARD -> carteBancaireService.addCard(requete);
                    case GET_CARDS -> carteBancaireService.getCards(requete);
                    case REMOVE_CARD -> carteBancaireService.removeCard(requete);
                    
                    case GET_NOTIFICATIONS -> notificationService.getNotifications(requete);
                    case MARK_NOTIFICATION_READ -> notificationService.markAsRead(requete);
                    
                    case PROCESS_PAYMENT -> paiementService.processPayment(requete);
                    
                    default -> new Reponse(false, "Fonctionnalité '" + requete.getType() + "' non encore implémentée.", null);
                };
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
