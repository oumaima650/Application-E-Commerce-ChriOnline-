package dao;

import model.Client;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class ClientDAO {
    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    public Client create(String email, String motDePasse, String salt, String wrappedDek, String nom, String prenom, String telephone, String dateNaissance) throws SQLException {
        Connection conn = getConn();
        conn.setAutoCommit(false);
        int idClient = 0;
        try {
            String sqlUser = "INSERT INTO Utilisateur (email, motDePasse, encryption_salt, wrapped_dek) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, motDePasse);
                ps.setString(3, salt);
                ps.setString(4, wrappedDek);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) idClient = keys.getInt(1);
                }
            }

            String sqlClient = "INSERT INTO Client (IdUtilisateur, nom, prenom, telephone, dateNaissance) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlClient)) {
                ps.setInt(1, idClient);
                ps.setString(2, nom);
                ps.setString(3, prenom);
                ps.setString(4, telephone);
                ps.setString(5, dateNaissance);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[ClientDAO] Created with the id : " + idClient);

            boolean twoFactorEnabled = false;
            LocalDateTime createdAt = null;
            LocalDateTime updatedAt = null;
            String sqlFetch = "SELECT two_factor_enabled, createdAt, updatedAt FROM Utilisateur WHERE IdUtilisateur = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlFetch)) {
                ps.setInt(1, idClient);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        twoFactorEnabled = rs.getBoolean("two_factor_enabled");
                        Timestamp ca = rs.getTimestamp("createdAt");
                        Timestamp ua = rs.getTimestamp("updatedAt");
                        if (ca != null) createdAt = ca.toLocalDateTime();
                        if (ua != null) updatedAt = ua.toLocalDateTime();
                    }
                }
            }
            return new Client(idClient, email, motDePasse, salt, wrappedDek, twoFactorEnabled, createdAt, updatedAt, nom, prenom, telephone, dateNaissance, null);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static boolean isTelephoneExist(String telephone) throws SQLException {
        String sql = "SELECT 1 FROM Client WHERE telephone = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, telephone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Client findById(int id) throws SQLException {
        String sql = "SELECT u.email, u.motDePasse, u.encryption_salt, u.wrapped_dek, u.two_factor_enabled, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.statut, c.dateNaissance " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur " +
                     "WHERE u.IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    String dob = rs.getString("dateNaissance");
                    
                    Client c = new Client(id, rs.getString("email"), rs.getString("motDePasse"), rs.getString("encryption_salt"), rs.getString("wrapped_dek"), rs.getBoolean("two_factor_enabled"), ca, ua,
                                      rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), dob, null);
                    c.setStatut(rs.getString("statut"));
                    return c;
                }
            }
        }
        return null;
    }

    public static boolean banUser(int userId) throws SQLException {
        String query = "UPDATE Client SET statut = 'BANNI', deletedAt = CURRENT_TIMESTAMP WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            System.out.println("[ClientDAO] Ban result for user " + userId + ": " + rows + " rows affected");
            return rows > 0;
        }
    }

    public static boolean unbanUser(int userId) throws SQLException {
        String query = "UPDATE Client SET statut = 'ACTIF', deletedAt = NULL WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            System.out.println("[ClientDAO] Unban result for user " + userId + ": " + rows + " rows affected");
            return rows > 0;
        }
    }

    public static List<Client> getAllClients() throws SQLException {
        return searchClients(null);
    }

    public static List<Client> searchClients(String query) throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT u.IdUtilisateur, u.email, u.encryption_salt, u.wrapped_dek, u.two_factor_enabled, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.deletedAt, c.statut, c.dateNaissance " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur ";
        
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (hasQuery) {
            sql += "WHERE u.IdUtilisateur = ? OR c.nom LIKE ? OR c.prenom LIKE ? OR u.email LIKE ? ";
        }
        sql += "ORDER BY u.createdAt DESC";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (hasQuery) {
                int idSearch = -1;
                try {
                    idSearch = Integer.parseInt(query.trim());
                } catch (NumberFormatException e) {}
                ps.setInt(1, idSearch);
                String q = "%" + query.trim() + "%";
                ps.setString(2, q);
                ps.setString(3, q);
                ps.setString(4, q);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    LocalDateTime da = rs.getTimestamp("deletedAt") != null ? rs.getTimestamp("deletedAt").toLocalDateTime() : null;
                    
                    String dob = rs.getString("dateNaissance");
                    
                    Client c = new Client(rs.getInt("IdUtilisateur"), rs.getString("email"), null, rs.getString("encryption_salt"), rs.getString("wrapped_dek"), rs.getBoolean("two_factor_enabled"), ca, ua,
                                          rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), dob, da);
                    c.setStatut(rs.getString("statut"));
                    c.setNom(rs.getString("nom"));
                    c.setPrenom(rs.getString("prenom"));
                    clients.add(c);
                }
            }
        }
        return clients;
    }
}
