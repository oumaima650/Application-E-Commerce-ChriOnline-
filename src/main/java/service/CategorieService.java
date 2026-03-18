package service;

import dao.CategorieDAO;
import model.Categorie;
import java.util.List;

public class CategorieService {

    private CategorieDAO categorieDAO = new CategorieDAO();

    // Récupère toutes les catégories
    public List<Categorie> getAll() {
        try {
            return categorieDAO.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des catégories", e);
        }
    }

    // Récupère une catégorie par son ID, lance une exception si introuvable
    public Categorie getById(int id) {
        Categorie cat = categorieDAO.getById(id);
        if (cat == null) {
            throw new IllegalArgumentException("Catégorie introuvable id=" + id);
        }
        return cat;
    }

    // Crée une nouvelle catégorie après validation
    public void creer(Categorie cat) {
        if (cat.getNom() == null || cat.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        try {
            categorieDAO.save(cat);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la catégorie", e);
        }
    }

    // Modifie une catégorie existante
    public void modifier(Categorie cat) {
        getById(cat.getIdCategorie()); // Vérifie l'existence
        if (cat.getNom() == null || cat.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        try {
            categorieDAO.update(cat);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification de la catégorie", e);
        }
    }

    // Supprime une catégorie par son ID
    public void supprimer(int id) {
        getById(id); // Vérifie l'existence
        try {
            categorieDAO.delete(id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de la catégorie", e);
        }
    }

    // Lie une variante à une catégorie
    public void ajouterVariante(int idCategorie, int idVariante) {
        getById(idCategorie); // Vérifie l'existence de la catégorie
        // Note: Idéalement vérifier l'existence de la variante aussi via VarianteService
        try {
            categorieDAO.addVariante(idCategorie, idVariante);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'ajout de la variante à la catégorie", e);
        }
    }

    // Retire le lien entre une variante et une catégorie
    public void retirerVariante(int idCategorie, int idVariante) {
        getById(idCategorie); // Vérifie l'existence
        try {
            categorieDAO.removeVariante(idCategorie, idVariante);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du retrait de la variante de la catégorie", e);
        }
    }
}
