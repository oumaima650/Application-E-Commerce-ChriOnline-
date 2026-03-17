package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.ProduitVarValeur;

public class ProduitVarValeurDAO {

    // Récupère toutes les valeurs de variantes
    public List<ProduitVarValeur> getAll() {
        List<ProduitVarValeur> list = new ArrayList<>();
        String query = "SELECT * FROM ProduitVarValeur";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToPVV(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des ProduitVarValeur", e);
        }
        return list;
    }

    // Récupère une valeur par son ID
    public ProduitVarValeur getById(int id) {
        String query = "SELECT * FROM ProduitVarValeur WHERE idPVV = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPVV(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du ProduitVarValeur id=" + id, e);
        }
        return null;
    }

    // Enregistre une valeur de variante
    public boolean save(ProduitVarValeur pvv) {
        String query = "INSERT INTO ProduitVarValeur (idProduit, idVariante, valeur) VALUES (?, ?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, pvv.getIdProduit());
            pstmt.setInt(2, pvv.getIdVariante());
            pstmt.setString(3, pvv.getValeur());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        pvv.setIdPVV(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du ProduitVarValeur", e);
        }
        return false;
    }

    // Met à jour une valeur de variante
    public boolean update(ProduitVarValeur pvv) {
        String query = "UPDATE ProduitVarValeur SET idProduit = ?, idVariante = ?, valeur = ? WHERE idPVV = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pvv.getIdProduit());
            pstmt.setInt(2, pvv.getIdVariante());
            pstmt.setString(3, pvv.getValeur());
            pstmt.setInt(4, pvv.getIdPVV());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du ProduitVarValeur", e);
        }
    }

    // Supprime une valeur de variante
    public boolean delete(int id) {
        String query = "DELETE FROM ProduitVarValeur WHERE idPVV = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du ProduitVarValeur", e);
        }
    }
   
    // Récupère les valeurs par produit
    public List<ProduitVarValeur> getByProduit(int idProduit) {
        List<ProduitVarValeur> list = new ArrayList<>();
        String query = "SELECT * FROM ProduitVarValeur WHERE idProduit = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, idProduit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPVV(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération PVV par produit", e);
        }
        return list;
    }

    // Convertit un résultat SQL en objet ProduitVarValeur
    private ProduitVarValeur mapResultSetToPVV(ResultSet rs) throws SQLException {
        ProduitVarValeur pvv = new ProduitVarValeur();
        pvv.setIdPVV(rs.getInt("idPVV"));
        pvv.setIdProduit(rs.getInt("idProduit"));
        pvv.setIdVariante(rs.getInt("idVariante"));
        pvv.setValeur(rs.getString("valeur"));
        return pvv;
    }
}
