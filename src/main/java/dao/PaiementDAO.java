package dao;

import model.Paiement;
import model.enums.StatutPaiement;
import model.enums.MethodePaiement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaiementDAO {

    //methode pour inserer paiement ds BD
    public boolean create(Paiement paiement) {
        String sql = "INSERT INTO Paiement (idCommande, idCarte, montant, statutPaiement, methodePaiement, created_At) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, paiement.getIdCommande());
            if (paiement.getIdCarte() != null) {
                ps.setInt(2, paiement.getIdCarte());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setBigDecimal(3, paiement.getMontant());
            ps.setString(4, paiement.getStatutPaiement() != null ? paiement.getStatutPaiement().name().toLowerCase() : StatutPaiement.EN_ATTENTE.name().toLowerCase());
            ps.setString(5, paiement.getMethodePaiement() != null ? paiement.getMethodePaiement().name().toLowerCase() : MethodePaiement.CARD.name().toLowerCase());
            ps.setTimestamp(6, paiement.getDatePaiement() != null ? Timestamp.valueOf(paiement.getDatePaiement()) : new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        paiement.setIdPaiement(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Paiement findById(int idPaiement) {
        String sql = "SELECT * FROM Paiement WHERE idPaiement = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPaiement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaiement(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Paiement> findByCommande(int idCommande) {
        List<Paiement> paiements = new ArrayList<>();
        String sql = "SELECT * FROM Paiement WHERE idCommande = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    paiements.add(mapResultSetToPaiement(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paiements;
    }

    public boolean updateStatut(int idPaiement, StatutPaiement nouveauStatut) {
        String sql = "UPDATE Paiement SET statutPaiement = ? WHERE idPaiement = ?";
        try (Connection con = ConnexionBDD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.name().toLowerCase());
            ps.setInt(2, idPaiement);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Paiement mapResultSetToPaiement(ResultSet rs) throws SQLException {
        Paiement paiement = new Paiement();
        paiement.setIdPaiement(rs.getInt("idPaiement"));
        paiement.setIdCommande(rs.getInt("idCommande"));
        int idCarte = rs.getInt("idCarte");
        if (!rs.wasNull()) {
            paiement.setIdCarte(idCarte);
        } else {
            paiement.setIdCarte(null);
        }
        paiement.setMontant(rs.getBigDecimal("montant"));
        
        String statutFromDb = rs.getString("statutPaiement");
        if (statutFromDb != null) {
            paiement.setStatutPaiement(StatutPaiement.valueOf(statutFromDb.toUpperCase()));
        }
        
        String methodeFromDb = rs.getString("methodePaiement");
        if (methodeFromDb != null) {
            paiement.setMethodePaiement(MethodePaiement.valueOf(methodeFromDb.toUpperCase()));
        }

        if (rs.getTimestamp("created_At") != null) {
            paiement.setDatePaiement(rs.getTimestamp("created_At").toLocalDateTime());
        }
        return paiement;
    }
}
