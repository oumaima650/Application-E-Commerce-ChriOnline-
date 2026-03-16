package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Categorie;

public class CategorieDAO {
    private Connection connection;

    public CategorieDAO() {
        this.connection = ConnexionBDD.getConnection();
    }

    public List<Categorie> getAll() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM Categorie";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public Categorie getById(int id) {
        String query = "SELECT * FROM Categorie WHERE idCategorie = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategorie(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean save(Categorie categorie) {
        String query = "INSERT INTO Categorie (nom, description) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categorie.getNom());
            pstmt.setString(2, categorie.getDescription());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        categorie.setIdCategorie(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Categorie categorie) {
        String query = "UPDATE Categorie SET nom = ?, description = ? WHERE idCategorie = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, categorie.getNom());
            pstmt.setString(2, categorie.getDescription());
            pstmt.setInt(3, categorie.getIdCategorie());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String query = "DELETE FROM Categorie WHERE idCategorie = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

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
