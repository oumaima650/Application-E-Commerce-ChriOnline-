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

}
