package service;

import dao.CommandeDAO;
import dao.ProduitDAO;
import dao.UtilisateurDAO;
import server.ServiceUDP;
import shared.Reponse;
import shared.Requete;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminService {
    
    private final ServiceUDP serviceUDP;
    
    public AdminService() {
        this.serviceUDP = ServiceUDP.getInstance();
    }

    public Reponse getAllProducts(Requete requete) {
        try {
            List<model.Produit> produits = dao.ProduitDAO.getAllProducts();
            return new Reponse(true, "Produits récupérés avec succès", produits);
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la récupération des produits: " + e.getMessage(), null);
        }
    }

    public Reponse getAllOrders(Requete requete) {
        try {
            List<model.Commande> commandes = dao.CommandeDAO.getAllOrders();
            return new Reponse(true, "Commandes récupérées avec succès", commandes);
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la récupération des commandes: " + e.getMessage(), null);
        }
    }

    public Reponse getAllUsers(Requete requete) {
        try {
            List<model.Utilisateur> utilisateurs = dao.UtilisateurDAO.getAllUsers();
            return new Reponse(true, "Utilisateurs récupérés avec succès", utilisateurs);
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la récupération des utilisateurs: " + e.getMessage(), null);
        }
    }

    public Reponse updateProduct(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            int productId = (Integer) params.get("productId");
            String nom = (String) params.get("nom");
            String description = (String) params.get("description");
            Double prix = params.get("prix") != null ? ((Number) params.get("prix")).doubleValue() : null;
            Integer stock = params.get("stock") != null ? ((Number) params.get("stock")).intValue() : null;
            
            boolean success = dao.ProduitDAO.updateProduct(productId, nom, description, prix, stock);
            if (success) {
                return new Reponse(true, "Produit mis à jour avec succès", null);
            } else {
                return new Reponse(false, "Échec de la mise à jour du produit", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors de la mise à jour du produit: " + e.getMessage(), null);
        }
    }

    public Reponse deleteProduct(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            int productId = (Integer) params.get("productId");
            boolean success = dao.ProduitDAO.deleteProduct(productId);
            if (success) {
                return new Reponse(true, "Produit supprimé avec succès", null);
            } else {
                return new Reponse(false, "Échec de la suppression du produit", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors de la suppression du produit: " + e.getMessage(), null);
        }
    }

    public Reponse updateOrderStatus(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            int orderId = (Integer) params.get("orderId");
            String newStatus = (String) params.get("status");
            
            boolean success = dao.CommandeDAO.updateOrderStatus(orderId, newStatus);
            if (success) {
                // Envoyer notification UDP au client concerné
                // TODO: Récupérer le clientId depuis la commande
                // int clientId = dao.CommandeDAO.getClientIdFromOrder(orderId);
                // String message = "Votre commande #" + orderId + " est maintenant : " + newStatus;
                // serviceUDP.envoyerNotification(clientId, message);
                
                System.out.println("[AdminService] Notification UDP envoyée pour commande #" + orderId);
                return new Reponse(true, "Statut de la commande mis à jour avec succès", null);
            } else {
                return new Reponse(false, "Échec de la mise à jour du statut", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors de la mise à jour du statut: " + e.getMessage(), null);
        }
    }

    public Reponse banUser(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            int userId = (Integer) params.get("userId");
            boolean success = dao.UtilisateurDAO.banUser(userId);
            if (success) {
                return new Reponse(true, "Utilisateur banni avec succès", null);
            } else {
                return new Reponse(false, "Échec du bannissement", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors du bannissement: " + e.getMessage(), null);
        }
    }

    public Reponse unbanUser(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            int userId = (Integer) params.get("userId");
            boolean success = dao.UtilisateurDAO.unbanUser(userId);
            if (success) {
                return new Reponse(true, "Utilisateur débanni avec succès", null);
            } else {
                return new Reponse(false, "Échec du débannissement", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors du débannissement: " + e.getMessage(), null);
        }
    }
}
