package service;

import dao.CommandeDAO;
import dao.ProduitDAO;
import dao.UtilisateurDAO;
import model.Commande;
import model.LigneCommande;
import model.Produit;
import model.Utilisateur;
import model.enums.StatutCommande;
import server.ServiceUDP;
import shared.Reponse;
import shared.Requete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


public class AdminService {
    
    private final ServiceUDP serviceUDP;
    
    public AdminService() {
        this.serviceUDP = ServiceUDP.getInstance();
    }
/*
    public Reponse getAllProducts(Requete requete) {
        try {
            List<model.Produit> produits = dao.ProduitDAO.getAll();
            return new Reponse(true, "Produits récupérés avec succès", produits);
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la récupération des produits: " + e.getMessage(), null);
        }
    }
*/
    public Reponse getAllOrders(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            String queryArg = params != null ? (String) params.get("query") : null;
            dao.CommandeDAO commandeDAO = new dao.CommandeDAO();
            List<model.Commande> commandes = new java.util.ArrayList<>();
            
            if (queryArg == null || queryArg.trim().isEmpty()) {
                commandes = commandeDAO.getAdminOrders();
            } else {
                queryArg = queryArg.trim();
                model.Commande c = commandeDAO.findByReference(queryArg);
                if (c != null) {
                    commandes.add(c);
                } else {
                    try {
                        int idClient = Integer.parseInt(queryArg);
                        commandes = commandeDAO.findWithFilters(idClient, null, null);
                    } catch (NumberFormatException e) {
                    }
                }
            }
            
            List<Map<String, Object>> commandesData = new java.util.ArrayList<>();
            for (model.Commande c : commandes) {
                Map<String, Object> map = new java.util.HashMap<>();

                // ── Identifiants ──────────────────────────────────────────
                map.put("rawId",    c.getIdCommande());
                map.put("id",       c.getReference());          // référence
                map.put("idClient", c.getIdClient());           // ← AJOUT
                map.put("idAdresse", c.getIdAdresse());         // ← AJOUT (peut être null)
                
                // ── Dates ─────────────────────────────────────────────────
                String fmt = "dd/MM/yyyy HH:mm";
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(fmt);
 
                map.put("date",     c.getCreatedAt()             != null ? c.getCreatedAt().format(dtf)             : "N/A");
                map.put("updatedAt",c.getUpdatedAt()             != null ? c.getUpdatedAt().format(dtf)             : "N/A");
                map.put("dateLivraisonPrevue",  c.getDateLivraisonPrevue()  != null ? c.getDateLivraisonPrevue().format(dtf)  : "N/A");
                map.put("dateLivraisonReelle",  c.getDateLivraisonReelle()  != null ? c.getDateLivraisonReelle().format(dtf)  : "—");
                
                 // ── Adresse complète (jointure Adresse) ───────────────────
                if (c.getIdAdresse() != null) {
                    String adresse = getAdresseComplete(c.getIdAdresse());
                    map.put("adresseLivraison", adresse != null ? adresse : "Adresse inconnue");
                } else {
                    map.put("adresseLivraison", "Aucune adresse");
                }

                List<model.LigneCommande> lignes = commandeDAO.findLignesByCommandeId(c.getIdCommande());
                double total = 0;
                for (model.LigneCommande lc : lignes) {
                    total += lc.getPrixAchat() * lc.getQuantite();
                }
                map.put("total", String.format("%.2f MAD", total).replace(",", " "));
                
                map.put("total", String.format("%.2f MAD", total).replace(",", " "));
                map.put("statut", c.getStatut().name());
                commandesData.add(map);
            }
            
            return new Reponse(true, "Commandes récupérées avec succès", java.util.Map.of("commandes", commandesData));
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la récupération des commandes: " + e.getMessage(), null);
        }
    }

    public Reponse searchOrders(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        try {
            String queryArg = (String) params.get("query");
            dao.CommandeDAO commandeDAO = new dao.CommandeDAO();
            List<model.Commande> commandes = new java.util.ArrayList<>();
            
            if (queryArg == null || queryArg.trim().isEmpty()) {
                commandes = commandeDAO.getAdminOrders();
            } else {
                queryArg = queryArg.trim();
                model.Commande c = commandeDAO.findByReference(queryArg);
                if (c != null) {
                    commandes.add(c);
                } else {
                    try {
                        int idClient = Integer.parseInt(queryArg);
                        commandes = commandeDAO.findWithFilters(idClient, null, null);
                    } catch (NumberFormatException e) {
                        // Pas un ID client valide, la liste restera vide
                    }
                }
            }
            
            List<Map<String, Object>> commandesData = new java.util.ArrayList<>();
            for (model.Commande c : commandes) {
                Map<String, Object> map = new java.util.HashMap<>();

                map.put("rawId",    c.getIdCommande());
                map.put("id",       c.getReference());
                map.put("idClient", c.getIdClient());
                map.put("idAdresse", c.getIdAdresse());
                
                String fmt = "dd/MM/yyyy HH:mm";
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern(fmt);
 
                map.put("date",     c.getCreatedAt()             != null ? c.getCreatedAt().format(dtf)             : "N/A");
                map.put("updatedAt",c.getUpdatedAt()             != null ? c.getUpdatedAt().format(dtf)             : "N/A");
                map.put("dateLivraisonPrevue",  c.getDateLivraisonPrevue()  != null ? c.getDateLivraisonPrevue().format(dtf)  : "N/A");
                map.put("dateLivraisonReelle",  c.getDateLivraisonReelle()  != null ? c.getDateLivraisonReelle().format(dtf)  : "—");
                
                if (c.getIdAdresse() != null) {
                    String adresse = getAdresseComplete(c.getIdAdresse());
                    map.put("adresseLivraison", adresse != null ? adresse : "Adresse inconnue");
                } else {
                    map.put("adresseLivraison", "Aucune adresse");
                }

                List<model.LigneCommande> lignes = commandeDAO.findLignesByCommandeId(c.getIdCommande());
                double total = 0;
                for (model.LigneCommande lc : lignes) {
                    total += lc.getPrixAchat() * lc.getQuantite();
                }
                map.put("total", String.format("%.2f MAD", total).replace(",", " "));
                
                map.put("total", String.format("%.2f MAD", total).replace(",", " "));
                map.put("statut", c.getStatut().name());
                commandesData.add(map);
            }
            
            return new Reponse(true, "Recherche de commandes réussie", java.util.Map.of("commandes", commandesData));
        } catch (SQLException e) {
            return new Reponse(false, "Erreur lors de la recherche des commandes: " + e.getMessage(), null);
        }
    }
/*
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
*/

     /**
     * Récupère l'adresse complète (rue + ville + code postal) depuis la table Adresse
     */
    private String getAdresseComplete(int idAdresse) {
        String sql = "SELECT addresseComplete, ville, codePostal FROM Adresse WHERE idAdresse = ?";
        try (Connection conn = dao.ConnexionBDD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAdresse);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String rue      = rs.getString("addresseComplete");
                String ville    = rs.getString("ville");
                String cp       = rs.getString("codePostal");
                StringBuilder sb = new StringBuilder(rue);
                if (cp   != null && !cp.isBlank())    sb.append(", ").append(cp);
                if (ville != null && !ville.isBlank()) sb.append(" ").append(ville);
                return sb.toString();
            }
        } catch (SQLException e) {
            System.err.println("[AdminService] Erreur récupération adresse #" + idAdresse + ": " + e.getMessage());
        }
        return null;
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
                // Si la commande est marquée comme "Livrée", on met à jour la date de livraison réelle
                if (st == model.enums.StatutCommande.LIVREE) {
                    commandeDAO.setDateLivraisonReelle(orderId, java.time.LocalDateTime.now());
                }

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
/*
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
    */
}
