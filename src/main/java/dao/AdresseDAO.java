package dao;

import model.Adresse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import service.StorageEncryptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdresseDAO {
    private static final Logger logger = LogManager.getLogger(AdresseDAO.class);

    public boolean create(Adresse adresse) {
        String sql = "INSERT INTO Adresse (IdClient, addresseComplete, ville, codePostal, createdAt) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, adresse.getIdClient());
            logger.debug("[ADDR-SEC] Chiffrement de l'adresse pour client ID: {}", adresse.getIdClient());
            String encryptedAddress = StorageEncryptionService.getInstance().encrypt(adresse.getAddresseComplete());
            ps.setString(2, encryptedAddress);
            ps.setString(3, adresse.getVille());
            ps.setString(4, adresse.getCodePostal());
            ps.setTimestamp(5, adresse.getCreatedAt() != null ? Timestamp.valueOf(adresse.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            
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
        String sql = "UPDATE Adresse SET addresseComplete = ?, ville = ?, codePostal = ? WHERE idAdresse = ? AND deletedAt IS NULL";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String encryptedAddress = StorageEncryptionService.getInstance().encrypt(adresse.getAddresseComplete());
            ps.setString(1, encryptedAddress);
            ps.setString(2, adresse.getVille());
            ps.setString(3, adresse.getCodePostal());
            ps.setInt(4, adresse.getIdAdresse());
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
        String decryptedAddress = StorageEncryptionService.getInstance().decrypt(rs.getString("addresseComplete"));
        logger.debug("[ADDR-SEC] Déchiffrement de l'adresse réussi.");
        adresse.setAddresseComplete(decryptedAddress);
        adresse.setVille(rs.getString("ville"));
        adresse.setCodePostal(rs.getString("codePostal"));
        if (rs.getTimestamp("createdAt") != null) {
            adresse.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
        }
        if (rs.getTimestamp("deletedAt") != null) {
            adresse.setDeletedAt(rs.getTimestamp("deletedAt").toLocalDateTime());
        }
        return adresse;
    }
}
