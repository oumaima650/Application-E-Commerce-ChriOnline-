package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Categorie;
import model.Variante;

public class CategorieDAO {

    // Récupère toutes les catégories
    public List<Categorie> getAll() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM Categorie";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des catégories", e);
        }
        return categories;
    }

    // Récupère une catégorie par son ID
    public Categorie getById(int id) {
        String query = "SELECT * FROM Categorie WHERE idCategorie = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategorie(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de la catégorie id=" + id, e);
        }
        return null;
    }

    // Enregistre une nouvelle catégorie
    public boolean save(Categorie cat) {
        String query = "INSERT INTO Categorie (nom, description) VALUES (?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, cat.getNom());
            pstmt.setString(2, cat.getDescription());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cat.setIdCategorie(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la catégorie", e);
        }
        return false;
    }

    // Met à jour une catégorie
    public boolean update(Categorie cat) {
        String query = "UPDATE Categorie SET nom = ?, description = ? WHERE idCategorie = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, cat.getNom());
            pstmt.setString(2, cat.getDescription());
            pstmt.setInt(3, cat.getIdCategorie());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la catégorie", e);
        }
    }

    // Supprime une catégorie
    public boolean delete(int id) {
        String query = "DELETE FROM Categorie WHERE idCategorie = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la catégorie", e);
        }
    }

    // Lie une variante à une catégorie
    public boolean addVariante(int idCategorie, int idVariante) {
        String query = "INSERT INTO CategorieVariante (idCategorie, idVariante) VALUES (?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idCategorie);
            pstmt.setInt(2, idVariante);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout d'une variante à la catégorie", e);
        }
    }

    // Retire le lien entre une variante et une catégorie
    public boolean removeVariante(int idCategorie, int idVariante) {
        String query = "DELETE FROM CategorieVariante WHERE idCategorie = ? AND idVariante = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idCategorie);
            pstmt.setInt(2, idVariante);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression d'une variante de la catégorie", e);
        }
    }

    // Convertit un résultat SQL en objet Categorie
    private Categorie mapResultSetToCategorie(ResultSet rs) throws SQLException {
        Categorie cat = new Categorie();
        cat.setIdCategorie(rs.getInt("idCategorie"));
        cat.setNom(rs.getString("nom"));
        cat.setDescription(rs.getString("description"));
        cat.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());
        cat.setUpdatedAt(rs.getTimestamp("updated_At").toLocalDateTime());
        return cat;
    }
}
