package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Variante;

public class VarianteDAO {

    public List<Variante> getAll() {
        List<Variante> variantes = new ArrayList<>();
        String query = "SELECT * FROM Variante";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                variantes.add(mapResultSetToVariante(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des variantes", e);
        }
        return variantes;
    }

    public Variante getById(int id) {
        String query = "SELECT * FROM Variante WHERE idVariante = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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

    public boolean save(Variante var) {
        String query = "INSERT INTO Variante (nom, description) VALUES (?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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

    public boolean update(Variante var) {
        String query = "UPDATE Variante SET nom = ?, description = ? WHERE idVariante = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, var.getNom());
            pstmt.setString(2, var.getDescription());
            pstmt.setInt(3, var.getIdVariante());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la variante", e);
        }
    }

    public boolean delete(int id) {
        String query = "DELETE FROM Variante WHERE idVariante = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
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
    try (Connection conn = ConnexionBDD.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
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
