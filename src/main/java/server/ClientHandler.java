package server;

import service.AuthService;
import service.CarteBancaireService;
import service.CategorieService;
import service.NotificationService;
import service.PaiementService;
import service.ProduitService;
import service.ProduitVarValeurService;
import service.SKUService;
import service.VarianteService;
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
    private final CategorieService categorieService;
    private final NotificationService notificationService;
    private final PaiementService paiementService;
    private final ProduitService produitService;
    private final service.CommandeService commandeService;
    private final VarianteService varianteService;
    private final ProduitVarValeurService pvvService;
    private final SKUService skuService;
    private final service.ClientService clientService;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    public ClientHandler(Socket socket) {
        this.socket      = socket;
        this.authService = new AuthService();
        this.carteBancaireService = new CarteBancaireService();
        this.categorieService = new CategorieService();
        this.notificationService = new NotificationService();
        this.paiementService = new PaiementService();
        this.produitService = new ProduitService();
        this.commandeService = new service.CommandeService();
        this.varianteService = new VarianteService();
        this.pvvService = new ProduitVarValeurService();
        this.skuService = new SKUService();
        this.clientService = new service.ClientService();
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

                try {
                    Reponse reponse = dispatch(requete);
                    envoyer(reponse);
                } catch (Exception e) {
                    System.err.println("[ClientHandler] Erreur lors du dispatch : " + e.getMessage());
                    e.printStackTrace();
                    envoyer(new Reponse(false, "Erreur interne du serveur lors du traitement de la requête.", null));
                }

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
            case REGISTER -> authService.signup(requete);

            // ───────────────────────────────
            // PUBLIC OPERATIONS (No Auth)
            // ───────────────────────────────
            case GET_ALL_PRODUITS -> produitService.getAll(requete);
            case GET_PRODUIT_BY_ID -> produitService.getById(requete);
            case SEARCH_PRODUITS_BY_NOM -> produitService.rechercherParNom(requete);
            case COUNT_PRODUITS -> produitService.compter(requete);

            case GET_ALL_CATEGORIES -> categorieService.getAll(requete);
            case GET_CATEGORIE_BY_ID -> categorieService.getById(requete);
 
            // Variantes (Public)
            case GET_ALL_VARIANTES -> varianteService.getAll(requete);
            case GET_VARIANTES_BY_PRODUIT -> varianteService.getByProduit(requete);
            case GET_PVV_BY_PRODUIT -> pvvService.getByProduit(requete);
 
            // SKU Public
            case GET_ALL_SKUS -> skuService.getAll(requete);
            case GET_SKU_BY_PRODUIT -> skuService.getByProduit(requete);
            case GET_SKU_BY_CODE -> skuService.getBySku(requete);
 
            // Cart Operations
            case ADD_TO_CART, REMOVE_FROM_CART, GET_CART, CLEAR_CART, UPDATE_QUANTITY_CART -> {
                String token = requete.getTokenSession();
                int userId = AuthService.getUserIdFromToken(token);
                if ("DEBUG".equals(token)) userId = 7;
                if (userId <= 0) {
                    yield new Reponse(false, "Non authentifié. Veuillez vous connecter.", null);
                }

                if (requete.getParametres() == null) {
                    requete.setParametres(new java.util.HashMap<>());
                } else if (!(requete.getParametres() instanceof java.util.HashMap)) {
                    requete.setParametres(new java.util.HashMap<>(requete.getParametres()));
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
                if (token != null) userId = AuthService.getUserIdFromToken(token);

                if (userId <= 0) {
                    yield new Reponse(false, "Non authentifié. Veuillez vous connecter.", null);
                }

                if (requete.getParametres() == null) {
                    requete.setParametres(new java.util.HashMap<>());
                } else if (!(requete.getParametres() instanceof java.util.HashMap)) {
                    requete.setParametres(new java.util.HashMap<>(requete.getParametres()));
                }
                requete.getParametres().put("idClient", userId);
                requete.getParametres().put("idUtilisateur", userId);

                yield switch (requete.getType()) {
                    case ADD_CARD -> carteBancaireService.addCard(requete);
                    case GET_CARDS -> carteBancaireService.getCards(requete);
                    case REMOVE_CARD -> carteBancaireService.removeCard(requete);

                    case GET_NOTIFICATIONS -> notificationService.getNotifications(requete);
                    case MARK_NOTIFICATION_READ -> notificationService.markAsRead(requete);

                    case PROCESS_PAYMENT -> paiementService.processPayment(requete);

                    // ───────────────────────────────
                    // Commande
                    // ───────────────────────────────
                    
                    case VALIDATE_ORDER -> commandeService.passerCommande(requete);
                    case GET_ORDERS -> commandeService.getCommandesByClient(requete);
                    case GET_ORDER -> commandeService.getCommandeByReference(requete);
                    case GET_ORDERS_FILTERED -> commandeService.getCommandesFiltrees(requete);
                    case UPDATE_ORDER_STATUS -> commandeService.updateStatutCommande(requete);

                    // ───────────────────────────────
                    // Profil client & Adresses
                    // ───────────────────────────────
                    case GET_PROFILE  -> clientService.getProfile(requete);
                    case GET_ADDRESSES -> clientService.getAdresses(requete);
                    case ADD_ADDRESS   -> clientService.addAdresse(requete);


                    
                    // ───────────────────────────────
                    // PRODUIT
                    // ───────────────────────────────
                    case ADD_PRODUIT -> produitService.creer(requete);
                    case UPDATE_PRODUIT -> produitService.modifier(requete);
                    case DELETE_PRODUIT -> produitService.supprimer(requete);

                    // CATEGORIE (Admin/Write)
                    case ADD_CATEGORIE -> categorieService.creer(requete);
                    case UPDATE_CATEGORIE -> categorieService.modifier(requete);
                    case DELETE_CATEGORIE -> categorieService.supprimer(requete);
                    case ADD_VARIANTE_TO_CATEGORIE -> categorieService.ajouterVariante(requete);
                    case REMOVE_VARIANTE_FROM_CATEGORIE -> categorieService.retirerVariante(requete);
 
                    // Variantes CRUD
                    case ADD_VARIANTE -> varianteService.creer(requete);
                    case UPDATE_VARIANTE -> varianteService.modifier(requete);
                    case DELETE_VARIANTE -> varianteService.supprimer(requete);
 
                    // PVV (ProduitVarValeur) management
                    case ADD_PVV -> pvvService.creer(requete);
                    case DELETE_PVV -> pvvService.supprimer(requete);
 
                    // SKU Admin
                    case ADD_SKU -> skuService.creer(requete);
                    case UPDATE_SKU -> skuService.modifier(requete);
                    case DELETE_SKU -> skuService.supprimer(requete);
                    case ADD_VALUE_TO_SKU -> skuService.ajouterValeur(requete);
                    case REMOVE_VALUE_FROM_SKU -> skuService.retirerValeur(requete);
 
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