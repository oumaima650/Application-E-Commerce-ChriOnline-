package dao;

import model.Commande;
import model.enums.StatutCommande;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeDAO {

    public CommandeDAO() {
    }


    /**
     * Récupérer toutes les commandes d'un client
     */
    public List<Commande> findByClientId(int idClient) throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM Commande WHERE IdClient = ? ORDER BY created_At DESC";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idClient);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                commandes.add(mapResultSetToCommande(rs));
            }
        }
        return commandes;
    }


    /**
     * Récupérer une commande par sa référence
     */
    public Commande findByReference(String reference) throws SQLException {
        String query = "SELECT * FROM Commande WHERE reference = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, reference);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCommande(rs);
            }
        }
        return null;
    }


    /**
     * Récupérer une commande par son ID
     */
    public Commande findById(int idCommande) throws SQLException {
        String query = "SELECT * FROM Commande WHERE idCommande = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idCommande);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToCommande(rs);
            }
        }
        return null;
    }


    /**
     * Récupérer les commandes avec filtres
     */
    public List<Commande> findWithFilters(int idClient, String statutFilter, String dateFilter, String categorieFilter) throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        // Note: La jointure avec Produit/Categorie nécessite une structure SKU-Produit que nous simplifions ici si nécessaire.
        StringBuilder query = new StringBuilder("SELECT c.* FROM Commande c WHERE c.IdClient = ?");
        List<Object> params = new ArrayList<>();
        params.add(idClient);

        if (statutFilter != null && !statutFilter.isEmpty()) {
            query.append(" AND c.statut = ?");
            params.add(statutFilter);
        }

        if (dateFilter != null && !dateFilter.isEmpty()) {
            query.append(" AND DATE(c.created_At) = ?");
            params.add(dateFilter);
        }

        query.append(" ORDER BY c.created_At DESC");

        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                commandes.add(mapResultSetToCommande(rs));
            }
        }
        return commandes;
    }


    /**
     * Compter le nombre de commandes par statut
     */
    public int countByStatus(int idClient, StatutCommande statut) throws SQLException {
        String query = "SELECT COUNT(*) FROM Commande WHERE IdClient = ? AND statut = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idClient);
            stmt.setString(2, statut.name().toLowerCase()); // Assumer que le statut en DB est en minuscule
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }


    /**
     * Mapper un ResultSet vers un objet Commande
     */
    private Commande mapResultSetToCommande(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setIdCommande(rs.getInt("idCommande"));
        commande.setIdClient(rs.getInt("IdClient"));
        commande.setIdAdresse(rs.getObject("idAdresse", Integer.class));
        commande.setReference(rs.getString("reference"));
        
        // Gérer le statut ENUM (Supporte le minuscule accentué de la DB)
        String dbStatut = rs.getString("statut");
        if (dbStatut != null) {
            dbStatut = dbStatut.toLowerCase().trim();
            if (dbStatut.equals("en_attente") || dbStatut.equals("en attente")) {
                commande.setStatut(StatutCommande.EN_ATTENTE);
            } else if (dbStatut.equals("validée") || dbStatut.equals("validee")) {
                commande.setStatut(StatutCommande.VALIDEE);
            } else if (dbStatut.equals("expédiée") || dbStatut.equals("expediee")) {
                commande.setStatut(StatutCommande.EXPEDIEE);
            } else if (dbStatut.equals("livrée") || dbStatut.equals("livree")) {
                commande.setStatut(StatutCommande.LIVREE);
            } else {
                try {
                    commande.setStatut(StatutCommande.valueOf(dbStatut.toUpperCase()));
                } catch (Exception e) {
                    commande.setStatut(StatutCommande.EN_ATTENTE);
                }
            }
        } else {
            commande.setStatut(StatutCommande.EN_ATTENTE);
        }
        
        Timestamp dateLivraisonPrevue = rs.getTimestamp("dateLivraisonPrevue");
        if (dateLivraisonPrevue != null) {
            commande.setDateLivraisonPrevue(dateLivraisonPrevue.toLocalDateTime());
        }
        
        Timestamp dateLivraisonReelle = rs.getTimestamp("dateLivraisonReelle");
        if (dateLivraisonReelle != null) {
            commande.setDateLivraisonReelle(dateLivraisonReelle.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_At");
        if (createdAt != null) {
            commande.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_At");
        if (updatedAt != null) {
            commande.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return commande;
    }

    /**
     * Créer une nouvelle commande
     */
    public Commande create(Commande commande) throws SQLException {
        String query = "INSERT INTO Commande (IdClient, idAdresse, reference, statut, dateLivraisonPrevue, created_At, updated_At) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, commande.getIdClient());
            stmt.setObject(2, commande.getIdAdresse());
            stmt.setString(3, commande.getReference());
            stmt.setString(4, commande.getStatut().name().toUpperCase());
            stmt.setObject(5, commande.getDateLivraisonPrevue());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    commande.setIdCommande(generatedKeys.getInt(1));
                }
            }
        }
        return commande;
    }


    /**
     * Mettre à jour le statut d'une commande
     */
    public boolean updateStatus(int idCommande, StatutCommande nouveauStatut) throws SQLException {
        String query = "UPDATE Commande SET statut = ?, updated_At = ? WHERE idCommande = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, nouveauStatut.name().toUpperCase());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, idCommande);
            
            return stmt.executeUpdate() > 0;
        }
    }


    /**
     * Ajouter une ligne à une commande
     */
    public void addLigneCommande(int idCommande, String sku, int quantite, double prixUnit) throws SQLException {
        String query = "INSERT INTO LigneCommande (idCommande, SKU, quantite, prixAchat) VALUES (?, ?, ?, ?)";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idCommande);
            stmt.setString(2, sku);
            stmt.setInt(3, quantite);
            stmt.setDouble(4, prixUnit);
            stmt.executeUpdate();
        }
    }


    /**
     * Récupérer les lignes d'une commande
     */
    public List<model.LigneCommande> findLignesByCommandeId(int idCommande) throws SQLException {
        List<model.LigneCommande> lignes = new ArrayList<>();
        // Note: Jointure simplifiée pour récupérer au moins le SKU et le prix.
        // Le nom du produit sera récupéré si possible selon la structure existante.
        String query = "SELECT lc.* FROM LigneCommande lc WHERE lc.idCommande = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idCommande);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                model.LigneCommande ligne = new model.LigneCommande();
                ligne.setIdCommande(idCommande);
                ligne.setSku(rs.getString("SKU"));
                ligne.setQuantite(rs.getInt("quantite"));
                ligne.setPrixAchat(rs.getDouble("prixAchat"));
                // Par défaut on remet le SKU comme nom si on n'a pas la jointure Produit
                ligne.setNomProduit(rs.getString("SKU"));
                lignes.add(ligne);
            }
        }
        return lignes;
    }
    
    /**
     * Récupérer toutes les commandes pour l'administrateur (exclut celles 'en attente')
     */
    public List<Commande> getAdminOrders() throws SQLException {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM Commande WHERE statut != 'en_attente' ORDER BY created_At DESC";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                commandes.add(mapResultSetToCommande(rs));
            }
        }
        return commandes;
    }
    
    /**
     * Récupérer l'ID client d'une commande (crucial pour les notifications UDP)
     */
    public int getClientIdFromOrder(int orderId) throws SQLException {
        String query = "SELECT IdClient FROM Commande WHERE idCommande = ?";
        
        try (Connection connection = ConnexionBDD.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("IdClient");
            }
        }
        return -1; // Commande non trouvée
    }
}



