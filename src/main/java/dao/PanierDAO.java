package dao;

import model.Panier;
import model.LignePanier;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierDAO {

    /**
     * Calcule le montant total du panier directement en base de données.
     * C'est beaucoup plus performant car on n'a pas besoin de charger toutes les lignes en Java.
     */
    public BigDecimal getMontantTotal(int idPanier) {
        String query = "SELECT SUM(lp.quantite * s.prix) as total " +
                       "FROM LignePanier lp " +
                       "JOIN SKU s ON lp.SKU = s.SKU " +
                       "WHERE lp.idPanier = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idPanier);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Trouve le panier d'un client. S'il n'existe pas, il est créé.
     */
    public Panier findOrCreateByClientId(int idClient) {
        Panier panier = getPanierByClientId(idClient);
        if (panier == null) {
            int idPanier = createPanier(idClient);
            panier = new Panier(idPanier, idClient, 0.0, null);
        }
        panier.setLignes(getLignesByPanierId(panier.getIdPanier()));
        return panier;
    }

    private Panier getPanierByClientId(int idClient) {
        String query = "SELECT * FROM Panier WHERE IdClient = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idClient);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Panier p = new Panier();
                p.setIdPanier(rs.getInt("idPanier"));
                p.setIdClient(rs.getInt("IdClient"));
                p.setCreatedAt(rs.getTimestamp("created_At") != null ? rs.getTimestamp("created_At").toLocalDateTime() : null);
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int createPanier(int idClient) {
        String query = "INSERT INTO Panier (IdClient) VALUES (?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idClient);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<LignePanier> getLignesByPanierId(int idPanier) {
        List<LignePanier> lignes = new ArrayList<>();
        String query = "SELECT * FROM LignePanier WHERE idPanier = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idPanier);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lignes.add(new LignePanier(
                    rs.getInt("idPanier"),
                    rs.getString("SKU"),
                    rs.getInt("quantite"),
                    BigDecimal.ZERO // Le sous-total n'est pas en BDD, on l'initialise
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lignes;
    }

    public void ajouterOuMettreAJourLigne(LignePanier ligne) {
        String query = "INSERT INTO LignePanier (idPanier, SKU, quantite) VALUES (?, ?, ?) " +
                       "ON DUPLICATE KEY UPDATE quantite = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, ligne.getIdPanier());
            stmt.setString(2, ligne.getSku());
            stmt.setInt(3, ligne.getQuantite());
            stmt.setInt(4, ligne.getQuantite());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimerLigne(int idPanier, String sku) {
        String query = "DELETE FROM LignePanier WHERE idPanier = ? AND SKU = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idPanier);
            stmt.setString(2, sku);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void viderPanier(int idPanier) {
        String query = "DELETE FROM LignePanier WHERE idPanier = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idPanier);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
