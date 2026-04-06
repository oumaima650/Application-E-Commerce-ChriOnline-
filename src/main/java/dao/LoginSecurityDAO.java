package dao;

import model.LoginSecurityState;
import java.sql.*;

public class LoginSecurityDAO {

    public LoginSecurityState findByIdentifier(String identifier, String type) throws SQLException {
        String sql = "SELECT * FROM login_security_state WHERE identifier = ? AND type = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LoginSecurityState state = new LoginSecurityState();
                    state.setId(rs.getInt("id"));
                    state.setIdentifier(rs.getString("identifier"));
                    state.setType(rs.getString("type"));
                    state.setCurrentAttempts(rs.getInt("current_attempts"));
                    state.setCurrentLevel(rs.getInt("current_level"));
                    Timestamp lastAttempt = rs.getTimestamp("last_attempt_at");
                    if (lastAttempt != null) state.setLastAttemptAt(lastAttempt.toLocalDateTime());
                    Timestamp blockedUntil = rs.getTimestamp("blocked_until");
                    if (blockedUntil != null) state.setBlockedUntil(blockedUntil.toLocalDateTime());
                    state.setMustResetPassword(rs.getBoolean("must_reset_password"));
                    return state;
                }
            }
        }
        return null;
    }

    public void upsert(LoginSecurityState state) throws SQLException {
        String sql = "INSERT INTO login_security_state (identifier, type, current_attempts, current_level, last_attempt_at, blocked_until, must_reset_password) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "current_attempts = VALUES(current_attempts), " +
                     "current_level = VALUES(current_level), " +
                     "last_attempt_at = VALUES(last_attempt_at), " +
                     "blocked_until = VALUES(blocked_until), " +
                     "must_reset_password = VALUES(must_reset_password)";
        
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, state.getIdentifier());
            pstmt.setString(2, state.getType());
            pstmt.setInt(3, state.getCurrentAttempts());
            pstmt.setInt(4, state.getCurrentLevel());
            pstmt.setTimestamp(5, state.getLastAttemptAt() != null ? Timestamp.valueOf(state.getLastAttemptAt()) : null);
            pstmt.setTimestamp(6, state.getBlockedUntil() != null ? Timestamp.valueOf(state.getBlockedUntil()) : null);
            pstmt.setBoolean(7, state.isMustResetPassword());
            pstmt.executeUpdate();
        }
    }

    public void reset(String identifier, String type) throws SQLException {
        String sql = "DELETE FROM login_security_state WHERE identifier = ? AND type = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, type);
            pstmt.executeUpdate();
        }
    }
}
