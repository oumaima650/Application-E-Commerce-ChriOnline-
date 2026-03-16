package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Produit;

public class ProduitDAO {

    public List<Produit> getAll() {

        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM Produit";

        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de tous les produits", e);
        }
        return produits;

    }

    public Produit getById(int id) {

        String query = "SELECT * FROM Produit WHERE idProduit = ?"; // protege contre les injections SQL

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);

                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du produit id=" + id, e);
            // Message d'erreur avec l'ID concerné → plus facile à déboguer
        }

        return null;
        // Si aucun produit trouvé avec cet ID, on retourne null
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE 3 : getByNom() — Recherche par nom (NOUVEAU)
    // ════════════════════════════════════════════════════════
    public List<Produit> getByNom(String nom) {
        // Retourne une liste car plusieurs produits peuvent avoir
        // un nom similaire (ex: "Chaise rouge", "Chaise bleue")

        List<Produit> produits = new ArrayList<>();

        String query = "SELECT * FROM Produit WHERE nom LIKE ?";
        // LIKE permet la recherche partielle
        // ex: chercher "chai" trouvera "Chaise"

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, "%" + nom + "%");
            // % = joker en SQL (remplace n'importe quels caractères)
            // "%chaise%" trouvera tout ce qui CONTIENT "chaise"
            // setString car nom est une chaîne de caractères

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapResultSetToProduit(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par nom : " + nom, e);
        }

        return produits;
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE 4 : save() — Insère un NOUVEAU produit en base
    // ════════════════════════════════════════════════════════
    public boolean save(Produit produit) {

        String query = "INSERT INTO Produit (nom, description) VALUES (?, ?)";
        // On n'insère pas idProduit → auto-généré par la base
        // On n'insère pas created_At → valeur par défaut côté base

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Statement.RETURN_GENERATED_KEYS → demande à la base
            // de nous renvoyer l'ID auto-généré après l'INSERT

            pstmt.setString(1, produit.getNom());
            // 1er ? = le nom du produit

            pstmt.setString(2, produit.getDescription());
            // 2ème ? = la description du produit

            int affectedRows = pstmt.executeUpdate();
            // executeUpdate() exécute INSERT/UPDATE/DELETE
            // retourne le nombre de lignes affectées
            // Si tout va bien → affectedRows = 1

            if (affectedRows > 0) {
                // L'insertion a réussi

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    // On récupère les clés auto-générées (l'idProduit)

                    if (generatedKeys.next()) {
                        produit.setIdProduit(generatedKeys.getInt(1));
                        // On met à jour l'objet produit avec son nouvel ID
                        // Ainsi l'objet en mémoire est synchronisé avec la base
                    }
                }
                return true;
                // Insertion réussie
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'insertion du produit : " + produit.getNom(), e);
        }

        return false;
        // Insertion échouée (affectedRows = 0)
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE 5 : update() — Modifie un produit existant
    // ════════════════════════════════════════════════════════
    public boolean update(Produit produit) {

        String query = "UPDATE Produit SET nom = ?, description = ? WHERE idProduit = ?";
        // On modifie nom et description
        // WHERE idProduit = ? → on cible précisément le bon produit

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, produit.getNom());        // 1er ? = nouveau nom
            pstmt.setString(2, produit.getDescription()); // 2ème ? = nouvelle description
            pstmt.setInt(3, produit.getIdProduit());      // 3ème ? = ID du produit à modifier

            return pstmt.executeUpdate() > 0;
            // Retourne true si au moins 1 ligne a été modifiée
            // Retourne false si aucun produit avec cet ID n'existe

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du produit id=" + produit.getIdProduit(), e);
        }
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE 6 : delete() — Supprime un produit par son ID
    // ════════════════════════════════════════════════════════
    public boolean delete(int id) {

        String query = "DELETE FROM Produit WHERE idProduit = ?";

        try (Connection conn = ConnexionBDD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            // On remplace ? par l'ID à supprimer

            return pstmt.executeUpdate() > 0;
            // true → suppression réussie
            // false → aucun produit avec cet ID

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du produit id=" + id, e);
        }
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE 7 : count() — Compte le nombre de produits (NOUVEAU)
    // ════════════════════════════════════════════════════════
    public int count() {

        String query = "SELECT COUNT(*) FROM Produit";
        // COUNT(*) → fonction SQL qui compte toutes les lignes

        try (Connection conn = ConnexionBDD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
                // La 1ère colonne du résultat contient le nombre total
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des produits", e);
        }

        return 0;
        // Si problème, on retourne 0 par défaut
    }


    // ════════════════════════════════════════════════════════
    // MÉTHODE PRIVÉE : mapResultSetToProduit()
    // Convertit une ligne SQL en objet Produit Java
    // Utilisée en interne par getAll(), getById(), getByNom()
    // ════════════════════════════════════════════════════════
    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        // private → utilisable uniquement dans cette classe
        // throws SQLException → on laisse l'erreur remonter à l'appelant

        Produit prod = new Produit();
        // On crée un objet Produit vide

        prod.setIdProduit(rs.getInt("idProduit"));
        // rs.getInt("idProduit") → lit la colonne "idProduit" de la ligne courante

        prod.setNom(rs.getString("nom"));
        // rs.getString("nom") → lit la colonne "nom"

        prod.setDescription(rs.getString("description"));
        // rs.getString("description") → lit la colonne "description"

        prod.setCreatedAt(rs.getTimestamp("created_At").toLocalDateTime());
        // rs.getTimestamp() → lit une date/heure SQL
        // .toLocalDateTime() → convertit en type Java moderne LocalDateTime

        return prod;
        // On retourne l'objet Produit complet et rempli
    }
}