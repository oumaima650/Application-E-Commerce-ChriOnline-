package scratch;

import dao.ConnexionBDD;
import java.sql.*;

public class CheckAdminTable {
    public static void main(String[] args) {
        String email = "amine@gmail.com";
        try (Connection conn = ConnexionBDD.getConnection()) {
            System.out.println("Checking Admin table for email: " + email);
            
            String sql = "SELECT a.*, u.email FROM Admin a JOIN Utilisateur u ON a.IdUtilisateur = u.IdUtilisateur WHERE u.email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[FOUND] Admin record exists!");
                        System.out.println("IdUtilisateur: " + rs.getInt("IdUtilisateur"));
                        System.out.println("Public Key: " + rs.getString("cle_publique"));
                    } else {
                        System.out.println("[NOT FOUND] No record found in Admin table for this email.");
                        
                        // Check if user exists in Utilisateur table at least
                        String sql2 = "SELECT IdUtilisateur FROM Utilisateur WHERE email = ?";
                        try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                            ps2.setString(1, email);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    System.out.println("[WARN] User exists in 'Utilisateur' table (ID: " + rs2.getInt(1) + ") but NOT in 'Admin' table.");
                                } else {
                                    System.out.println("[ERROR] User does NOT even exist in 'Utilisateur' table.");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
