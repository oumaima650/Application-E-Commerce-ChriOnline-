package service;

import dao.SKUDAO;
import model.SKU;
import shared.Reponse;
import shared.Requete;
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


    public Reponse getAll(Requete requete) {
        try {
            return new Reponse(true, "Liste des SKUs récupérée.", java.util.Map.of("skus", getAll()));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse getBySku(Requete requete) {
        try {
            Object skuParam = requete.getParametres().get("sku");
            if (!(skuParam instanceof String)) {
                return new Reponse(false, "Type de données invalide (sku doit être String).", null);
            }
            return new Reponse(true, "SKU trouvé.", java.util.Map.of("sku", getBySku((String) skuParam)));
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
            return new Reponse(true, "Liste des SKUs pour le produit.", java.util.Map.of("skus", getByProduit((Integer) idParam)));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse creer(Requete requete) {
        try {
            Object skuObj = requete.getParametres().get("sku");
            if (!(skuObj instanceof SKU)) {
                return new Reponse(false, "Type de données invalide (sku doit être SKU).", null);
            }
            SKU s = (SKU) skuObj;
            creer(s);
            return new Reponse(true, "SKU créé avec succès.", java.util.Map.of("sku", s));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse modifier(Requete requete) {
        try {
            Object skuObj = requete.getParametres().get("sku");
            if (!(skuObj instanceof SKU)) {
                return new Reponse(false, "Type de données invalide (sku doit être SKU).", null);
            }
            SKU s = (SKU) skuObj;
            modifier(s);
            return new Reponse(true, "SKU modifié avec succès.", java.util.Map.of("sku", s));
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse supprimer(Requete requete) {
        try {
            Object skuParam = requete.getParametres().get("sku");
            if (!(skuParam instanceof String)) {
                return new Reponse(false, "Type de données invalide (sku doit être String).", null);
            }
            supprimer((String) skuParam);
            return new Reponse(true, "SKU supprimé avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse ajouterValeur(Requete requete) {
        try {
            Object skuParam = requete.getParametres().get("sku");
            Object pvvParam = requete.getParametres().get("idPVV");
            if (!(skuParam instanceof String) || !(pvvParam instanceof Integer)) {
                return new Reponse(false, "Types de données invalides (sku: String, idPVV: Integer).", null);
            }
            ajouterValeur((String) skuParam, (Integer) pvvParam);
            return new Reponse(true, "Valeur ajoutée au SKU.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }

    public Reponse retirerValeur(Requete requete) {
        try {
            Object skuParam = requete.getParametres().get("sku");
            Object pvvParam = requete.getParametres().get("idPVV");
            if (!(skuParam instanceof String) || !(pvvParam instanceof Integer)) {
                return new Reponse(false, "Types de données invalides (sku: String, idPVV: Integer).", null);
            }
            retirerValeur((String) skuParam, (Integer) pvvParam);
            return new Reponse(true, "Valeur retirée du SKU.", null);
        } catch (Exception e) {
            return new Reponse(false, e.getMessage(), null);
        }
    }
}
