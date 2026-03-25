package service;

import dao.CategorieDAO;
import model.Categorie;
import shared.Reponse;
import shared.Requete;
import java.util.List;
import java.util.Map;
import service.VarianteService;

public class CategorieService {

    private CategorieDAO categorieDAO = new CategorieDAO();
    private final VarianteService varianteService = new VarianteService();

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

    // ───────────────────────────────
    // WRAPPERS REQUETE / REPONSE
    // ───────────────────────────────

    public Reponse getAll(Requete requete) {
        try {
            return new Reponse(true, "Liste des catégories récupérée.", Map.of("categories", getAll()));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse getById(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idCategorie");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idCategorie doit être Integer).", null);
            }
            Integer id = (Integer) idParam;
            return new Reponse(true, "Catégorie trouvée.", Map.of("categorie", getById(id)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse creer(Requete requete) {
        try {
            Object catParam = requete.getParametres().get("categorie");
            if (!(catParam instanceof Categorie)) {
                return new Reponse(false, "Type de données invalide (categorie doit être Categorie).", null);
            }
            Categorie cat = (Categorie) catParam;
            creer(cat); // Le DAO injecte l'ID généré dans cat
            return new Reponse(true, "Catégorie créée avec succès.", Map.of("categorie", cat));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse modifier(Requete requete) {
        try {
            Object catParam = requete.getParametres().get("categorie");
            if (!(catParam instanceof Categorie)) {
                return new Reponse(false, "Type de données invalide (categorie doit être Categorie).", null);
            }
            Categorie cat = (Categorie) catParam;
            modifier(cat);
            return new Reponse(true, "Catégorie modifiée avec succès.", Map.of("categorie", cat));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse supprimer(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idCategorie");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idCategorie doit être Integer).", null);
            }
            Integer id = (Integer) idParam;
            supprimer(id);
            return new Reponse(true, "Catégorie supprimée avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse ajouterVariante(Requete requete) {
        try {
            Object idCatParam = requete.getParametres().get("idCategorie");
            Object idVarParam = requete.getParametres().get("idVariante");
            
            if (!(idCatParam instanceof Integer) || !(idVarParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idCategorie et idVariante doivent être Integer).", null);
            }
            
            Integer idCategorie = (Integer) idCatParam;
            Integer idVariante = (Integer) idVarParam;
            
            // Vérification existence variante
            varianteService.getById(idVariante);
            
            ajouterVariante(idCategorie, idVariante);
            return new Reponse(true, "Variante liée à la catégorie avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse retirerVariante(Requete requete) {
        try {
            Object idCatParam = requete.getParametres().get("idCategorie");
            Object idVarParam = requete.getParametres().get("idVariante");
            
            if (!(idCatParam instanceof Integer) || !(idVarParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idCategorie et idVariante doivent être Integer).", null);
            }
            
            Integer idCategorie = (Integer) idCatParam;
            Integer idVariante = (Integer) idVarParam;
            
            retirerVariante(idCategorie, idVariante);
            return new Reponse(true, "Variante retirée de la catégorie avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
}
