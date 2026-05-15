package scratch;

import dao.ConnexionBDD;
import java.sql.*;

public class UpdateAdminKey {
    public static void main(String[] args) {
        String email = "amine@gmail.com";
        String newKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxEt+S2kowley3QX4aSldjA/u6dMcS2nvQw3LCYjPmDVsrwTH5g/quc2iJ6adQY93VFjYXfC+B7+9e2+UR18pVpl0EyVgHwkerB+VvzFHWm2mOYlwnubx9J98LGGdmdAemBe2HQVejO7OQkQL5lpULvaWAKu5OgbjqLgVKRn2WbEtC6kBD+aoZTJqfEXvogtxGcADHoMsDJttlcgnf+KTd/MDP45k/39LN0uLBG1avt026Zb25ANDlfeJCfWkhQpq725awDm0+eS/vVU1FbISoLVy3P5ncXeVMyH2z1kA+xJUiufZJLIhoIoNYR6Fb5bKmj9d19DMIHg6YROmZlT5RwIDAQAB";
        
        try (Connection conn = ConnexionBDD.getConnection()) {
            System.out.println("Updating Admin key for email: " + email);
            
            String sql = "UPDATE Admin a JOIN Utilisateur u ON a.IdUtilisateur = u.IdUtilisateur SET a.cle_publique = ? WHERE u.email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newKey);
                ps.setString(2, email);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("[SUCCESS] Successfully updated " + rows + " row(s).");
                } else {
                    System.out.println("[INFO] No rows updated. Checking if record exists...");
                    
                    // Try to insert if not exists
                    String findUserSql = "SELECT IdUtilisateur FROM Utilisateur WHERE email = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(findUserSql)) {
                        ps2.setString(1, email);
                        try (ResultSet rs = ps2.executeQuery()) {
                            if (rs.next()) {
                                int userId = rs.getInt(1);
                                String insertSql = "INSERT INTO Admin (IdUtilisateur, cle_publique) VALUES (?, ?)";
                                try (PreparedStatement ps3 = conn.prepareStatement(insertSql)) {
                                    ps3.setInt(1, userId);
                                    ps3.setString(2, newKey);
                                    ps3.executeUpdate();
                                    System.out.println("[SUCCESS] Successfully inserted new Admin record for user ID: " + userId);
                                }
                            } else {
                                System.out.println("[ERROR] User 'amine@gmail.com' not found in Utilisateur table.");
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
