package dao;

import model.Client;
import java.sql.*;
import java.time.LocalDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.ArrayList;
import service.StorageEncryptionService;

public class ClientDAO {
    private static final Logger logger = LogManager.getLogger(ClientDAO.class);

    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    public Client create(String email, String motDePasse, String nom, String prenom, String telephone, String dateNaissance) throws SQLException {
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

            StorageEncryptionService encryptionService = StorageEncryptionService.getInstance();
            logger.info("[AUDIT SECU] Préparation de l'insertion MySQL : Chiffrement des données du client.");
            String encryptedNom = encryptionService.encryptDeterministic(nom);
            String encryptedPrenom = encryptionService.encryptDeterministic(prenom);
            String encryptedPhone = encryptionService.encryptDeterministic(telephone);
            String encryptedDob = encryptionService.encrypt(dateNaissance);

            logger.info("[AUDIT SECU] Données chiffrées avec succès pour l'email: {}", email);

            String sqlClient = "INSERT INTO Client (IdUtilisateur, nom, prenom, telephone, dateNaissance) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlClient)) {
                ps.setInt(1, idClient);
                ps.setString(2, encryptedNom);
                ps.setString(3, encryptedPrenom);
                ps.setString(4, encryptedPhone);
                ps.setString(5, encryptedDob);
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
            return new Client(idClient, email, motDePasse, twoFactorEnabled, createdAt, updatedAt, nom, prenom, telephone, dateNaissance, null);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static boolean isTelephoneExist(String telephone) throws SQLException {
        // Pour chercher un téléphone chiffré aléatoirement, on ne peut pas faire d'index aveugle simple 
        // SAUF si on décide de chiffrer aussi le téléphone de manière déterministe.
        // Pour l'instant, on va le laisser ainsi ou utiliser l'index déterministe si on veut la recherche exacte.
        String encryptedPhone = StorageEncryptionService.getInstance().encryptDeterministic(telephone);
        String sql = "SELECT 1 FROM Client WHERE telephone = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, encryptedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Client findById(int id) throws SQLException {
        String sql = "SELECT u.email, u.motDePasse, u.two_factor_enabled, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.statut, c.dateNaissance " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur " +
                     "WHERE u.IdUtilisateur = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    
                    StorageEncryptionService encryptionService = StorageEncryptionService.getInstance();
                    String decryptedNom = encryptionService.decryptDeterministic(rs.getString("nom"));
                    String decryptedPrenom = encryptionService.decryptDeterministic(rs.getString("prenom"));
                    String decryptedPhone = encryptionService.decryptDeterministic(rs.getString("telephone"));
                    String decryptedDob = encryptionService.decrypt(rs.getString("dateNaissance"));
                    logger.debug("[ClientDAO] Decrypted fields for client ID: {}", id);
                    
                    Client c = new Client(id, rs.getString("email"), rs.getString("motDePasse"), rs.getBoolean("two_factor_enabled"), ca, ua,
                                      decryptedNom, decryptedPrenom, decryptedPhone, decryptedDob, null);
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
        String sql = "SELECT u.IdUtilisateur, u.email, u.two_factor_enabled, u.createdAt, u.updatedAt, c.nom, c.prenom, c.telephone, c.deletedAt, c.statut, c.dateNaissance " +
                     "FROM Utilisateur u JOIN Client c ON u.IdUtilisateur = c.IdUtilisateur ";
        
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (hasQuery) {
            // Note: On change LIKE par = pour les champs chiffrés car LIKE ne marche pas sur du ciphertext
            sql += "WHERE u.IdUtilisateur = ? OR c.nom = ? OR c.prenom = ? OR u.email LIKE ? ";
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
                
                // Pour le nom et prénom, on doit chiffrer la recherche de manière déterministe
                logger.info("[AUDIT SECU] Exécution recherche avec chiffrement déterministe.");
                String encryptedQuery = StorageEncryptionService.getInstance().encryptDeterministic(query.trim());
                ps.setString(2, encryptedQuery);
                ps.setString(3, encryptedQuery);
                
                // Pour l'email (non chiffré), on garde le LIKE
                ps.setString(4, "%" + query.trim() + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                StorageEncryptionService encryptionService = StorageEncryptionService.getInstance();
                while (rs.next()) {
                    LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                    LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                    LocalDateTime da = rs.getTimestamp("deletedAt") != null ? rs.getTimestamp("deletedAt").toLocalDateTime() : null;
                    
                    String decryptedNom = encryptionService.decryptDeterministic(rs.getString("nom"));
                    String decryptedPrenom = encryptionService.decryptDeterministic(rs.getString("prenom"));
                    String decryptedPhone = encryptionService.decryptDeterministic(rs.getString("telephone"));
                    String decryptedDob = encryptionService.decrypt(rs.getString("dateNaissance"));
                    
                    Client c = new Client(rs.getInt("IdUtilisateur"), rs.getString("email"), null, rs.getBoolean("two_factor_enabled"), ca, ua,
                                          decryptedNom, decryptedPrenom, decryptedPhone, decryptedDob, da);
                    c.setStatut(rs.getString("statut"));
                    clients.add(c);
                }
            }
        }
        return clients;
    }
}
