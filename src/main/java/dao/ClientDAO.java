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

    public Client create(String email, String motDePasse, String nom, String prenom, String telephone) throws SQLException {
        Connection conn = getConn();
        conn.setAutoCommit(false);
        int idClient = 0;
        try {
            String sqlUser = "INSERT INTO Utilisateur (email, motDePasse) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, motDePasse);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) idClient = keys.getInt(1);
                }
            }

            String sqlClient = "INSERT INTO Client (IdUtilisateur, nom, prenom, telephone) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlClient)) {
                ps.setInt(1, idClient);
                ps.setString(2, nom);
                ps.setString(3, prenom);
                ps.setString(4, telephone);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[ClientDAO] Created with the id : " + idClient);

            LocalDateTime createdAt = null;
            LocalDateTime updatedAt = null;
            String sqlFetch = "SELECT createdAt, updatedAt FROM Utilisateur WHERE IdUtilisateur = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlFetch)) {
                ps.setInt(1, idClient);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ca = rs.getTimestamp("createdAt");
                        Timestamp ua = rs.getTimestamp("updatedAt");
                        if (ca != null) createdAt = ca.toLocalDateTime();
                        if (ua != null) updatedAt = ua.toLocalDateTime();
                    }
                }
            }
            return new Client(idClient, email, motDePasse, createdAt, updatedAt, nom, prenom, telephone, null);
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
        String sql = "SELECT u.email, u.motDePasse, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.statut " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur " +
                     "WHERE u.IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    Client c = new Client(id, rs.getString("email"), rs.getString("motDePasse"), ca, ua,
                                      rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), null);
                    c.setStatut(rs.getString("statut"));
                    return c;
                }
            }
        }
        return null;
    }

    public static boolean banUser(int userId) throws SQLException {
        String query = "UPDATE Client SET statut = 'BANNI', deletedAt = NOW() WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean unbanUser(int userId) throws SQLException {
        String query = "UPDATE Client SET statut = 'ACTIF', deletedAt = NULL WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<Client> getAllClients() throws SQLException {
        return searchClients(null);
    }

    public static List<Client> searchClients(String query) throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT u.IdUtilisateur, u.email, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.deletedAt, c.statut " +
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
                    
                    Client c = new Client(rs.getInt("IdUtilisateur"), rs.getString("email"), null, ca, ua,
                                          rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), da);
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
