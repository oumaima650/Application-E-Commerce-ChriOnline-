package dao;

import model.Administrateur;
import model.Client;
import model.Utilisateur;
import model.enums.TypeEtulisateur;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO {

    private static Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    public record LoginData(int id, String hash, boolean twoFactorEnabled, String salt, String wrappedDek) {}

    /**
     * Gets the user ID and password hash for a given email.
     * @return LoginData or null if user doesn't exist.
     */
    public static LoginData getLoginData(String email) throws SQLException {
        String sql = "SELECT IdUtilisateur, motDePasse, two_factor_enabled, encryption_salt, wrapped_dek FROM Utilisateur WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LoginData(rs.getInt(1), rs.getString(2), rs.getBoolean(3), rs.getString(4), rs.getString(5));
                }
            }
        }
        return null;
    }

    /**
     * @deprecated Use PasswordService.verify with getLoginData instead.
     */
    @Deprecated
    public static int verifyLogInInformations(String email, String motDePasse) throws SQLException {
        // ... kept for temporary compatibility if needed
        return -1; 
    }


    public static boolean userExist(String email) throws SQLException {
        String getInfoReauete = "SELECT IdUtilisateur FROM Utilisateur WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(getInfoReauete)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static TypeEtulisateur userType(int idUtilisateur) throws SQLException {
        String getInfoRequete = "SELECT * FROM Client WHERE IdUtilisateur = ?";
        try (PreparedStatement ps = getConn().prepareStatement(getInfoRequete)) {
            ps.setInt(1, idUtilisateur);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return TypeEtulisateur.CLIENT;
                } else {
                    // hadi password ghalet
                    return TypeEtulisateur.ADMIN;
                }
            }
        }
    }

    // Récupère uniquement les clients pour l'admin
    public static List<model.Utilisateur> getAllUsers() throws SQLException {
        List<model.Utilisateur> list = new ArrayList<>();
        list.addAll(ClientDAO.getAllClients());
        return list;
    }

    // L'implémentation de banUser/unbanUser a été migrée vers ClientDAO.java

    public static Utilisateur findById(int id) throws SQLException {
        if (userType(id) == TypeEtulisateur.CLIENT) {
            return new ClientDAO().findById(id);
        } else {
            String sql = "SELECT email, motDePasse, encryption_salt, wrapped_dek, two_factor_enabled, createdAt, updatedAt FROM Utilisateur WHERE IdUtilisateur = ?";
            try (Connection conn = getConn();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                        LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                        return new Administrateur(id, rs.getString("email"), rs.getString("motDePasse"), rs.getString("encryption_salt"), rs.getString("wrapped_dek"), rs.getBoolean("two_factor_enabled"), ca, ua);
                    }
                }
            }
        }
        return null;
    }
    public static List<Integer> getAdminsIds() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT IdUtilisateur FROM Admin";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /**
     * Updates the password for a user with the given email.
     * @param email The user's email.
     * @param newHash The new hashed password.
     * @return true if updated, false otherwise.
     */
    public static boolean updatePassword(String email, String newHash) throws SQLException {
        String sql = "UPDATE Utilisateur SET motDePasse = ?, updatedAt = NOW() WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Met à jour le hash du mot de passe ET les données de chiffrement (sel + DEK wrappée)
     * en une seule transaction. À utiliser lors du reset de mot de passe pour garantir
     * la cohérence entre le nouveau KEK (dérivé du nouveau mot de passe) et la DEK.
     */
    public static boolean updatePasswordAndEncryption(String email, String newHash,
                                                       String newEncryptionSalt,
                                                       String newWrappedDek) throws SQLException {
        String sql = "UPDATE Utilisateur SET motDePasse = ?, encryption_salt = ?, wrapped_dek = ?, updatedAt = NOW() WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newEncryptionSalt);
            ps.setString(3, newWrappedDek);
            ps.setString(4, email);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean updatePasswordAndEncryption(int userId, String newHash,
                                                       String newEncryptionSalt,
                                                       String newWrappedDek) throws SQLException {
        String sql = "UPDATE Utilisateur SET motDePasse = ?, encryption_salt = ?, wrapped_dek = ?, updatedAt = NOW() WHERE IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newEncryptionSalt);
            ps.setString(3, newWrappedDek);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean updateTwoFactorStatus(String email, boolean enabled) throws SQLException {
        String sql = "UPDATE Utilisateur SET two_factor_enabled = ?, updatedAt = NOW() WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        }
    }

    public static void delete(int id) throws SQLException {
        String sql = "DELETE FROM Utilisateur WHERE IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes all Utilisateur records whose corresponding Client account
     * has an EN_ATTENTE status and was created more than {@code hours} hours ago.
     * The ON DELETE CASCADE on the FK ensures that the Client row is also removed.
     *
     * @param hours Number of hours after which a pending account is considered abandoned.
     * @return Number of accounts deleted.
     */
    public static int deleteOldPendingAccounts(int hours) throws SQLException {
        String sql = """
                DELETE u FROM Utilisateur u
                INNER JOIN Client c ON c.IdUtilisateur = u.IdUtilisateur
                WHERE c.statut = 'EN_ATTENTE'
                  AND u.createdAt < NOW() - INTERVAL ? HOUR
                """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hours);
            return ps.executeUpdate();
        }
    }
}
