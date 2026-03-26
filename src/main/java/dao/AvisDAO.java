package dao;

import model.Avis;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AvisDAO {

    public List<Avis> getAvisByProduitId(int idProduit) {
        List<Avis> avisList = new ArrayList<>();
        String query = "SELECT a.*, c.nom, c.prenom " +
                       "FROM Avis a " +
                       "JOIN Client c ON a.IdClient = c.IdUtilisateur " +
                       "WHERE a.idProduit = ? " +
                       "ORDER BY a.idCommentaire DESC";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idProduit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Avis avis = new Avis(
                    rs.getInt("idCommentaire"),
                    rs.getInt("IdClient"),
                    rs.getInt("idProduit"),
                    rs.getObject("idCommande") != null ? rs.getInt("idCommande") : null,
                    rs.getString("contenu"),
                    rs.getObject("evaluation") != null ? rs.getInt("evaluation") : null,
                    rs.getString("image")
                );
                avis.setNomClient(rs.getString("prenom") + " " + rs.getString("nom"));
                avisList.add(avis);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return avisList;
    }

    public boolean addAvis(Avis avis) {
        String query = "INSERT INTO Avis (IdClient, idProduit, idCommande, contenu, evaluation, image) " +
                       "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, avis.getIdClient());
            stmt.setInt(2, avis.getIdProduit());
            
            if (avis.getIdCommande() != null) {
                stmt.setInt(3, avis.getIdCommande());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setString(4, avis.getContenu());
            
            if (avis.getEvaluation() != null) {
                stmt.setInt(5, avis.getEvaluation());
            } else {
                stmt.setNull(5, Types.TINYINT);
            }
            
            stmt.setString(6, avis.getImage());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    avis.setIdCommentaire(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
