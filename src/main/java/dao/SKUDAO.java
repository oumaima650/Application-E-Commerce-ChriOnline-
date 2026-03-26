package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.SKU;

public class SKUDAO {

    // Récupère tous les SKUs
    public List<SKU> getAll() {
        List<SKU> skus = new ArrayList<>();
        String query = "SELECT * FROM SKU";
        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                skus.add(mapResultSetToSKU(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des SKUs", e);
        }
        return skus;
    }

    // Récupère un SKU par son identifiant
    public SKU getBySku(String skuId) {
        String query = "SELECT * FROM SKU WHERE SKU = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, skuId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSKU(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du SKU", e);
        }
        return null;
    }

    // Enregistre un nouveau SKU
    public boolean save(SKU sku) {
        String query = "INSERT INTO SKU (SKU, prix, quantite, image) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sku.getSku());
            pstmt.setBigDecimal(2, sku.getPrix());
            pstmt.setInt(3, sku.getQuantite());
            pstmt.setString(4, sku.getImage());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du SKU", e);
        }
    }

    // Met à jour les informations d'un SKU
    public boolean update(SKU sku) {
        String query = "UPDATE SKU SET prix = ?, quantite = ?, image = ? WHERE SKU = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setBigDecimal(1, sku.getPrix());
            pstmt.setInt(2, sku.getQuantite());
            pstmt.setString(3, sku.getImage());
            pstmt.setString(4, sku.getSku());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du SKU", e);
        }
    }

    // Supprime un SKU par son identifiant
    public boolean delete(String skuId) {
        String query = "DELETE FROM SKU WHERE SKU = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, skuId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du SKU", e);
        }
    }

public List<SKU> getByProduit(int idProduit) {
    List<SKU> skus = new ArrayList<>();
    String query = "SELECT DISTINCT s.* FROM SKU s " +
                   "JOIN SKUVarValeur svv ON s.SKU = svv.SKU " +
                   "JOIN ProduitVarValeur pvv ON svv.idPVV = pvv.idPVV " +
                   "WHERE pvv.idProduit = ?";
    try (Connection conn = ConnexionBDD.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setInt(1, idProduit);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                skus.add(mapResultSetToSKU(rs));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Erreur récupération SKUs par produit", e);
    }
    return skus;
}


    // Lie une valeur de variante à un SKU
    public boolean addValeur(String sku, int idPVV) {
        String query = "INSERT INTO SKUVarValeur (SKU, idPVV) VALUES (?, ?)";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sku);
            pstmt.setInt(2, idPVV);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout d'une valeur au SKU", e);
        }
    }

    // Supprime le lien entre une valeur et un SKU
    public boolean removeValeur(String sku, int idPVV) {
        String query = "DELETE FROM SKUVarValeur WHERE SKU = ? AND idPVV = ?";
        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sku);
            pstmt.setInt(2, idPVV);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression d'une valeur du SKU", e);
        }
    }

    public SKU getByVariants(int idProduit, List<Integer> pvvIds) {
        if (pvvIds == null || pvvIds.isEmpty()) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT s.* FROM SKU s WHERE s.SKU IN ( ");
        sb.append("  SELECT svv.SKU FROM SKUVarValeur svv ");
        sb.append("  JOIN ProduitVarValeur pvv ON svv.idPVV = pvv.idPVV ");
        sb.append("  WHERE pvv.idProduit = ? AND svv.idPVV IN (");
        for (int i = 0; i < pvvIds.size(); i++) {
            sb.append("?");
            if (i < pvvIds.size() - 1) sb.append(",");
        }
        sb.append(") GROUP BY svv.SKU HAVING COUNT(DISTINCT svv.idPVV) = ? )");

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
            
            pstmt.setInt(1, idProduit);
            for (int i = 0; i < pvvIds.size(); i++) {
                pstmt.setInt(i + 2, pvvIds.get(i));
            }
            pstmt.setInt(pvvIds.size() + 2, pvvIds.size());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSKU(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur résolution SKU par variantes", e);
        }
        return null;
    }

    // Convertit un résultat SQL en objet SKU
    private SKU mapResultSetToSKU(ResultSet rs) throws SQLException {
        SKU sku = new SKU();
        sku.setSku(rs.getString("SKU"));
        sku.setPrix(rs.getBigDecimal("prix"));
        sku.setQuantite(rs.getInt("quantite"));
        sku.setImage(rs.getString("image"));
        return sku;
    }
}
