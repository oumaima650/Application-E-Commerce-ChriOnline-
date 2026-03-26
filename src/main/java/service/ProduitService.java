package service;

import dao.ProduitDAO;
import model.Produit;
import shared.Reponse;
import shared.Requete;
import java.util.List;
import java.util.Map;

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

    /**
     * Récupère un produit complet avec toutes ses variantes et SKU
     * @param idProduit ID du produit
     * @return Map contenant les informations complètes du produit
     */
    public Map<String, Object> getProduitCompletAvecVariantes(int idProduit) {
        try {
            Map<String, Object> produit = produitDAO.getProduitCompletAvecVariantes(idProduit);
            if (produit == null) {
                throw new IllegalArgumentException("Produit introuvable ou sans SKU id=" + idProduit);
            }
            return produit;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération du produit complet id=" + idProduit, e);
        }
    }

    // ───────────────────────────────
    // WRAPPERS REQUETE / REPONSE
    // ───────────────────────────────

    public Reponse getAll(Requete requete) {
        try {
            return new Reponse(true, "Liste des produits récupérée.", Map.of("produits", getAll()));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse getById(Requete requete) {
        try {
            Integer id = (Integer) requete.getParametres().get("idProduit");
            if (id == null) return new Reponse(false, "ID Produit manquant.", null);
            return new Reponse(true, "Produit trouvé.", Map.of("produit", getById(id)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse rechercherParNom(Requete requete) {
        try {
            String nom = (String) requete.getParametres().get("nom");
            if (nom == null) return new Reponse(false, "Nom de recherche manquant.", null);
            return new Reponse(true, "Résultats de recherche.", Map.of("produits", rechercherParNom(nom)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse compter(Requete requete) {
        try {
            return new Reponse(true, "Nombre de produits.", Map.of("count", compter()));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse getProduitCompletAvecVariantes(Requete requete) {
        try {
            Integer id = (Integer) requete.getParametres().get("idProduit");
            System.out.println("[ProduitService] getProduitCompletAvecVariantes received idProduit: " + id);
            if (id == null) {
                System.out.println("[ProduitService] ERROR: idProduit is null in request parameters");
                return new Reponse(false, "ID Produit manquant.", null);
            }
            return new Reponse(true, "Produit complet récupéré.", Map.of("produit", getProduitCompletAvecVariantes(id)));
        } catch (Exception e) {
            System.err.println("[ProduitService] Exception: " + e.getMessage());
            e.printStackTrace();
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse creer(Requete requete) {
        try {
            Produit p = (Produit) requete.getParametres().get("produit");
            if (p == null) return new Reponse(false, "Données du produit manquantes.", null);
            creer(p);
            return new Reponse(true, "Produit créé avec succès.", Map.of("produit", p));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse modifier(Requete requete) {
        try {
            Produit p = (Produit) requete.getParametres().get("produit");
            if (p == null) return new Reponse(false, "Données du produit manquantes.", null);
            modifier(p);
            return new Reponse(true, "Produit modifié avec succès.", Map.of("produit", p));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse supprimer(Requete requete) {
        try {
            Integer id = (Integer) requete.getParametres().get("idProduit");
            if (id == null) return new Reponse(false, "ID Produit manquant.", null);
            supprimer(id);
            return new Reponse(true, "Produit supprimé avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
}
