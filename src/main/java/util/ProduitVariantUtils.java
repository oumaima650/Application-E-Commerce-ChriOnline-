package util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilitaire pour organiser les données de variantes de produits
 */
public class ProduitVariantUtils {
    
    /**
     * Organise les variantes par nom (ex: Couleur, Taille, Stockage)
     * @param variantes Liste des variantes brutes
     * @return Map organisée par nom de variante avec liste de valeurs uniques
     */
    public static Map<String, List<String>> organiserVariantesParNom(List<Map<String, Object>> variantes) {
        Map<String, Set<String>> variantesSet = new HashMap<>();
        
        for (Map<String, Object> variante : variantes) {
            String nomVariante = (String) variante.get("nomVariante");
            String valeur = (String) variante.get("valeur");
            
            if (nomVariante != null && valeur != null) {
                variantesSet.computeIfAbsent(nomVariante, k -> new HashSet<>()).add(valeur);
            }
        }
        
        // Convertir les Set en List pour l'affichage
        return variantesSet.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new ArrayList<>(entry.getValue()).stream()
                            .sorted() // Trier alphabétiquement
                            .collect(Collectors.toList())
                ));
    }
    
    /**
     * Organise les SKU par code
     * @param skus Liste des SKU
     * @return Map avec SKU code comme clé et détails comme valeur
     */
    public static Map<String, Map<String, Object>> organiserSkusParCode(List<Map<String, Object>> skus) {
        Map<String, Map<String, Object>> skusMap = new HashMap<>();
        
        for (Map<String, Object> sku : skus) {
            String codeSku = (String) sku.get("SKU");
            if (codeSku != null) {
                skusMap.put(codeSku, sku);
            }
        }
        
        return skusMap;
    }
    
    /**
     * Trouve le SKU correspondant à une combinaison de variantes
     * @param skusMap Map des SKU organisés par code
     * @param variantesSelectionnees Map des variantes sélectionnées (nom -> valeur)
     * @return Le SKU correspondant ou null
     */
    public static Map<String, Object> trouverSkuPourVariantes(
            Map<String, Map<String, Object>> skusMap,
            Map<String, String> variantesSelectionnees) {
        
        for (Map<String, Object> sku : skusMap.values()) {
            if (skuCorrespondAuxVariantes(sku, variantesSelectionnees)) {
                return sku;
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie si un SKU correspond aux variantes sélectionnées
     * @param sku Le SKU à vérifier
     * @param variantesSelectionnees Les variantes sélectionnées
     * @return true si le SKU correspond exactement
     */
    private static boolean skuCorrespondAuxVariantes(
            Map<String, Object> sku, 
            Map<String, String> variantesSelectionnees) {
        
        @SuppressWarnings("unchecked")
        Map<String, String> skuVariantes = (Map<String, String>) sku.get("variantes");
        
        if (skuVariantes == null) return false;
        
        for (Map.Entry<String, String> entry : variantesSelectionnees.entrySet()) {
            String selectedVal = entry.getValue();
            String skuVal = skuVariantes.get(entry.getKey());
            if (skuVal == null || !skuVal.equals(selectedVal)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Crée une structure de produit complète et organisée
     * @param produitComplet Le produit brut du DAO
     * @return Structure organisée pour l'affichage
     */
    public static Map<String, Object> creerStructureProduitOrganise(Map<String, Object> produitComplet) {
        if (produitComplet == null) return null;
        
        Map<String, Object> organise = new HashMap<>();
        
        // Informations de base du produit
        organise.put("idProduit", produitComplet.get("idProduit"));
        organise.put("nomProduit", produitComplet.get("nomProduit"));
        organise.put("description", produitComplet.get("description"));
        
        // Organiser les variantes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> variantes = (List<Map<String, Object>>) produitComplet.get("variantes");
        if (variantes != null) {
            organise.put("variantesOrganisees", organiserVariantesParNom(variantes));
            organise.put("variantesBrutes", variantes);
        }
        
        // Organiser les SKU
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skus = (List<Map<String, Object>>) produitComplet.get("skus");
        if (skus != null) {
            organise.put("skusOrganises", organiserSkusParCode(skus));
            organise.put("skusBruts", skus);
            
            // Trouver le prix minimum et maximum
            Optional<Double> minPrice = skus.stream()
                    .map(sku -> ((Number) sku.get("prix")).doubleValue())
                    .min(Double::compareTo);
                    
            Optional<Double> maxPrice = skus.stream()
                    .map(sku -> ((Number) sku.get("prix")).doubleValue())
                    .max(Double::compareTo);
            
            minPrice.ifPresent(price -> organise.put("prixMinimum", price));
            maxPrice.ifPresent(price -> organise.put("prixMaximum", price));
            
            // Stock total
            int stockTotal = skus.stream()
                    .mapToInt(sku -> (Integer) sku.get("quantite"))
                    .sum();
            organise.put("stockTotal", stockTotal);
        }
        
        return organise;
    }
    
    /**
     * Extrait les valeurs uniques pour une variante spécifique
     * @param variantesOrganisees Map des variantes organisées
     * @param nomVariante Nom de la variante (ex: "Couleur")
     * @return Liste des valeurs uniques pour cette variante
     */
    public static List<String> getValeursPourVariante(
            Map<String, List<String>> variantesOrganisees, 
            String nomVariante) {
        return variantesOrganisees.getOrDefault(nomVariante, new ArrayList<>());
    }
}
