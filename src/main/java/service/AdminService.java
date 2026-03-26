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
            dao.CommandeDAO commandeDAO = new dao.CommandeDAO();
            List<model.Commande> commandes = commandeDAO.getAdminOrders();
            
            List<Map<String, Object>> commandesData = new java.util.ArrayList<>();
            for (model.Commande c : commandes) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("rawId", c.getIdCommande());
                map.put("id", c.getReference());
                map.put("client", "Client #" + c.getIdClient());
                
                String date = c.getCreatedAt() != null ? c.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
                map.put("date", date);
                
                List<model.LigneCommande> lignes = commandeDAO.findLignesByCommandeId(c.getIdCommande());
                double total = 0;
                for (model.LigneCommande lc : lignes) {
                    total += lc.getPrixAchat() * lc.getQuantite();
                }
                map.put("total", String.format("%.2f MAD", total).replace(",", " "));
                
                String statut = c.getStatut().name();
                if (c.getStatut() == model.enums.StatutCommande.VALIDEE) statut = "Validée";
                if (c.getStatut() == model.enums.StatutCommande.EXPEDIEE) statut = "Expédiée";
                if (c.getStatut() == model.enums.StatutCommande.LIVREE) statut = "Livrée";
                
                map.put("statut", statut);
                commandesData.add(map);
            }
            
            return new Reponse(true, "Commandes récupérées avec succès", java.util.Map.of("commandes", commandesData));
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
            String newStatusStr = (String) params.get("status");
            
            // Convertir la chaîne "Expédiée", "Validée", etc en Enum
            model.enums.StatutCommande st = model.enums.StatutCommande.VALIDEE;
            if (newStatusStr.equalsIgnoreCase("Expédiée") || newStatusStr.equalsIgnoreCase("Expediee")) st = model.enums.StatutCommande.EXPEDIEE;
            else if (newStatusStr.equalsIgnoreCase("Livrée") || newStatusStr.equalsIgnoreCase("Livree")) st = model.enums.StatutCommande.LIVREE;
            
            dao.CommandeDAO commandeDAO = new dao.CommandeDAO();
            boolean success = commandeDAO.updateStatus(orderId, st);
            
            if (success) {
                // Envoyer notification UDP au client concerné
                int clientId = commandeDAO.getClientIdFromOrder(orderId);
                if (clientId > 0) {
                    String message = "Votre commande #" + orderId + " est maintenant : " + newStatusStr;
                    serviceUDP.envoyerNotification(clientId, message);
                    System.out.println("[AdminService] Notification UDP envoyée au client " + clientId + " pour commande #" + orderId);
                } else {
                    System.err.println("[AdminService] Impossible de trouver le client pour la commande #" + orderId);
                }
                System.out.println("[AdminService] Statut de la commande #" + orderId + " MAJ vers " + st.name());
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
