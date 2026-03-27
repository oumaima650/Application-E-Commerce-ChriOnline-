package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Variante;

public class VarianteDAO {

    // Récupère toutes les variantes
    public List<Variante> getAll() {
        List<Variante> variantes = new ArrayList<>();
        String query = "SELECT * FROM Variante";
        Connection conn = ConnexionBDD.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                variantes.add(mapResultSetToVariante(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des variantes", e);
        }
        return variantes;
    }

    // Récupère une variante par son ID
    public Variante getById(int id) {
        String query = "SELECT * FROM Variante WHERE idVariante = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVariante(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de la variante id=" + id, e);
        }
        return null;
    }

    // Enregistre une nouvelle variante
    public boolean save(Variante var) {
        String query = "INSERT INTO Variante (nom, description) VALUES (?, ?)";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, var.getNom());
            pstmt.setString(2, var.getDescription());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        var.setIdVariante(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la variante", e);
        }
        return false;
    }

    // Met à jour une variante
    public boolean update(Variante var) {
        String query = "UPDATE Variante SET nom = ?, description = ? WHERE idVariante = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, var.getNom());
            pstmt.setString(2, var.getDescription());
            pstmt.setInt(3, var.getIdVariante());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la variante", e);
        }
    }

    // Supprime une variante
    public boolean delete(int id) {
        String query = "DELETE FROM Variante WHERE idVariante = ?";
        Connection conn = ConnexionBDD.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la variante", e);
        }
    }
   
public List<Variante> getByCategorie(int idCategorie) {
    List<Variante> variantes = new ArrayList<>();
    String query = "SELECT v.* FROM Variante v " +
                   "JOIN CategorieVariante cv ON v.idVariante = cv.idVariante " +
                   "WHERE cv.idCategorie = ?";
    Connection conn = ConnexionBDD.getConnection();
    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setInt(1, idCategorie);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                variantes.add(mapResultSetToVariante(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Erreur récupération variantes par catégorie", e);
    }
    return variantes;
}
 
public List<Variante> getByProduit(int idProduit) {
    List<Variante> variantes = new ArrayList<>();
    String query = "SELECT DISTINCT v.* FROM Variante v " +
                   "JOIN ProduitVarValeur pvv ON v.idVariante = pvv.idVariante " +
                   "WHERE pvv.idProduit = ?";
    Connection conn = ConnexionBDD.getConnection();
    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setInt(1, idProduit);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                variantes.add(mapResultSetToVariante(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Erreur récupération variantes par produit", e);
    }
    return variantes;
}

    // Convertit un résultat SQL en objet Variante
    private Variante mapResultSetToVariante(ResultSet rs) throws SQLException {
        Variante var = new Variante();
        var.setIdVariante(rs.getInt("idVariante"));
        var.setNom(rs.getString("nom"));
        var.setDescription(rs.getString("description"));
        var.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());
        var.setUpdatedAt(rs.getTimestamp("updated_At").toLocalDateTime());
        return var;
    }
}
