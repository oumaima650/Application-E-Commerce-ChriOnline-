package service;

import dao.VarianteDAO;
import model.Variante;
import java.util.List;

public class VarianteService {

    private VarianteDAO varianteDAO = new VarianteDAO();

    // Récupère toutes les variantes
    public List<Variante> getAll() {
        try {
            return varianteDAO.getAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des variantes", e);
        }
    }

    // Récupère une variante par ID
    public Variante getById(int id) {
        Variante v = varianteDAO.getById(id);
        if (v == null) {
            throw new IllegalArgumentException("Variante introuvable id=" + id);
        }
        return v;
    }

    // Récupère les variantes par catégorie
    public List<Variante> getByCategorie(int idCategorie) {
        try {
            return varianteDAO.getByCategorie(idCategorie);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des variantes pour la catégorie id=" + idCategorie, e);
        }
    }

    // Crée une variante
    public void creer(Variante var) {
        if (var.getNom() == null || var.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la variante est obligatoire");
        }
        try {
            varianteDAO.save(var);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la variante", e);
        }
    }

    // Modifie une variante
    public void modifier(Variante var) {
        getById(var.getIdVariante()); // Vérifie l'existence
        if (var.getNom() == null || var.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la variante est obligatoire");
        }
        try {
            varianteDAO.update(var);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification de la variante", e);
        }
    }

    // Supprime une variante
    public void supprimer(int id) {
        getById(id); // Vérifie l'existence
        try {
            varianteDAO.delete(id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de la variante", e);
        }
    }
}
