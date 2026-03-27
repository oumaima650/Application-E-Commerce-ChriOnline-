package dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;

public class RefreshTokenDAO {

    private Connection getConn() {
        return ConnexionBDD.getConnection();
    }

    /**
     * Hashes the raw UUID refresh token using SHA-256 for secure storage.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    public void save(int userId, String rawToken, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, hashToken(rawToken));
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public record RefreshTokenInfo(int id, int userId, boolean isUsed, boolean isRevoked, LocalDateTime expiresAt) {}

    public RefreshTokenInfo findByToken(String rawToken) throws SQLException {
        String sql = "SELECT id, user_id, is_used, is_revoked, expires_at FROM refresh_tokens WHERE token_hash = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hashToken(rawToken));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RefreshTokenInfo(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getBoolean("is_used"),
                            rs.getBoolean("is_revoked"),
                            rs.getTimestamp("expires_at").toLocalDateTime()
                    );
                }
            }
        }
        return null;
    }

    public void markAsUsed(int id) throws SQLException {
        String sql = "UPDATE refresh_tokens SET is_used = TRUE WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * ALARM: Revokes all tokens for a user if reuse is detected.
     */
    public void revokeAllForUser(int userId) throws SQLException {
        String sql = "UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.err.println("[SECURITY ALARM] All refresh tokens revoked for user ID: " + userId);
        }
    }
}
