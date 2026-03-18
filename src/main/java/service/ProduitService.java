package service;

import dao.ProduitDAO;
import model.Produit;
import java.util.List;

public class ProduitService {

    private ProduitDAO produitDAO = new ProduitDAO();

    // Récupère tous les produits
    public List<Produit> getAll() {
        try {
            return produitDAO.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des produits", e);
        }
    }

    // Récupère un produit par son ID
    public Produit getById(int id) {
        Produit p = produitDAO.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("Produit introuvable id=" + id);
        }
        return p;
    }

    // Recherche des produits par nom
    public List<Produit> rechercherParNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le terme de recherche ne peut pas être vide");
        }
        try {
            return produitDAO.getByNom(nom);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche de produits", e);
        }
    }

    // Crée un produit (vérifie l'unicité du nom)
    public void creer(Produit produit) {
        if (produit.getNom() == null || produit.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
        
        List<Produit> existants = produitDAO.getByNom(produit.getNom());
        for (Produit p : existants) {
            if (p.getNom().equalsIgnoreCase(produit.getNom())) {
                throw new IllegalArgumentException("Un produit avec ce nom existe déjà : " + produit.getNom());
            }
        }

        try {
            produitDAO.save(produit);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du produit", e);
        }
    }

    // Modifie un produit existant
    public void modifier(Produit produit) {
        getById(produit.getIdProduit()); // Vérifie l'existence
        if (produit.getNom() == null || produit.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
        try {
            produitDAO.update(produit);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification du produit", e);
        }
    }

    // Supprime un produit
    public void supprimer(int id) {
        getById(id); // Vérifie l'existence
        try {
            produitDAO.delete(id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }

    // Retourne le nombre total de produits
    public int compter() {
        try {
            return produitDAO.count();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du comptage des produits", e);
        }
    }
}
