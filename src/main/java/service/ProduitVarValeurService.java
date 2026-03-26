package service;

import dao.ProduitVarValeurDAO;
import model.ProduitVarValeur;
import shared.Reponse;
import shared.Requete;
import java.util.List;
import java.util.Map;

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

 
    public Reponse getAll(Requete requete) {
        try {
            return new Reponse(true, "Liste des PVV récupérée.", Map.of("pvvs", getAll()));
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
            return new Reponse(true, "Liste des PVV pour le produit.", Map.of("pvvs", getByProduit((Integer) idParam)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse creer(Requete requete) {
        try {
            Object pvvParam = requete.getParametres().get("pvv");
            if (!(pvvParam instanceof ProduitVarValeur)) {
                return new Reponse(false, "Type de données invalide (pvv doit être ProduitVarValeur).", null);
            }
            ProduitVarValeur pvv = (ProduitVarValeur) pvvParam;
            creer(pvv);
            return new Reponse(true, "PVV créé avec succès.", Map.of("pvv", pvv));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse modifier(Requete requete) {
        try {
            Object pvvParam = requete.getParametres().get("pvv");
            if (!(pvvParam instanceof ProduitVarValeur)) {
                return new Reponse(false, "Type de données invalide (pvv doit être ProduitVarValeur).", null);
            }
            ProduitVarValeur pvv = (ProduitVarValeur) pvvParam;
            modifier(pvv);
            return new Reponse(true, "PVV modifié avec succès.", Map.of("pvv", pvv));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
 
    public Reponse supprimer(Requete requete) {
        try {
            Object idParam = requete.getParametres().get("idPVV");
            if (!(idParam instanceof Integer)) {
                return new Reponse(false, "Type de données invalide (idPVV doit être Integer).", null);
            }
            supprimer((Integer) idParam);
            return new Reponse(true, "PVV supprimé avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
}
