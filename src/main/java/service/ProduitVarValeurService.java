package service;

import dao.ProduitVarValeurDAO;
import model.ProduitVarValeur;
import java.util.List;

public class ProduitVarValeurService {

    private ProduitVarValeurDAO pvvDAO = new ProduitVarValeurDAO();

    // Retourne toutes les lignes PVV
    public List<ProduitVarValeur> getAll() {
        try {
            return pvvDAO.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des valeurs de variantes", e);
        }
    }

    // Récupère une ligne par son ID
    public ProduitVarValeur getById(int id) {
        ProduitVarValeur pvv = pvvDAO.getById(id);
        if (pvv == null) {
            throw new IllegalArgumentException("Valeur de variante introuvable id=" + id);
        }
        return pvv;
    }

    // Récupère toutes les valeurs pour un produit
    public List<ProduitVarValeur> getByProduit(int idProduit) {
        try {
            return pvvDAO.getByProduit(idProduit);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des valeurs pour le produit id=" + idProduit, e);
        }
    }

    // Crée une valeur de variante
    public void creer(ProduitVarValeur pvv) {
        if (pvv.getIdProduit() <= 0) {
            throw new IllegalArgumentException("L'identifiant du produit est invalide");
        }
        if (pvv.getIdVariante() <= 0) {
            throw new IllegalArgumentException("L'identifiant de la variante est invalide");
        }
        if (pvv.getValeur() == null || pvv.getValeur().trim().isEmpty()) {
            throw new IllegalArgumentException("La valeur de la variante est obligatoire");
        }
        try {
            pvvDAO.save(pvv);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la valeur de variante", e);
        }
    }

    // Modifie une valeur de variante
    public void modifier(ProduitVarValeur pvv) {
        getById(pvv.getIdPVV()); // Vérifie l'existence
        if (pvv.getValeur() == null || pvv.getValeur().trim().isEmpty()) {
            throw new IllegalArgumentException("La valeur de la variante est obligatoire");
        }
        try {
            pvvDAO.update(pvv);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification de la valeur de variante", e);
        }
    }

    // Supprime une valeur de variante
    public void supprimer(int id) {
        getById(id); // Vérifie l'existence
        try {
            pvvDAO.delete(id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de la valeur de variante", e);
        }
    }
}
