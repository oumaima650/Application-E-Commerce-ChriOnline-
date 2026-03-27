package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import model.Produit;

public class ProduitDAO {

    // Récupère tous les produits
    public List<Produit> getAll() {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT p.*, c.nom AS nomCategorie FROM Produit p " +
                       "LEFT JOIN Categorie c ON p.idCategorie = c.idCategorie " +
                       "WHERE p.deletedAt IS NULL";
        
        System.out.println("[ProduitDAO] Tentative de récupération des produits...");
        
        Connection conn = ConnexionBDD.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
            
            System.out.println("[ProduitDAO] " + produits.size() + " produits trouvés");
            
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de tous les produits", e);
        }
        return produits;
    }

    public List<Produit> getAllIncludeDeleted() {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT p.*, c.nom AS nomCategorie FROM Produit p " +
                       "LEFT JOIN Categorie c ON p.idCategorie = c.idCategorie";
        Connection conn = ConnexionBDD.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
            return produits;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de tous les produits (inc. supprimés)", e);
        }
    }

    // Récupère un produit par son ID
    public Produit getById(int id) {
        String query = "SELECT p.*, c.nom AS nomCategorie FROM Produit p " +
                       "LEFT JOIN Categorie c ON p.idCategorie = c.idCategorie " +
                       "WHERE p.idProduit = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du produit id=" + id, e);
        }
        return null;
    }

    // Recherche des produits par nom
    public List<Produit> getByNom(String nom) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT p.*, c.nom AS nomCategorie FROM Produit p " +
                       "LEFT JOIN Categorie c ON p.idCategorie = c.idCategorie " +
                       "WHERE p.nom LIKE ? AND p.deletedAt IS NULL";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + nom + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapResultSetToProduit(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par nom : " + nom, e);
        }
        return produits;
    }

    public List<Produit> getByNomIncludeDeleted(String nom) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT p.*, c.nom AS nomCategorie FROM Produit p " +
                       "LEFT JOIN Categorie c ON p.idCategorie = c.idCategorie " +
                       "WHERE p.nom LIKE ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + nom + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapResultSetToProduit(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par nom (inc. supprimés) : " + nom, e);
        }
        return produits;
    }

    // Ajoute un nouveau produit
    public boolean save(Produit produit) {

        String query = "INSERT INTO Produit (idCategorie, nom, description) VALUES (?, ?, ?)";

        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, produit.getIdCategorie());
            pstmt.setString(2, produit.getNom());
            pstmt.setString(3, produit.getDescription());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        produit.setIdProduit(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'insertion du produit : " + produit.getNom(), e);
        }
        return false;
    }

    // Met à jour un produit
    public boolean update(Produit produit) {

        String query = "UPDATE Produit SET idCategorie = ?, nom = ?, description = ? WHERE idProduit = ?";


        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, produit.getIdCategorie());
            pstmt.setString(2, produit.getNom());        
            pstmt.setString(3, produit.getDescription()); 
            pstmt.setInt(4, produit.getIdProduit());      
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du produit id=" + produit.getIdProduit(), e);
        }
    }

    public boolean delete(int id) {
        String query = "UPDATE Produit SET deletedAt = CURRENT_TIMESTAMP WHERE idProduit = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression logique du produit id=" + id, e);
        }
    }

    public boolean restore(int id) {
        String query = "UPDATE Produit SET deletedAt = NULL WHERE idProduit = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la restauration du produit id=" + id, e);
        }
    }

    public boolean physicalDelete(int id) {
        String query = "DELETE FROM Produit WHERE idProduit = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression physiq    ue du produit id=" + id, e);
        }
    }

    public int count() {
        String query = "SELECT COUNT(*) FROM Produit WHERE deletedAt IS NULL";
        Connection conn = ConnexionBDD.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des produits", e);
        }
        return 0;
    }

    public Map<String, Object> getProduitCompletAvecVariantes(int idProduit) {
        String query = "SELECT p.idProduit, p.nom AS nomProduit, p.description, v.nom AS nomVariante, " +
                "pvv.valeur, s.SKU, s.prix, s.quantite, s.image " +
                "FROM Produit p " +
                "JOIN ProduitVarValeur pvv ON p.idProduit = pvv.idProduit " +
                "JOIN Variante v ON pvv.idVariante = v.idVariante " +
                "JOIN SKUVarValeur svv ON pvv.idPVV = svv.idPVV " +
                "JOIN SKU s ON svv.SKU = s.SKU " +
                "WHERE p.idProduit = ? AND p.deletedAt IS NULL AND s.deletedAt IS NULL";
        
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idProduit);
            try (ResultSet rs = pstmt.executeQuery()) {
                Map<String, Object> resultat = new HashMap<>();
                List<Map<String, Object>> variantesList = new ArrayList<>();
                Map<String, Map<String, Object>> skusMap = new HashMap<>();
                
                String nomProduit = null;
                String description = null;
                
                while (rs.next()) {
                    if (nomProduit == null) {
                        nomProduit = rs.getString("nomProduit");
                        description = rs.getString("description");
                    }
                    
                    String nomVariante = rs.getString("nomVariante");
                    String valeur = rs.getString("valeur");
                    String skuCode = rs.getString("SKU");
                    
                    Map<String, Object> varianteObj = new HashMap<>();
                    varianteObj.put("nomVariante", nomVariante);
                    varianteObj.put("valeur", valeur);
                    variantesList.add(varianteObj);
                    
                    if (!skusMap.containsKey(skuCode)) {
                        Map<String, Object> sku = new HashMap<>();
                        sku.put("SKU", skuCode);
                        sku.put("prix", rs.getBigDecimal("prix"));
                        sku.put("quantite", rs.getInt("quantite"));
                        sku.put("image", rs.getString("image"));
                        sku.put("variantes", new HashMap<String, String>());
                        skusMap.put(skuCode, sku);
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, String> skuVariantes = (Map<String, String>) skusMap.get(skuCode).get("variantes");
                    skuVariantes.put(nomVariante, valeur);
                }
                
                if (nomProduit != null) {
                    resultat.put("idProduit", idProduit);
                    resultat.put("nomProduit", nomProduit);
                    resultat.put("description", description);
                    resultat.put("variantes", variantesList);
                    resultat.put("skus", new ArrayList<>(skusMap.values()));
                    return resultat;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du produit complet id=" + idProduit, e);
        }
        return null;
    }

    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit prod = new Produit();
        prod.setIdProduit(rs.getInt("idProduit"));
        prod.setIdCategorie(rs.getInt("idCategorie"));
        prod.setNom(rs.getString("nom"));
        prod.setDescription(rs.getString("description"));
        
        // Fetch category name if present in ResultSet
        try {
            prod.setNomCategorie(rs.getString("nomCategorie"));
        } catch (SQLException e) {
            // nomCategorie might not be in all queries
        }
        
        Timestamp ts = null;
        try { ts = rs.getTimestamp("createdAt"); } catch (SQLException e) {
            try { ts = rs.getTimestamp("created_At"); } catch (SQLException e2) {}
        }
        if (ts != null) prod.setCreatedAt(ts.toLocalDateTime());

        Timestamp deleted = rs.getTimestamp("deletedAt");
        if (deleted != null) prod.setDeletedAt(deleted.toLocalDateTime());

        return prod;
    }
}