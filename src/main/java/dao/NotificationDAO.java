package dao;

import model.Notification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public boolean create(Notification notification) {
        String sql = "INSERT INTO Notification (IdUtilisateur, contenu, statut, created_At) VALUES (?, ?, ?, ?)";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, notification.getIdUtilisateur());
            ps.setString(2, notification.getContenu());
            ps.setString(3, notification.getStatut().name().toLowerCase());
            ps.setTimestamp(4, notification.getCreatedAt() != null ? Timestamp.valueOf(notification.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        notification.setIdNotification(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Notification> findByUtilisateur(int idUtilisateur) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM Notification WHERE IdUtilisateur = ? ORDER BY created_At DESC";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }

    public boolean updateStatut(int idNotification, Notification.StatutNotification nouveauStatut) {
        String sql = "UPDATE Notification SET statut = ? WHERE idNotification = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.name().toLowerCase());
            ps.setInt(2, idNotification);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setIdNotification(rs.getInt("idNotification"));
        notification.setIdUtilisateur(rs.getInt("IdUtilisateur"));
        notification.setContenu(rs.getString("contenu"));
        
        String statutFromDb = rs.getString("statut");
        if (statutFromDb != null) {
            notification.setStatut(Notification.StatutNotification.valueOf(statutFromDb.toUpperCase()));
        }

        if (rs.getTimestamp("created_At") != null) {
            notification.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());
        }
        return notification;
    }
}
