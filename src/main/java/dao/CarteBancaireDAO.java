package dao;

import model.CarteBancaire;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import service.StorageEncryptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarteBancaireDAO {
    private static final Logger logger = LogManager.getLogger(CarteBancaireDAO.class);

    public boolean create(CarteBancaire carte) {
        String sql = "INSERT INTO carte_bancaire (IdClient, numeroCarte, typeCarte) VALUES (?, ?, ?)";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, carte.getIdClient());
            logger.debug("[CARD-SEC] Chiffrement du numéro de carte pour client ID: {}", carte.getIdClient());
            String encryptedNumero = StorageEncryptionService.getInstance().encrypt(carte.getNumeroCarte());
            ps.setString(2, encryptedNumero);
            ps.setString(3, carte.getTypeCarte());
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        carte.setIdCarte(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public CarteBancaire findById(int idCarte) {
        String sql = "SELECT * FROM Carte_bancaire WHERE idCarte = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCarte);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCarteBancaire(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<CarteBancaire> findByClient(int idClient) {
        List<CarteBancaire> cartes = new ArrayList<>();
        String sql = "SELECT * FROM Carte_bancaire WHERE IdClient = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cartes.add(mapResultSetToCarteBancaire(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cartes;
    }

    public boolean update(CarteBancaire carte) {
        String sql = "UPDATE Carte_bancaire SET numeroCarte = ?, typeCarte = ? WHERE idCarte = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String encryptedNumero = StorageEncryptionService.getInstance().encrypt(carte.getNumeroCarte());
            ps.setString(1, encryptedNumero);
            ps.setString(2, carte.getTypeCarte());
            ps.setInt(3, carte.getIdCarte());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int idCarte) {
        // La table Carte_bancaire n'a pas de deletedAt, donc c'est une suppression physique
        String sql = "DELETE FROM Carte_bancaire WHERE idCarte = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCarte);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private CarteBancaire mapResultSetToCarteBancaire(ResultSet rs) throws SQLException {
        CarteBancaire carte = new CarteBancaire();
        carte.setIdCarte(rs.getInt("idCarte"));
        carte.setIdClient(rs.getInt("IdClient"));
        String decryptedNumero = StorageEncryptionService.getInstance().decrypt(rs.getString("numeroCarte"));
        logger.debug("[CARD-SEC] Déchiffrement du numéro de carte réussi.");
        carte.setNumeroCarte(decryptedNumero);
        carte.setTypeCarte(rs.getString("typeCarte"));
        return carte;
    }
}
