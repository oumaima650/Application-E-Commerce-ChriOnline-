package server;

import service.AuthService;
import server.ServeurUDP;
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
import service.SecurityManager;

import java.util.Map;
//pour RSA/AES
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
//

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler implements Runnable {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    private final Socket socket;
    private final AuthService authService;
    private final AdminService adminService;
    private final ServeurUDP serveurUDP;
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
    private final service.AvisService avisService;
    private final SecurityManager securityManager;
    // pourRSA/AES
    private final PrivateKey serverPrivateKey;
    private final PublicKey serverPublicKey;
    private SecretKey sessionSecretKey;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, SecurityManager securityManager, PrivateKey serverPrivateKey,
            PublicKey serverPublicKey) {
        this.socket = socket;
        this.securityManager = securityManager;
        // pour RSA/AES
        this.serverPrivateKey = serverPrivateKey;
        this.serverPublicKey = serverPublicKey;
        // [WHITELIST IP ADMIN] On passe l'instance de SecurityManager à AuthService
        // pour qu'il puisse vérifier l'IP lors du login admin (réutilisation de la
        // whitelist déjà chargée)
        this.authService = new AuthService(securityManager.getLoginAttemptService(), securityManager);

        this.adminService = new AdminService();
        this.serveurUDP = ServeurUDP.getInstance();
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
        this.avisService = new service.AvisService();
    }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        logger.info("Client connecté : {}", clientAddr);

        try {
            // SECURITY: Timeout d'inactivité de 5 minutes (300 000 ms)
            // Si le client ne fait aucune requête pendant 5 min, le socket lancera une
            // exception
            // et la connexion sera fermée pour libérer une place dans le pool global.
            socket.setSoTimeout(300000);

            out = new ObjectOutputStream(socket.getOutputStream());

            out.flush();

            // PROTECTION: ObjectInputFilter (Whitelist approach)
            // Allows only safe classes required for the application's protocol.
            // Documentation:
            // https://docs.oracle.com/en/java/javase/21/core/serialization-filtering1.html
            java.io.ObjectInputFilter filter = java.io.ObjectInputFilter.Config.createFilter(
                    "shared.**;" +
                            "model.**;" +
                            "java.lang.**;" +
                            "java.util.**;" +
                            "java.time.**;" +
                            "java.security.**;" +
                            "[B;" +
                            "!*" // Reject all other classes
            );

            in = new ObjectInputStream(socket.getInputStream());
            in.setObjectInputFilter(filter);

            // DEBUT HANDSHAKE RSA/AES
            try {
                // 1. Envoyer cle publique du serveur au client
                out.writeObject(this.serverPublicKey);
                out.flush();

                // 2. Recevoir cle AES generee par le client, chiffree avec cle publique
                byte[] aesKeyChiffree = (byte[]) in.readObject();

                // 3. Dechiffrer avec cle privee du serveur
                Cipher cipherRSA = Cipher.getInstance("RSA");
                cipherRSA.init(Cipher.DECRYPT_MODE, this.serverPrivateKey);
                byte[] aesKeyEnClair = cipherRSA.doFinal(aesKeyChiffree);

                // 4. Construire objet SecretKey AES
                this.sessionSecretKey = new SecretKeySpec(aesKeyEnClair, 0, aesKeyEnClair.length, "AES");

                logger.info("Handshake RSA/AES réussi avec {}", clientAddr);
            } catch (Exception e) {
                logger.error("Échec du Handshake avec {}", clientAddr, e);
                fermer();
                return;
            }
            // FIN HANDSHAKE

            while (!socket.isClosed()) {
                Requete requete;
                try {
                    requete = (Requete) in.readObject();
                } catch (Exception e) {
                    logger.info("Client déconnecté : {}", clientAddr);
                    break;
                }

                if (requete == null || requete.getType() == null) {
                    envoyer(new Reponse(false, "Requête invalide.", null));
                    continue;
                }

                logger.info("Requête reçue : {} de {}", requete.getType(), clientAddr);

                // --- SECURITY CHECK (Centralisé via SecurityManager) ---
                Reponse securityError = securityManager.validateRequest(requete, clientAddr);
                if (securityError != null) {
                    logger.warn("Requête BLOQUÉE par la sécurité pour {}", clientAddr);
                    envoyer(securityError);
                    continue; // On arrête là pour cette requête
                }

                // --- DECHIFFREMENT CIBLE DE LA REQUETE ---
                decryptRequeteFields(requete);

                try {
                    Reponse reponse = dispatch(requete);
                    
                    // --- CHIFFREMENT CIBLE DE LA REPONSE ---
                    encryptReponseFields(reponse);

                    envoyer(reponse);
                } catch (Exception e) {
                    logger.error("Erreur lors du dispatch", e);
                    envoyer(new Reponse(false, "Erreur interne du serveur lors du traitement de la requête.", null));
                }

                if (requete.getType() == shared.RequestType.LOGOUT) {
                    break;
                }
            }

        } catch (java.net.SocketTimeoutException e) {
            logger.info("Déconnexion automatique : Inactivité détectée (5 min) pour {}", clientAddr);
        } catch (IOException e) {
            logger.error("Erreur IO", e);
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
        if (type == RequestType.LOGIN || type == RequestType.REGISTER || type == RequestType.REFRESH ||
                type == RequestType.REQUEST_PASSWORD_RESET || type == RequestType.CONFIRM_PASSWORD_RESET ||
                type == RequestType.VERIFY_2FA_LOGIN || type == RequestType.VERIFY_SIGNUP) {

            // Inject client context (IP) for security
            if (requete.getParametres() == null) {
                requete.setParametres(new java.util.HashMap<>());
            }
            requete.getParametres().put("clientIp", socket.getInetAddress().getHostAddress());

            return switch (type) {
                case LOGIN -> authService.login(requete);
                case REGISTER -> authService.signup(requete);
                case REFRESH -> authService.refresh(requete);
                case REQUEST_PASSWORD_RESET -> authService.handleRequestReset(requete);
                case CONFIRM_PASSWORD_RESET -> authService.handleConfirmReset(requete);
                case VERIFY_2FA_LOGIN -> authService.handleVerify2FALogin(requete);
                case VERIFY_SIGNUP -> authService.handleVerifySignup(requete);
                default -> new Reponse(false, "Internal Error", null);
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
            case LOGOUT -> authService.logout(requete);
            case LOGOUT_ALL -> authService.logoutAll(requete);

            // 2FA (Secure)
            case GENERATE_2FA_CODE -> authService.handleGenerate2FACode(requete);
            case TOGGLE_2FA -> authService.handleToggle2FA(requete);

            // ───────────────────────────────
            // PUBLIC OPERATIONS (No Auth)
            // ───────────────────────────────
            case GET_ALL_PRODUITS -> produitService.getAll(requete);
            case GET_ALL_PRODUITS_AFFICHABLES -> {
                logger.debug("Traitement GET_ALL_PRODUITS_AFFICHABLES...");
                Reponse response = produitAffichableService.getAll(requete);
                logger.debug("Réponse GET_ALL_PRODUITS_AFFICHABLES: {}", (response.isSucces() ? "SUCCÈS" : "ÉCHEC"));
                if (response.isSucces() && response.getDonnees() != null) {
                    Object produitsObj = response.getDonnees().get("produits");
                    if (produitsObj instanceof List) {
                        List<?> produits = (List<?>) produitsObj;
                        logger.debug("Nombre de produits retournés: {}", produits.size());
                    }
                }
                yield response;
            }
            case GET_PRODUIT_BY_ID -> produitService.getById(requete);
            case GET_PRODUIT_COMPLET_AVEC_VARIANTES -> produitService.getProduitCompletAvecVariantes(requete);
            case SEARCH_PRODUITS_BY_NOM -> produitService.rechercherParNom(requete);
            case COUNT_PRODUITS -> produitService.compter(requete);
            case GET_AVIS_BY_PRODUIT -> avisService.getAvisByProduit(requete);

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

                    serveurUDP.registerClient(userId, clientIp, udpPort);
                    yield new Reponse(true, "Port UDP enregistré avec succès", null);
                } else {
                    yield new Reponse(false, "Port UDP manquant dans la requête", null);
                }
            }

            case ADD_TO_CART, REMOVE_FROM_CART, GET_CART, CLEAR_CART, UPDATE_QUANTITY_CART -> {
                // userId is already injected into requete.getParametres() in the JWT validation
                // step above
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
                // userId is already injected into requete.getParametres() in the JWT validation
                // step above
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
                    case ADMIN_GET_ALL_PRODUCTS -> produitService.adminGetAll(requete);
                    case ADMIN_GET_ALL_ORDERS -> adminService.getAllOrders(requete);
                    case ADMIN_GET_ALL_USERS -> adminService.getAllClients(requete);
                    case ADMIN_UPDATE_PRODUCT -> produitService.adminUpdateProduct(requete);
                    case ADMIN_DELETE_PRODUCT -> produitService.supprimer(requete);
                    case ADMIN_UPDATE_ORDER_STATUS -> adminService.updateOrderStatus(requete);
                    case ADMIN_SEARCH_ORDERS -> adminService.searchOrders(requete);
                    case ADMIN_BAN_USER -> adminService.unbanUser(requete);
                    case ADMIN_UNBAN_USER -> adminService.unbanUser(requete);
                    case ADMIN_GET_SKU_BY_PRODUIT -> skuService.adminGetByProduit(requete);
                    case ADMIN_ADD_PRODUCT_COMPLET -> produitService.creerProduitComplet(requete);
                    case ADMIN_GET_VARIANTES_BY_CATEGORIE -> categorieService.getVariantes(requete);

                    // ───────────────────────────────
                    // Commande
                    // ───────────────────────────────

                    case VALIDATE_ORDER -> {
                        logger.debug("VALIDATE_ORDER reçu par ClientHandler");
                        Reponse res = commandeService.passerCommande(requete);
                        if (res.getDonnees() != null) {
                            logger.debug("VALIDATE_ORDER Données retournées : {}", res.getDonnees().keySet());
                            if (res.getDonnees().containsKey("items")) {
                                List<?> items = (List<?>) res.getDonnees().get("items");
                                logger.debug("Nombre d'items: {}", (items != null ? items.size() : 0));
                            }
                            logger.debug("Total: {}", res.getDonnees().get("total"));
                        }
                        yield res;
                    }
                    case GET_ORDERS -> commandeService.getCommandesByClient(requete);
                    case GET_ORDER -> commandeService.getCommandeByReference(requete);
                    case GET_ORDERS_FILTERED -> commandeService.getCommandesFiltrees(requete);
                    case UPDATE_ORDER_STATUS -> commandeService.updateStatutCommande(requete);

                    // ───────────────────────────────
                    // Profil client & Adresses
                    // ───────────────────────────────
                    case GET_PROFILE -> clientService.getProfile(requete);
                    case GET_ADDRESSES -> clientService.getAdresses(requete);
                    case ADD_ADDRESS -> clientService.addAdresse(requete);

                    case ADD_AVIS -> avisService.addAvis(requete);
                    case GET_USER_AVIS_FOR_PRODUCT -> avisService.getUserAvisForProduct(requete);
                    case UPDATE_AVIS -> avisService.updateAvis(requete);

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

                    default ->
                        new Reponse(false, "Fonctionnalité '" + requete.getType() + "' non encore implémentée.", null);
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
            logger.error("Impossible d'envoyer la réponse", e);
        }
    }

    private void fermer() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (!socket.isClosed())
                socket.close();
            logger.info("Connexion fermée.");
        } catch (IOException e) {
            logger.error("Erreur lors de la fermeture", e);
        }
    }

    private boolean isPublicEndpoint(RequestType type) {
        return type == RequestType.LOGIN ||
                type == RequestType.REGISTER ||
                type == RequestType.REFRESH ||
                type == RequestType.REQUEST_PASSWORD_RESET ||
                type == RequestType.CONFIRM_PASSWORD_RESET ||
                type == RequestType.GET_ALL_PRODUITS ||
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
    //

    private void decryptRequeteFields(Requete requete) {
        if (this.sessionSecretKey == null || requete.getParametres() == null)
            return;

        // Liste des champs considérés sensibles que le client va chiffrer (étendus PII)
        String[] sensitiveKeys = { "motDePasse", "password", "newPassword", "numeroCarte", "cvv", "dateExpiration",
                "carteBancaire", "email", "nom", "prenom", "telephone", "dateNaissance", "rue", "ville", "codePostal", "pays", "code" , "commandes" , "panier", "items"};

        for (String key : sensitiveKeys) {
            if (requete.getParametres().containsKey(key)) {
                Object valueObj = requete.getParametres().get(key);
                if (valueObj instanceof String) {
                    try {
                        String encryptedBase64 = (String) valueObj;
                        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

                        if (combined.length < 12) continue;
                        byte[] iv = new byte[12];
                        byte[] encryptedBytes = new byte[combined.length - 12];
                        System.arraycopy(combined, 0, iv, 0, 12);
                        System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.length);

                        Cipher cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                        cipherAES.init(Cipher.DECRYPT_MODE, this.sessionSecretKey, gcmSpec);

                        byte[] decryptedBytes = cipherAES.doFinal(encryptedBytes);

                        // Deserialize Object
                        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decryptedBytes);
                        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
                        Object originalObject = ois.readObject();

                        // Remplace donnee chiffree par donnee en clair pour la suite du flux
                        requete.getParametres().put(key, originalObject);
                    } catch (Exception e) {
                        logger.warn("Info - Déchiffrement AES-GCM échoué/ignoré pour {}", key, e);
                    }
                }
            }
        }

        // Déchiffrement en profondeur du jeton JWT de session (Vulnérabilité Hijacking)
        if (requete.getTokenSession() != null && !requete.getTokenSession().isBlank()) {
            try {
                String encryptedBase64 = requete.getTokenSession();
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.DECRYPT_MODE, this.sessionSecretKey);
                byte[] decodedBase64 = Base64.getDecoder().decode(encryptedBase64);
                byte[] decrypted = cipherAES.doFinal(decodedBase64);
                
                requete.setTokenSession(new String(decrypted));
            } catch (Exception e) {
                // Silencieux (Le client n'a peut-être pas encore activé le chiffrement du JWT)
            }
        }
    }

    private void encryptReponseFields(Reponse reponse) {
        if (this.sessionSecretKey == null || reponse == null || reponse.getDonnees() == null) return;

        String[] sensitiveKeys = {"accessToken", "refreshToken", "utilisateur", "commandes", "adresses", "adresse", "historique_commandes", "profil", "paiement", "motDePasse", "password", "newPassword", "email", "nom", "prenom", "telephone", "dateNaissance", "rue", "ville", "codePostal", "pays", "code"};

        for (String key : sensitiveKeys) {
            if (reponse.getDonnees().containsKey(key)) {
                Object rawValue = reponse.getDonnees().get(key);
                if (rawValue != null) {
                    try {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
                        oos.writeObject(rawValue);
                        oos.flush();
                        byte[] objectBytes = baos.toByteArray();

                        byte[] iv = new byte[12];
                        new SecureRandom().nextBytes(iv);

                        Cipher cipherAES = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                        cipherAES.init(Cipher.ENCRYPT_MODE, this.sessionSecretKey, gcmSpec);

                        byte[] encryptedBytes = cipherAES.doFinal(objectBytes);

                        byte[] combined = new byte[iv.length + encryptedBytes.length];
                        System.arraycopy(iv, 0, combined, 0, iv.length);
                        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

                        String encryptedBase64 = java.util.Base64.getEncoder().encodeToString(combined);

                        reponse.getDonnees().put(key, encryptedBase64);
                    } catch (Exception e) {
                        logger.warn("Info - Chiffrement AES-GCM échoué pour {}", key, e);
                    }
                }
            }
        }
    }
}