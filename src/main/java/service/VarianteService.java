package service;

import dao.VarianteDAO;
import model.Variante;
import shared.Reponse;
import shared.Requete;
import java.util.List;
import java.util.Map;

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
 
    // Récupère les variantes par produit (via ses PVVs)
    public List<Variante> getByProduit(int idProduit) {
        try {
            return varianteDAO.getByProduit(idProduit);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des variantes pour le produit id=" + idProduit, e);
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
 
    // ───────────────────────────────
    // WRAPPERS REQUETE / REPONSE
    // ───────────────────────────────
 
    public Reponse getAll(Requete requete) {
        try {
            return new Reponse(true, "Liste des variantes récupérée.", Map.of("variantes", getAll()));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse getById(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idVariante");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idVariante doit être Integer).", null);
            }
            return new Reponse(true, "Variante trouvée.", Map.of("variante", getById((Integer) idParam)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse getByCategorie(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idCategorie");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idCategorie doit être Integer).", null);
            }
            return new Reponse(true, "Variantes de la catégorie récupérées.", Map.of("variantes", getByCategorie((Integer) idParam)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse getByProduit(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idProduit");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idProduit doit être Integer).", null);
            }
            return new Reponse(true, "Variantes du produit récupérées.", Map.of("variantes", getByProduit((Integer) idParam)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse creer(Requete requete) {
        try {
            Object varParam = requete.getParametres().get("variante");
            if (!(varParam instanceof Variante)) {
                return new Reponse(false, "Type de données invalide (variante doit être Variante).", null);
            }
            Variante var = (Variante) varParam;
            creer(var);
            return new Reponse(true, "Variante créée avec succès.", Map.of("variante", var));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse modifier(Requete requete) {
        try {
            Object varParam = requete.getParametres().get("variante");
            if (!(varParam instanceof Variante)) {
                return new Reponse(false, "Type de données invalide (variante doit être Variante).", null);
            }
            Variante var = (Variante) varParam;
            modifier(var);
            return new Reponse(true, "Variante modifiée avec succès.", Map.of("variante", var));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse supprimer(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idVariante");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idVariante doit être Integer).", null);
            }
            supprimer((Integer) idParam);
            return new Reponse(true, "Variante supprimée avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
}
