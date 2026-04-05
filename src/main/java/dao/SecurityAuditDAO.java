package dao;

import java.sql.*;

public class SecurityAuditDAO {

    public void log(String ip, String email, int level, String result, boolean captchaPassed) {
        String sql = "INSERT INTO security_audit_logs (ip_address, email, level_assigned, attempt_result, captcha_passed) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setString(2, email);
            pstmt.setInt(3, level);
            pstmt.setString(4, result);
            pstmt.setBoolean(5, captchaPassed);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SecurityAuditDAO] Erreur lors du logging de sécurité : " + e.getMessage());
        }
    }
}
