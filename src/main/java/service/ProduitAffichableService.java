package service;

import dao.ProduitDAO;
import dao.SKUDAO;
import dao.CategorieDAO;
import model.ProduitAffichable;
import model.Produit;
import model.SKU;
import model.Categorie;
import shared.Reponse;
import shared.Requete;

import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * Service pour récupérer les produits avec toutes les informations nécessaires pour l'affichage
 * Combine les données de Produit + SKU + Categorie
 */
public class ProduitAffichableService {

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final SKUDAO skuDAO = new SKUDAO();
    private final CategorieDAO categorieDAO = new CategorieDAO();

    /**
     * Récupère tous les produits avec leurs SKU et catégories
     * Retourne une liste de ProduitAffichable
     */
    public List<ProduitAffichable> getAllProduitsAffichables() {
        try {
            System.out.println("[ProduitAffichableService] Début de récupération des produits affichables...");
            List<ProduitAffichable> result = new ArrayList<>();
            
            // Récupérer tous les produits
            System.out.println("[ProduitAffichableService] Récupération des produits de base...");
            List<Produit> produits = produitDAO.getAll();
            System.out.println("[ProduitAffichableService] " + produits.size() + " produits bruts récupérés");
            
            for (Produit produit : produits) {
                System.out.println("[ProduitAffichableService] Traitement produit: " + produit.getNom() + " (ID: " + produit.getIdProduit() + ")");
                
                // Pour chaque produit, récupérer le premier SKU disponible
                List<SKU> skus = skuDAO.getByProduit(produit.getIdProduit());
                System.out.println("[ProduitAffichableService] " + skus.size() + " SKU trouvés pour ce produit");
                
                if (!skus.isEmpty()) {
                    // Prendre le premier SKU (ou le moins cher, ou celui avec le plus de stock)
                    SKU sku = skus.get(0); // Pour l'instant, on prend le premier
                    System.out.println("[ProduitAffichableService] SKU sélectionné: " + sku.getSku() + " (Prix: " + sku.getPrix() + ")");
                    
                    // Récupérer la catégorie du produit
                    Categorie categorie = getCategorieParId(produit.getIdCategorie());
                    System.out.println("[ProduitAffichableService] Catégorie: " + (categorie != null ? categorie.getNom() : "NULL"));
                    
                    // Créer le produit affichable
                    ProduitAffichable prodAff = new ProduitAffichable(
                        produit.getIdProduit(),
                        produit.getNom(),
                        produit.getDescription(),
                        sku.getSku(),
                        sku.getPrix(),
                        sku.getQuantite(),
                        sku.getImage(),
                        categorie != null ? categorie.getNom() : "Non catégorisé",
                        categorie != null ? categorie.getIdCategorie() : 0
                    );
                    
                    // Notes et avis simulés pour la démo
                    prodAff.setNoteMoyenne(4.0 + (produit.getIdProduit() % 2)); // 4.0 ou 5.0
                    prodAff.setNombreAvis(10 + (produit.getIdProduit() % 50)); // 10-59 avis
                    prodAff.setCreatedAt(produit.getCreatedAt());
                    
                    // Ajouter une promotion fictive pour démo (20% de réduction sur certains produits)
                    if (produit.getIdProduit() % 3 == 0) { // 1 produit sur 3 en promotion
                        BigDecimal prixPromo = sku.getPrix().multiply(new BigDecimal("0.8"));
                        prodAff.appliquerPromotion(prixPromo);
                        System.out.println("[ProduitAffichableService] PROMOTION appliquée: " + prixPromo);
                    }
                    
                    result.add(prodAff);
                    System.out.println("[ProduitAffichableService] Produit affichable créé: " + prodAff.getNom());
                } else {
                    System.out.println("[ProduitAffichableService] ATTENTION: Aucun SKU trouvé pour le produit " + produit.getNom());
                }
            }
            
            System.out.println("[ProduitAffichableService] Total produits affichables créés: " + result.size());
            return result;
            
        } catch (Exception e) {
            System.err.println("[ProduitAffichableService] Erreur lors de la récupération des produits affichables: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des produits affichables", e);
        }
    }

    /**
     * Récupère la catégorie d'un produit par son ID
     */
    private Categorie getCategorieParId(int idCategorie) {
        if (idCategorie <= 0) return null;
        try {
            return categorieDAO.getById(idCategorie);
        } catch (Exception e) {
            System.err.println("[ProduitAffichableService] Erreur lors de la récupération de la catégorie " + idCategorie + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Recherche des produits par nom avec leurs informations complètes
     */
    public List<ProduitAffichable> rechercherProduitsAffichables(String nom) {
        try {
            List<Produit> produits = produitDAO.getByNom(nom);
            List<ProduitAffichable> result = new ArrayList<>();
            
            for (Produit produit : produits) {
                List<SKU> skus = skuDAO.getByProduit(produit.getIdProduit());
                
                if (!skus.isEmpty()) {
                    SKU sku = skus.get(0);
                    Categorie categorie = getCategorieParId(produit.getIdCategorie());
                    
                    ProduitAffichable prodAff = new ProduitAffichable(
                        produit.getIdProduit(),
                        produit.getNom(),
                        produit.getDescription(),
                        sku.getSku(),
                        sku.getPrix(),
                        sku.getQuantite(),
                        sku.getImage(),
                        categorie != null ? categorie.getNom() : "Non catégorisé",
                        categorie != null ? categorie.getIdCategorie() : 0
                    );
                    
                    prodAff.setNoteMoyenne(4.5);
                    prodAff.setNombreAvis(25);
                    
                    result.add(prodAff);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche de produits affichables", e);
        }
    }

    /**
     * Récupère les produits par catégorie
     */
    public List<ProduitAffichable> getProduitsParCategorie(String nomCategorie) {
        try {
            List<ProduitAffichable> tousLesProduits = getAllProduitsAffichables();
            
            return tousLesProduits.stream()
                    .filter(p -> p.getCategorie().equalsIgnoreCase(nomCategorie))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des produits par catégorie", e);
        }
    }

    /**
     * Wrapper pour la requête GET_ALL_PRODUITS_AFFICHABLES
     */
    public Reponse getAll(Requete requete) {
        try {
            List<ProduitAffichable> produits = getAllProduitsAffichables();
            return new Reponse(true, "Produits affichables récupérés avec succès", 
                             Map.of("produits", produits));
        } catch (Exception e) {
            return new Reponse(false, "Erreur: " + e.getMessage(), null);
        }
    }

    /**
     * Wrapper pour la recherche
     */
    public Reponse rechercher(Requete requete) {
        try {
            String nom = (String) requete.getParametres().get("nom");
            if (nom == null) {
                return new Reponse(false, "Nom de recherche manquant", null);
            }
            
            List<ProduitAffichable> produits = rechercherProduitsAffichables(nom);
            return new Reponse(true, "Résultats de recherche", 
                             Map.of("produits", produits));
        } catch (Exception e) {
            return new Reponse(false, "Erreur: " + e.getMessage(), null);
        }
    }

    /**
     * Wrapper pour la recherche par catégorie
     */
    public Reponse getByCategorie(Requete requete) {
        try {
            String categorie = (String) requete.getParametres().get("categorie");
            if (categorie == null) {
                return new Reponse(false, "Catégorie manquante", null);
            }
            
            List<ProduitAffichable> produits = getProduitsParCategorie(categorie);
            return new Reponse(true, "Produits de la catégorie " + categorie, 
                             Map.of("produits", produits));
        } catch (Exception e) {
            return new Reponse(false, "Erreur: " + e.getMessage(), null);
        }
    }
}
