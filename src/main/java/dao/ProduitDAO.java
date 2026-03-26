package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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

        String query = "INSERT INTO Produit (nom, description) VALUES (?, ?)";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getDescription());
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

        String query = "UPDATE Produit SET nom = ?, description = ? WHERE idProduit = ?";


        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, produit.getNom());        // 1er ? = nouveau nom
            pstmt.setString(2, produit.getDescription()); // 2eme ? = nouvelle description
            pstmt.setInt(3, produit.getIdProduit());      // 3eme ? = ID du produit à modifier
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

    // Convertit un résultat SQL en objet Produit
    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit prod = new Produit();
        prod.setIdProduit(rs.getInt("idProduit"));
        prod.setNom(rs.getString("nom"));
        prod.setDescription(rs.getString("description"));
        prod.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());

        return prod;

    }
}