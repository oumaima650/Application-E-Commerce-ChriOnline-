package dao;

import java.sql.*;
import java.time.LocalDateTime;

public class PasswordResetDAO {

    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    /**
     * Stores or updates a reset code for an email.
     */
    public void upsertCode(String email, String codeOPTHash, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_codes (email, code_OPT_hash, expires_at) " +
                     "VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE code_OPT_hash = ?, expires_at = ?, created_at = CURRENT_TIMESTAMP";
        
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, codeOPTHash);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(expiresAt));
            ps.setString(4, codeOPTHash);
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    /**
     * Finds a valid (non-expired) code hash for an email.
     * @return The hash or null if not found or expired.
     */
    public String findValidCodeHash(String email) throws SQLException {
        String sql = "SELECT code_OPT_hash FROM password_reset_codes WHERE email = ? AND expires_at > ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code_OPT_hash");
                }
            }
        }
        return null;
    }

    /**
     * Deletes the code for an email after use.
     */
    public void deleteCode(String email) throws SQLException {
        String sql = "DELETE FROM password_reset_codes WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }
}
