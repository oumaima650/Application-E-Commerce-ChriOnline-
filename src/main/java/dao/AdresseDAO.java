package dao;

import model.Adresse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdresseDAO {

    public boolean create(Adresse adresse) {
        String sql = "INSERT INTO Adresse (IdClient, addresseComplete, ville, createdAt) VALUES (?, ?, ?, ?)";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, adresse.getIdClient());
            ps.setString(2, adresse.getAddresseComplete());
            ps.setString(3, adresse.getVille());
            ps.setTimestamp(4, adresse.getCreatedAt() != null ? Timestamp.valueOf(adresse.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        adresse.setIdAdresse(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Adresse findById(int idAdresse) {
        String sql = "SELECT * FROM Adresse WHERE idAdresse = ? AND deletedAt IS NULL";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAdresse);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAdresse(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Adresse> findByClient(int idClient) {
        List<Adresse> adresses = new ArrayList<>();
        String sql = "SELECT * FROM Adresse WHERE IdClient = ? AND deletedAt IS NULL";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    adresses.add(mapResultSetToAdresse(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adresses;
    }

    public boolean update(Adresse adresse) {
        String sql = "UPDATE Adresse SET addresseComplete = ?, ville = ? WHERE idAdresse = ? AND deletedAt IS NULL";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, adresse.getAddresseComplete());
            ps.setString(2, adresse.getVille());
            ps.setInt(3, adresse.getIdAdresse());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int idAdresse) {
        // Soft delete since deletedAt is in schema
        String sql = "UPDATE Adresse SET deletedAt = CURRENT_TIMESTAMP WHERE idAdresse = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAdresse);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Adresse mapResultSetToAdresse(ResultSet rs) throws SQLException {
        Adresse adresse = new Adresse();
        adresse.setIdAdresse(rs.getInt("idAdresse"));
        adresse.setIdClient(rs.getInt("IdClient"));
        adresse.setAddresseComplete(rs.getString("addresseComplete"));
        adresse.setVille(rs.getString("ville"));
        if (rs.getTimestamp("createdAt") != null) {
            adresse.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        }
        if (rs.getTimestamp("deletedAt") != null) {
            adresse.setDeletedAt(rs.getTimestamp("deletedAt").toLocalDateTime());
        }
        return adresse;
    }
}
