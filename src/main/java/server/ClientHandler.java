package server;

import service.AuthService;
import server.ServiceUDP;
import service.AdminService;
import service.CarteBancaireService;
import service.CategorieService;
import service.NotificationService;
import service.PaiementService;
import service.ProduitAffichableService;
import service.ProduitService;
import service.ProduitVarValeurService;
import service.SKUService;
import service.VarianteService;
import io.jsonwebtoken.ExpiredJwtException;
import service.JWTService;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.Map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuthService authService;
    private final AdminService adminService;
    private final ServiceUDP serviceUDP;
    private final CarteBancaireService carteBancaireService;
    private final CategorieService categorieService;
    private final NotificationService notificationService;
    private final PaiementService paiementService;
    private final ProduitService produitService;
    private final ProduitAffichableService produitAffichableService;
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
        this.adminService = new AdminService();
        this.serviceUDP = ServiceUDP.getInstance();
        this.carteBancaireService = new CarteBancaireService();
        this.categorieService = new CategorieService();
        this.notificationService = new NotificationService();
        this.paiementService = new PaiementService();
        this.produitService = new ProduitService();
        this.produitAffichableService = new ProduitAffichableService();
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
        RequestType type = requete.getType();
        int userId = -1;
        String userRole = "GUEST";
        String sessionId = null;

        // 1. PUBLIC ENDPOINTS (No Auth)
        if (type == RequestType.LOGIN || type == RequestType.REGISTER || type == RequestType.REFRESH) {
            return switch (type) {
                case LOGIN    -> authService.login(requete);
                case REGISTER -> authService.signup(requete);
                case REFRESH  -> authService.refresh(requete);
                default       -> new Reponse(false, "Internal Error", null);
            };
        }

        // 2. JWT VALIDATION for secure endpoints
        JWTService.TokenClaims claims = null;
        boolean isPublic = isPublicEndpoint(type);

        if (!isPublic) {
            try {
                String token = requete.getTokenSession();
                if (token == null || token.isBlank()) {
                    return new Reponse(false, "INVALID_TOKEN", null);
                }
                claims = JWTService.verifyAccessToken(token);
            } catch (ExpiredJwtException e) {
                return new Reponse(false, "TOKEN_EXPIRED", null);
            } catch (Exception e) {
                return new Reponse(false, "INVALID_TOKEN", null);
            }
        }

        // 3. SECURE CONTEXT SETUP
        if (requete.getParametres() == null) {
            requete.setParametres(new java.util.HashMap<>());
        } else if (!(requete.getParametres() instanceof java.util.HashMap)) {
            requete.setParametres(new java.util.HashMap<>(requete.getParametres()));
        }
        
        if (claims != null) {
            userId = Integer.parseInt(claims.userId());
            userRole = claims.role();
            sessionId = claims.sessionId();
            
            requete.getParametres().put("userId", userId);
            requete.getParametres().put("userRole", userRole);
            requete.getParametres().put("sessionId", sessionId);
        } else {
            // Guest mode
            requete.getParametres().put("userId", -1);
            requete.getParametres().put("userRole", "GUEST");
        }

        // 4. DISPATCH
        return switch (type) {
            case LOGOUT     -> authService.logout(requete);
            case LOGOUT_ALL -> authService.logoutAll(requete);

            // ───────────────────────────────
            // PUBLIC OPERATIONS (No Auth)
            // ───────────────────────────────
            case GET_ALL_PRODUITS -> produitService.getAll(requete);
            case GET_ALL_PRODUITS_AFFICHABLES -> {
                System.out.println("[ClientHandler] Traitement GET_ALL_PRODUITS_AFFICHABLES...");
                Reponse response = produitAffichableService.getAll(requete);
                System.out.println("[ClientHandler] Réponse GET_ALL_PRODUITS_AFFICHABLES: " + (response.isSucces() ? "SUCCÈS" : "ÉCHEC"));
                if (response.isSucces() && response.getDonnees() != null) {
                    Object produitsObj = response.getDonnees().get("produits");
                    if (produitsObj instanceof List) {
                        List<?> produits = (List<?>) produitsObj;
                        System.out.println("[ClientHandler] Nombre de produits retournés: " + produits.size());
                    }
                }
                yield response;
            }
            case GET_PRODUIT_BY_ID -> produitService.getById(requete);
            case GET_PRODUIT_COMPLET_AVEC_VARIANTES -> produitService.getProduitCompletAvecVariantes(requete);
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
            case GET_SKU_BY_VARIANTS -> skuService.getByVariants(requete);
            case GET_PRODUCT_VARIANTS -> pvvService.getByProduit(requete);
            case REGISTER_UDP_PORT -> {
                // Enregistrer le port UDP du client pour les notifications
                Map<String, Object> params = requete.getParametres();
                if (params != null && params.containsKey("udpPort")) {
                    int udpPort = (Integer) params.get("udpPort");
                    String clientIp = socket.getInetAddress().getHostAddress();
                    
                    serviceUDP.registerClient(  userId, clientIp, udpPort);
                    yield new Reponse(true, "Port UDP enregistré avec succès", null);
                } else {
                    yield new Reponse(false, "Port UDP manquant dans la requête", null);
                }
            }
 
            case ADD_TO_CART, REMOVE_FROM_CART, GET_CART, CLEAR_CART, UPDATE_QUANTITY_CART -> {
                // userId is already injected into requete.getParametres() in the JWT validation step above
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
                // userId is already injected into requete.getParametres() in the JWT validation step above
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
                    
                    // Admin operations
                    //case ADMIN_GET_ALL_PRODUCTS -> adminService.getAllProducts(requete);
                    case ADMIN_GET_ALL_ORDERS -> adminService.getAllOrders(requete);
                    case ADMIN_GET_ALL_USERS -> adminService.getAllUsers(requete);
                    //case ADMIN_UPDATE_PRODUCT -> adminService.updateProduct(requete);
                    //case ADMIN_DELETE_PRODUCT -> adminService.deleteProduct(requete);
                    case ADMIN_UPDATE_ORDER_STATUS -> adminService.updateOrderStatus(requete);
                    case ADMIN_SEARCH_ORDERS -> adminService.searchOrders(requete);
                    case ADMIN_BAN_USER -> adminService.banUser(requete);
                    case ADMIN_UNBAN_USER -> adminService.unbanUser(requete);
                    

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

    private boolean isPublicEndpoint(RequestType type) {
        return type == RequestType.GET_ALL_PRODUITS ||
               type == RequestType.GET_ALL_PRODUITS_AFFICHABLES ||
               type == RequestType.GET_PRODUIT_BY_ID ||
               type == RequestType.GET_PRODUIT_COMPLET_AVEC_VARIANTES ||
               type == RequestType.SEARCH_PRODUITS_BY_NOM ||
               type == RequestType.COUNT_PRODUITS ||
               type == RequestType.GET_ALL_CATEGORIES ||
               type == RequestType.GET_CATEGORIE_BY_ID ||
               type == RequestType.GET_ALL_VARIANTES ||
               type == RequestType.GET_VARIANTES_BY_PRODUIT ||
               type == RequestType.GET_PVV_BY_PRODUIT ||
               type == RequestType.GET_ALL_SKUS ||
               type == RequestType.GET_SKU_BY_PRODUIT ||
               type == RequestType.GET_SKU_BY_CODE ||
               type == RequestType.GET_SKU_BY_VARIANTS ||
               type == RequestType.GET_PRODUCT_VARIANTS;
    }
}