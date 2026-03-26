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

    public record LoginData(int id, String hash) {}

    /**
     * Gets the user ID and password hash for a given email.
     * @return LoginData or null if user doesn't exist.
     */
    public static LoginData getLoginData(String email) throws SQLException {
        String sql = "SELECT IdUtilisateur, motDePasse FROM Utilisateur WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LoginData(rs.getInt(1), rs.getString(2));
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
/*
    // Récupère tous les utilisateurs
    public static List<model.Utilisateur> getAllUsers() throws SQLException {
        List<model.Utilisateur> utilisateurs = new ArrayList<>();
        String query = "SELECT * FROM Utilisateur ORDER BY created_At DESC";
        
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                model.Utilisateur user = new model.Utilisateur();
                user.setId(rs.getInt("IdUtilisateur"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setType(rs.getString("type"));
                utilisateurs.add(user);
            }
        }
        return utilisateurs;
    }

    // Bannir un utilisateur
    public static boolean banUser(int userId) throws SQLException {
        String query = "UPDATE Utilisateur SET type = 'BANNI' WHERE IdUtilisateur = ?";
        
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Débannir un utilisateur
    public static boolean unbanUser(int userId) throws SQLException {
        String query = "UPDATE Utilisateur SET type = 'CLIENT' WHERE IdUtilisateur = ?";
        
        try (Connection conn = getConn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
 */

    public static Utilisateur findById(int id) throws SQLException {
        if (userType(id) == TypeEtulisateur.CLIENT) {
            return new ClientDAO().findById(id);
        } else {
            String sql = "SELECT email, motDePasse, createdAt, updatedAt FROM Utilisateur WHERE IdUtilisateur = ?";
            try (Connection conn = getConn();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LocalDateTime ca = rs.getTimestamp("createdAt") != null ? rs.getTimestamp("createdAt").toLocalDateTime() : null;
                        LocalDateTime ua = rs.getTimestamp("updatedAt") != null ? rs.getTimestamp("updatedAt").toLocalDateTime() : null;
                        return new Administrateur(id, rs.getString("email"), rs.getString("motDePasse"), ca, ua);
                    }
                }
            }
        }
        return null;
    }
}
