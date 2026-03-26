package service;

import model.Panier;
import model.LignePanier;
import dao.PanierDAO;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class PanierService {

    private final PanierDAO panierDAO;

    public PanierService() {
        this.panierDAO = new PanierDAO();
    }

    /**
     * Récupère le panier d'un client (charge depuis la BDD ou crée un nouveau).
     */
    public Panier recupererPanier(int idClient) {
        Panier panier = panierDAO.getPanierByClientId(idClient);
        if (panier == null) {
            int idPanier = panierDAO.createPanier(idClient);
            panier = new Panier(idPanier, idClient, 0.0, java.time.LocalDateTime.now());
        } else {
            panier.setLignes(panierDAO.getLignesByPanierId(panier.getIdPanier()));
            calculerTotal(panier);
        }
        return panier;
    }

    /**
     * Calcule le montant total du panier en interrogeant la base de données.
     */
    public BigDecimal calculerTotal(Panier panier) {
        BigDecimal total = panierDAO.getMontantTotal(panier.getIdPanier());
        panier.setTotal(total.doubleValue());
        return total;
    }

    /**
     * Ajoute un article au panier via une requête.
     * Paramètres attendus : "idClient" (Integer), "sku" (String), "quantite"
     * (Integer)
     */
    public shared.Reponse ajouter(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String sku = (String) params.get("sku");
        Integer quantite = (Integer) params.getOrDefault("quantite", 1);

        if (idClient == null || sku == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient ou sku.", null);
        }

        try {
            Panier panier = recupererPanier(idClient);

            // On gère la logique de mise à jour de la liste ici
            LignePanier ligneAEnregistrer = null;
            Optional<LignePanier> ligneExistante = panier.getLignes().stream()
                    .filter(l -> l.getSku().equals(sku))
                    .findFirst();

            if (ligneExistante.isPresent()) {
                ligneAEnregistrer = ligneExistante.get();
                ligneAEnregistrer.setQuantite(quantite); // ✅ REMPLACER la quantité, pas additionner
            } else {
                ligneAEnregistrer = new LignePanier(panier.getIdPanier(), sku, quantite, BigDecimal.ZERO);
                panier.getLignes().add(ligneAEnregistrer);
            }

            // Persistance
            panierDAO.ajouterOuMettreAJourLigne(ligneAEnregistrer);
            calculerTotal(panier);

            return new shared.Reponse(true, "Produit ajouté au panier.", null);
        } catch (Exception e) {
            return new shared.Reponse(false, "Stock ou produit introuvable, ou erreur interne : " + e.getMessage(),
                    null);
        }
    }

    /**
     * Supprime un article via une requête.
     * Paramètres attendus : "idClient" (Integer), "sku" (String)
     */
    public shared.Reponse supprimer(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String sku = (String) params.get("sku");

        if (idClient == null || sku == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient ou sku.", null);
        }

        try {
            Panier panier = recupererPanier(idClient);
            boolean removed = panier.getLignes().removeIf(l -> l.getSku().equals(sku));

            if (removed) {
                panierDAO.supprimerLigne(panier.getIdPanier(), sku);
                calculerTotal(panier);
                return new shared.Reponse(true, "Produit retiré du panier.", null);
            } else {
                return new shared.Reponse(false, "Produit non trouvé dans le panier.", null);
            }
        } catch (Exception e) {
            return new shared.Reponse(false, "Erreur lors de la suppression : " + e.getMessage(), null);
        }
    }

    /**
     * Vide le panier via une requête.
     * Paramètres attendus : "idClient" (Integer)
     */
    public shared.Reponse vider(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            Panier panier = recupererPanier(idClient);
            panier.getLignes().clear();
            panierDAO.viderPanier(panier.getIdPanier());
            panier.setTotal(0.0);
            return new shared.Reponse(true, "Panier vidé avec succès.", null);
        } catch (Exception e) {
            return new shared.Reponse(false, "Erreur lors du vidage du panier : " + e.getMessage(), null);
        }
    }

    /**
     * Affiche le contenu du panier via une requête.
     * Paramètres attendus : "idClient" (Integer)
     */
    public shared.Reponse afficher(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            Panier panier = recupererPanier(idClient);
            Map<String, Object> donnees = new java.util.HashMap<>();

            if (panier.getLignes() == null || panier.getLignes().isEmpty()) {
                donnees.put("lignes", java.util.Collections.emptyList());
                donnees.put("total", 0.0);
                return new shared.Reponse(true, "Le panier est vide.", donnees);
            }

            // Préparer les données pour le client
            java.util.List<Map<String, Object>> lignesMap = new java.util.ArrayList<>();
            for (LignePanier ligne : panier.getLignes()) {
                Map<String, Object> l = new java.util.HashMap<>();
                l.put("sku", ligne.getSku());
                l.put("quantite", ligne.getQuantite());
                l.put("image", ligne.getImage());
                // We use sousTotal because the LignePanier model does not store the unit price
                // directly
                l.put("sousTotal", ligne.getSousTotal() != null ? ligne.getSousTotal().doubleValue() : 0.0);
                lignesMap.add(l);
            }

            donnees.put("lignes", lignesMap);
            donnees.put("total", panier.getTotal());

            return new shared.Reponse(true, "Panier récupéré.", donnees);
        } catch (Exception e) {
            return new shared.Reponse(false, "Erreur lors de la récupération du panier : " + e.getMessage(), null);
        }
    }

    public shared.Reponse modifierQuantite(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String sku = (String) params.get("sku");
        Integer nouvelleQuantite = (Integer) params.get("quantite");

        if (idClient == null || sku == null || nouvelleQuantite == null) {
            return new shared.Reponse(false, "Paramètres manquants.", null);
        }

        try {
            Panier panier = recupererPanier(idClient);
            if (nouvelleQuantite <= 0) {
                panierDAO.supprimerLigne(panier.getIdPanier(), sku);
            } else {
                panierDAO.setLigneQuantite(panier.getIdPanier(), sku, nouvelleQuantite);
            }
            return new shared.Reponse(true, "Quantité mise à jour.", null);
        } catch (Exception e) {
            return new shared.Reponse(false, "Erreur : " + e.getMessage(), null);
        }
    }

    /**
     * Retourne les lignes brutes du panier pour le service commande.
     */
    public java.util.List<LignePanier> getLignesPanier(int idClient) {
        Panier panier = recupererPanier(idClient);
        return panier.getLignes();
    }

    public java.util.List<LignePanier> getLignesPanier(int idClient, java.util.List<String> skus) {
        Panier panier = recupererPanier(idClient);
        return panierDAO.getLignesParSkus(panier.getIdPanier(), skus);
    }

    public void supprimerLignes(int idClient, java.util.List<String> skus) {
        Panier panier = recupererPanier(idClient);
        panierDAO.supprimerLignesParSkus(panier.getIdPanier(), skus);
    }

    public void viderPanier(int idClient) {
        Panier panier = recupererPanier(idClient);
        panierDAO.viderPanier(panier.getIdPanier());
    }
}
