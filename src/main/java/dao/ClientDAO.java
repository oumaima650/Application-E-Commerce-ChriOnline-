package dao;

import model.Client;

import java.sql.*;
import java.time.LocalDateTime;

public class ClientDAO {
    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    public Client create(String email, String motDePasse,String nom, String prenom, String telephone ) throws SQLException {
        Connection conn = getConn();

        conn.setAutoCommit(false);
        int idClient = 0 ;
        try {
            String sqlUser = "INSERT INTO Utilisateur (email, motDePasse) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email);
                ps.setString(2, motDePasse);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        idClient = keys.getInt(1);
                    }
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
            System.out.println("[ClientDAO] Created with the id : "+ idClient) ;

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

            return new Client(idClient, email, motDePasse, createdAt, updatedAt, nom, prenom, telephone,null);

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
        String sql = "SELECT u.email, u.motDePasse, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur " +
                     "WHERE u.IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    return new Client(id, rs.getString("email"), rs.getString("motDePasse"), ca, ua,
                                      rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), null);
                }
            }
        }
        return null;
    }

    // Bannir un client (on utilise deletedAt dans la table Client)
    public static boolean banUser(int userId) throws SQLException {
        String query = "UPDATE Client SET deletedAt = NOW() WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Débannir un client
    public static boolean unbanUser(int userId) throws SQLException {
        String query = "UPDATE Client SET deletedAt = NULL WHERE IdUtilisateur = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
    public static List<Client> getAllClients() throws SQLException {
        List<Client> clients = new java.util.ArrayList<>();
        String sql = "SELECT u.IdUtilisateur, u.email, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.deletedAt " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur " +
                     "ORDER BY u.createdAt DESC";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                LocalDateTime da = rs.getTimestamp("deletedAt") != null ? rs.getTimestamp("deletedAt").toLocalDateTime() : null;
                
                Client c = new Client(rs.getInt("IdUtilisateur"), rs.getString("email"), null, ca, ua,
                                      rs.getString("nom"), rs.getString("prenom"), rs.getString("telephone"), da);
                
                // On peuple aussi les champs de la classe parente pour l'affichage Admin
                c.setNom(rs.getString("nom"));
                c.setPrenom(rs.getString("prenom"));
                c.setRole(da == null ? "CLIENT" : "BANNI");
                clients.add(c);
            }
        }
        return clients;
    }
}
