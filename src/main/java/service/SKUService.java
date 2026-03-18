package service;

import dao.SKUDAO;
import model.SKU;
import java.math.BigDecimal;
import java.util.List;

public class SKUService {

    private SKUDAO skuDAO = new SKUDAO();

    // Récupère tous les SKUs
    public List<SKU> getAll() {
        try {
            return skuDAO.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des SKUs", e);
        }
    }

    // Récupère un SKU par son code unique
    public SKU getBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("Le code SKU ne peut pas être vide");
        }
        SKU s = skuDAO.getBySku(sku);
        if (s == null) {
            throw new IllegalArgumentException("SKU introuvable : " + sku);
        }
        return s;
    }

    // Récupère tous les SKUs liés à un produit
    public List<SKU> getByProduit(int idProduit) {
        try {
            return skuDAO.getByProduit(idProduit);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des SKUs pour le produit id=" + idProduit, e);
        }
    }

    // Crée un nouveau SKU
    public void creer(SKU sku) {
        if (sku.getSku() == null || sku.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("Le code SKU est obligatoire");
        }
        if (sku.getPrix() == null || sku.getPrix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être un nombre positif");
        }
        if (sku.getQuantite() < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }
        try {
            skuDAO.save(sku);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du SKU", e);
        }
    }

    // Modifie un SKU existant
    public void modifier(SKU sku) {
        getBySku(sku.getSku()); // Vérifie l'existence
        if (sku.getPrix() == null || sku.getPrix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être un nombre positif");
        }
        if (sku.getQuantite() < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }
        try {
            skuDAO.update(sku);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification du SKU", e);
        }
    }

    // Supprime un SKU
    public void supprimer(String sku) {
        getBySku(sku); // Vérifie l'existence
        try {
            skuDAO.delete(sku);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression du SKU", e);
        }
    }

    // Lie un SKU à une valeur de variante
    public void ajouterValeur(String sku, int idPVV) {
        getBySku(sku); // Vérifie l'existence du SKU
        try {
            skuDAO.addValeur(sku, idPVV);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'ajout de la valeur au SKU", e);
        }
    }

    // Retire une valeur de variante d'un SKU
    public void retirerValeur(String sku, int idPVV) {
        getBySku(sku); // Vérifie l'existence
        try {
            skuDAO.removeValeur(sku, idPVV);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du retrait de la valeur du SKU", e);
        }
    }
}
