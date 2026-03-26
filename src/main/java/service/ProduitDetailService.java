package service;

import client.ClientSocket;
import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import util.ProduitVariantUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Service client pour récupérer et manipuler les détails complets d'un produit
 */
public class ProduitDetailService {
    
    /**
     * Récupère un produit complet avec ses variantes et SKU
     * @param idProduit ID du produit
     * @return Map contenant le produit organisé ou null en cas d'erreur
     */
    public static Map<String, Object> getProduitComplet(int idProduit) {
        System.out.println("[ProduitDetailService] getProduitComplet called with idProduit: " + idProduit);
        
        try {
            // Créer la requête
            Map<String, Object> params = new HashMap<>();
            params.put("idProduit", idProduit);
            
            Requete requete = new Requete(RequestType.GET_PRODUIT_COMPLET_AVEC_VARIANTES, params, null);
            
            // Envoyer la requête
            Reponse reponse = ClientSocket.getInstance().envoyer(requete);
            
            if (reponse != null && reponse.isSucces()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> produitBrut = (Map<String, Object>) reponse.getDonnees().get("produit");
                
                if (produitBrut != null) {
                    // Organiser les données pour l'affichage
                    return ProduitVariantUtils.creerStructureProduitOrganise(produitBrut);
                }
            } else {
                System.err.println("[ProduitDetailService] Erreur: " + 
                    (reponse != null ? reponse.getMessage() : "Réponse nulle"));
            }
            
        } catch (Exception e) {
            System.err.println("[ProduitDetailService] Exception lors de la récupération du produit: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Test la récupération d'un produit avec ses variantes
     * @param idProduit ID du produit à tester
     */
    public static void testGetProduitComplet(int idProduit) {
        System.out.println("=== Test Récupération Produit Complet ===");
        System.out.println("ID Produit: " + idProduit);
        
        Map<String, Object> produit = getProduitComplet(idProduit);
        
        if (produit != null) {
            System.out.println("Produit trouvé:");
            System.out.println("   Nom: " + produit.get("nomProduit"));
            System.out.println("   Description: " + produit.get("description"));
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> variantes = (Map<String, List<String>>) produit.get("variantesOrganisees");
            
            if (variantes != null && !variantes.isEmpty()) {
                System.out.println("   Variantes disponibles:");
                for (Map.Entry<String, List<String>> entry : variantes.entrySet()) {
                    System.out.println("     " + entry.getKey() + ": " + entry.getValue());
                }
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> skus = (Map<String, Map<String, Object>>) produit.get("skusOrganises");
            
            if (skus != null && !skus.isEmpty()) {
                System.out.println("   SKU disponibles (" + skus.size() + "):");
                for (Map.Entry<String, Map<String, Object>> entry : skus.entrySet()) {
                    Map<String, Object> sku = entry.getValue();
                    System.out.println("     " + entry.getKey() + 
                        " - Prix: " + sku.get("prix") + 
                        " - Stock: " + sku.get("quantite") + 
                        " - Image: " + sku.get("image"));
                }
            }
            
            if (produit.containsKey("prixMinimum")) {
                System.out.println("   Prix minimum: " + produit.get("prixMinimum"));
            }
            if (produit.containsKey("prixMaximum")) {
                System.out.println("   Prix maximum: " + produit.get("prixMaximum"));
            }
            if (produit.containsKey("stockTotal")) {
                System.out.println("   Stock total: " + produit.get("stockTotal"));
            }
            
        } else {
            System.out.println("Produit non trouvé ou erreur lors de la récupération");
        }
        
        System.out.println("=== Fin Test ===\n");
    }
    
    /**
     * Exemple d'utilisation pour une interface utilisateur
     * @param idProduit ID du produit
     * @return Structure prête pour l'affichage dans une UI
     */
    public static Map<String, Object> preparerDonneesPourUI(int idProduit) {
        Map<String, Object> produit = getProduitComplet(idProduit);
        
        if (produit == null) {
            return null;
        }
        
        // Préparer les sélections par défaut
        Map<String, String> selections = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> variantes = (Map<String, List<String>>) produit.get("variantesOrganisees");
        
        if (variantes != null) {
            for (Map.Entry<String, List<String>> entry : variantes.entrySet()) {
                List<String> valeurs = entry.getValue();
                if (!valeurs.isEmpty()) {
                    selections.put(entry.getKey(), valeurs.get(0)); // Première valeur par défaut
                }
            }
        }
        
        // Trouver le SKU correspondant aux sélections par défaut
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> skus = (Map<String, Map<String, Object>>) produit.get("skusOrganises");
        Map<String, Object> skuActuel = null;
        
        if (skus != null) {
            skuActuel = ProduitVariantUtils.trouverSkuPourVariantes(skus, selections);
            
            // Si aucun SKU ne correspond, prendre le premier
            if (skuActuel == null && !skus.isEmpty()) {
                skuActuel = skus.values().iterator().next();
            }
        }
        
        // Créer la structure pour l'UI
        Map<String, Object> uiData = new HashMap<>();
        uiData.put("produit", produit);
        uiData.put("variantes", variantes);
        uiData.put("skus", skus);
        uiData.put("selections", selections);
        uiData.put("skuActuel", skuActuel);
        
        return uiData;
    }
    
    /**
     * Met à jour le SKU actuel en fonction des nouvelles sélections de variantes
     * @param uiData Données actuelles de l'UI
     * @param nomVariante Nom de la variante modifiée
     * @param nouvelleValeur Nouvelle valeur sélectionnée
     * @return Le nouveau SKU ou null si non trouvé
     */
    public static Map<String, Object> mettreAJourSku(
            Map<String, Object> uiData, 
            String nomVariante, 
            String nouvelleValeur) {
        
        @SuppressWarnings("unchecked")
        Map<String, String> selections = (Map<String, String>) uiData.get("selections");
        
        if (selections != null) {
            selections.put(nomVariante, nouvelleValeur);
            
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> skus = (Map<String, Map<String, Object>>) uiData.get("skus");
            
            if (skus != null) {
                Map<String, Object> nouveauSku = ProduitVariantUtils.trouverSkuPourVariantes(skus, selections);
                
                if (nouveauSku != null) {
                    uiData.put("skuActuel", nouveauSku);
                    return nouveauSku;
                }
            }
        }
        
        return null;
    }
}
