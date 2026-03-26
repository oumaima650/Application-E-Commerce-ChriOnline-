package dao;

import model.Administrateur;
import model.Client;
import model.Utilisateur;
import model.enums.TypeEtulisateur;

import java.sql.*;
import java.time.LocalDateTime;

public class UtilisateurDAO {

    private static Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    public static int verifyLogInInformations(String email , String motDePasse) throws SQLException {
        if (userExist(email)){
            String getInfoRequete = "SELECT IdUtilisateur from Utilisateur WHERE email = ? AND motDePasse = ? " ;
            try (PreparedStatement ps = getConn().prepareStatement(getInfoRequete)) {
                ps.setString(1, email);
                ps.setString(2, motDePasse);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) ;
                    } else {
                        // hadi password ghalet
                        return 0 ;
                    }
                }
            }
        }
        // hadi ze3ma user makaynch
        return -1 ;
    }


    public static boolean userExist(String email) throws SQLException{
        String getInfoReauete = "SELECT IdUtilisateur FROM Utilisateur WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(getInfoReauete)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true ;
                } else {
                    return false ;
                }
            }
        }
    }

    public static TypeEtulisateur userType(int idUtilisateur) throws SQLException{
        String getInfoRequete = "SELECT * FROM Client WHERE IdUtilisateur = ?";
        try (PreparedStatement ps = getConn().prepareStatement(getInfoRequete)) {
            ps.setInt(1, idUtilisateur);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return TypeEtulisateur.CLIENT;
                } else {
                    return TypeEtulisateur.ADMIN;
                }
            }
        }
    }

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
