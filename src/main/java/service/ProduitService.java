package service;

import dao.ProduitDAO;
import model.Produit;
import shared.Reponse;
import shared.Requete;
import java.util.List;
import java.util.Map;

public class ProduitService {

    private ProduitDAO produitDAO = new ProduitDAO();
    private dao.ProduitVarValeurDAO pvvDAO = new dao.ProduitVarValeurDAO();
    private dao.SKUDAO skuDAO = new dao.SKUDAO();

    /**
     * Crée un produit COMPLET (Produit + Variantes + SKUs) dans une seule transaction.
     */
    public synchronized Reponse creerProduitComplet(Requete requete) {
        java.sql.Connection conn = dao.ConnexionBDD.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. Extraire les données
            Produit produit = (Produit) requete.getParametres().get("produit");
            List<Map<String, Object>> variantsData = (List<Map<String, Object>>) requete.getParametres().get("variantsData");
            List<Map<String, Object>> skusData = (List<Map<String, Object>>) requete.getParametres().get("skusData");

            if (produit == null || variantsData == null || skusData == null) {
                return new Reponse(false, "Données incomplètes pour la création du produit.", null);
            }

            // 2. Sauvegarder le Produit
            produitDAO.save(produit); // L'ID est injecté dans produit par le DAO
            int idProduit = produit.getIdProduit();

            // 3. Sauvegarder les ProduitVarValeur (PVV)
            // On garde une map pour retrouver l'ID PVV par (idVariante, valeurString)
            Map<String, Integer> pvvIds = new java.util.HashMap<>();
            for (Map<String, Object> vData : variantsData) {
                int idVariante = (int) vData.get("idVariante");
                List<String> values = (List<String>) vData.get("values");
                for (String valStr : values) {
                    model.ProduitVarValeur pvv = new model.ProduitVarValeur();
                    pvv.setIdProduit(idProduit);
                    pvv.setIdVariante(idVariante);
                    pvv.setValeur(valStr);
                    pvvDAO.save(pvv);
                    pvvIds.put(idVariante + ":" + valStr, pvv.getIdPVV());
                }
            }

            // 4. Sauvegarder les SKUs
            for (Map<String, Object> sData : skusData) {
                model.SKU sku = new model.SKU();
                sku.setSku((String) sData.get("sku"));
                sku.setPrix(new java.math.BigDecimal(sData.get("price").toString()));
                sku.setQuantite((int) sData.get("quantity"));
                sku.setImage((String) sData.get("imageUrl"));

                skuDAO.save(sku);

                // Lier aux PVV
                Map<Integer, String> combinations = (Map<Integer, String>) sData.get("combinations");
                for (Map.Entry<Integer, String> entry : combinations.entrySet()) {
                    int idVariant = entry.getKey();
                    String valStr = entry.getValue();
                    Integer idPVV = pvvIds.get(idVariant + ":" + valStr);
                    if (idPVV != null) {
                        skuDAO.addValeur(sku.getSku(), idPVV);
                    }
                }
            }

            conn.commit();
            return new Reponse(true, "Produit créé avec succès avec ses variantes et SKUs.", Map.of("idProduit", idProduit));

        } catch (Exception e) {
            try { conn.rollback(); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la création complète: " + e.getMessage(), null);
        } finally {
            try { conn.setAutoCommit(true); } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
        }
    }

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

    public Reponse adminGetAll(Requete requete) {
        try {
            String query = (String) requete.getParametres().get("query");
            List<Produit> produits;
            if (query != null && !query.trim().isEmpty()) {
                produits = produitDAO.getByNomIncludeDeleted(query);
            } else {
                produits = produitDAO.getAllIncludeDeleted();
            }
            return new Reponse(true, "Liste des produits (admin).", Map.of("produits", produits));
        } catch (Exception e) {
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
            
            Produit p = produitDAO.getById(id);
            if (p == null) return new Reponse(false, "Produit inexistant.", null);
            
            boolean wasDeleted = p.getDeletedAt() != null;
            if (wasDeleted) {
                produitDAO.restore(id);
                return new Reponse(true, "Produit restauré avec succès.", null);
            } else {
                produitDAO.delete(id);
                return new Reponse(true, "Produit supprimé avec succès.", null);
            }
        } catch (Exception e) {
            return new Reponse(false, "Erreur lors de la suppression/restauration: " + e.getMessage(), null);
        }
    }

    public Reponse adminUpdateProduct(Requete requete) {
        try {
            Object idObj = requete.getParametres().get("idProduit");
            Object nomObj = requete.getParametres().get("nom");
            Object descObj = requete.getParametres().get("description");
            
            if (idObj == null) {
                return new Reponse(false, "ERREUR: ID Produit (idProduit) manquant dans la requête.", null);
            }
            if (nomObj == null) {
                return new Reponse(false, "ERREUR: Nom du produit (nom) manquant dans la requête.", null);
            }
            
            Integer id = (idObj instanceof Number) ? ((Number) idObj).intValue() : Integer.parseInt(idObj.toString());
            String nom = nomObj.toString();
            String desc = (descObj != null) ? descObj.toString() : "";
            
            Produit p = new Produit();
            p.setIdProduit(id);
            p.setNom(nom);
            p.setDescription(desc);
            
            modifier(p); // Calls internal modifier(Produit p)
            return new Reponse(true, "Produit mis à jour avec succès.", null);
        } catch (Exception e) {
            return new Reponse(false, "EXCEP adminUpdateProduct: " + e.getMessage(), null);
        }
    }
}
