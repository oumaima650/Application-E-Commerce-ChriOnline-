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
        String query = "SELECT * FROM Produit";
        
        System.out.println("[ProduitDAO] Tentative de récupération des produits...");
        System.out.println("[ProduitDAO] Query: " + query);

        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            System.out.println("[ProduitDAO] Connexion BD réussie, exécution de la requête...");
            
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
            
            System.out.println("[ProduitDAO] " + produits.size() + " produits trouvés dans la base de données");
            
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur SQL: " + e.getMessage());
            System.err.println("[ProduitDAO] SQL State: " + e.getSQLState());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération de tous les produits", e);
        }
        return produits;

    }

    // Récupère un produit par son ID
    public Produit getById(int id) {

        String query = "SELECT * FROM Produit WHERE idProduit = ?"; // protege contre les injections SQL
        
        System.out.println("[ProduitDAO] Tentative de récupération du produit id=" + id + "...");
        System.out.println("[ProduitDAO] Query: " + query);

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

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

        String query = "SELECT * FROM Produit WHERE nom LIKE ?";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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

    // Ajoute un nouveau produit
    public boolean save(Produit produit) {

        String query = "INSERT INTO Produit (idCategorie, nom, description) VALUES (?, ?, ?)";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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


        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, produit.getIdCategorie());
            pstmt.setString(2, produit.getNom());        
            pstmt.setString(3, produit.getDescription()); 
            pstmt.setInt(4, produit.getIdProduit());      
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du produit id=" + produit.getIdProduit(), e);
        }
    }

    // Supprime un produit par son ID
    public boolean delete(int id) {

        String query = "DELETE FROM Produit WHERE idProduit = ?";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du produit id=" + id, e);
        }
    }

    // Compte le nombre total de produits
    public int count() {
        String query = "SELECT COUNT(*) FROM Produit";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des produits", e);
        }
        return 0;
    }

    /**
     * Récupère un produit complet avec toutes ses variantes et SKU
     * @param idProduit ID du produit
     * @return Map contenant les informations complètes du produit
     */
    public Map<String, Object> getProduitCompletAvecVariantes(int idProduit) {
        String query = "SELECT " +
                "p.idProduit, " +
                "p.nom AS nomProduit, " +
                "p.description, " +
                "v.nom AS nomVariante, " +
                "pvv.valeur, " +
                "s.SKU, " +
                "s.prix, " +
                "s.quantite, " +
                "s.image " +
                "FROM Produit p " +
                "JOIN ProduitVarValeur pvv ON p.idProduit = pvv.idProduit " +
                "JOIN Variante v ON pvv.idVariante = v.idVariante " +
                "JOIN SKUVarValeur svv ON pvv.idPVV = svv.idPVV " +
                "JOIN SKU s ON svv.SKU = s.SKU " +
                "WHERE p.idProduit = ?";
        
        System.out.println("[ProduitDAO] Récupération produit complet id=" + idProduit);
        
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
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
                    List<Map<String, Object>> skusList = new ArrayList<>(skusMap.values());
                    resultat.put("idProduit", idProduit);
                    resultat.put("nomProduit", nomProduit);
                    resultat.put("description", description);
                    resultat.put("variantes", variantesList);
                    resultat.put("skus", skusList);
                    
                    System.out.println("[ProduitDAO] Produit complet trouvé: " + nomProduit + 
                            " (" + variantesList.size() + " associations variantes, " + skusList.size() + " SKUs distincts)");
                    
                    return resultat;
                } else {
                    System.out.println("[ProduitDAO] Aucun produit trouvé avec id=" + idProduit);
                    return null;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[ProduitDAO] Erreur lors de la récupération du produit complet: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération du produit complet id=" + idProduit, e);
        }
    }

    // Convertit un résultat SQL en objet Produit
    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit prod = new Produit();
        prod.setIdProduit(rs.getInt("idProduit"));
        prod.setIdCategorie(rs.getInt("idCategorie"));
        prod.setNom(rs.getString("nom"));
        prod.setDescription(rs.getString("description"));
        prod.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());

        return prod;
    }
}