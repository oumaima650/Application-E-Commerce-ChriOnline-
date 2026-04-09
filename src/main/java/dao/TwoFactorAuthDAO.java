package dao;

import java.sql.*;
import java.time.LocalDateTime;

public class TwoFactorAuthDAO {

    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    /**
     * Stores or updates a 2FA code for an email.
     */
    public void upsertCode(String email, String codeHash, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO TwoFactorCodes (email, code_hash, expires_at, attempts) " +
                     "VALUES (?, ?, ?, 0) " +
                     "ON DUPLICATE KEY UPDATE code_hash = ?, expires_at = ?, attempts = 0";
        
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, codeHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.setString(4, codeHash);
            ps.setTimestamp(5, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves the code info for an email.
     */
    public CodeInfo findCode(String email) throws SQLException {
        String sql = "SELECT code_hash, expires_at, attempts FROM TwoFactorCodes WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CodeInfo(
                        rs.getString("code_hash"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        rs.getInt("attempts")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Increments the attempt count for a code.
     */
    public void incrementAttempts(String email) throws SQLException {
        String sql = "UPDATE TwoFactorCodes SET attempts = attempts + 1 WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes the code for an email.
     */
    public void deleteCode(String email) throws SQLException {
        String sql = "DELETE FROM TwoFactorCodes WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    public record CodeInfo(String hash, LocalDateTime expiresAt, int attempts) {}
}
