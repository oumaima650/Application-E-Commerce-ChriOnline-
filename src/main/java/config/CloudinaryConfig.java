package config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration pour Cloudinary - Gestion des images du produit
 */
public class CloudinaryConfig {
    
    private static Cloudinary cloudinary;
    
    // Configuration - À remplacer avec vos vraies clés Cloudinary
    private static final String CLOUD_NAME = "dam3isgtd";
    private static final String API_KEY = "977551729985273";
    private static final String API_SECRET = "9nlq8U8Qa4HBVRm43liXs4WvpH4";
    
    public static synchronized Cloudinary getInstance() {
        if (cloudinary == null) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("api_key", API_KEY);
            config.put("api_secret", API_SECRET);
            config.put("secure", true); // Force HTTPS
            
            cloudinary = new Cloudinary(config);
            System.out.println("[Cloudinary] Instance initialisée");
        }
        return cloudinary;
    }
    
    /**
     * Upload une image sur Cloudinary
     * @param filePath Chemin du fichier local
     * @param publicId ID public pour l'image
     * @return URL de l'image uploadée
     */
    public static String uploadImage(String filePath, String publicId) {
        try {
            Map<String, Object> uploadResult = getInstance().uploader().upload(
                filePath, 
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "chrionline/products",
                    "resource_type", "image",
                    "quality", "auto",
                    "fetch_format", "auto"
                )
            );
            
            String url = (String) uploadResult.get("secure_url");
            System.out.println("[Cloudinary] Image uploadée: " + url);
            return url;
        } catch (Exception e) {
            System.err.println("[Cloudinary] Erreur upload: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Upload une image depuis un byte array
     * @param imageBytes Image en bytes
     * @param publicId ID public pour l'image
     * @return URL de l'image uploadée
     */
    public static String uploadImage(byte[] imageBytes, String publicId) {
        try {
            Map<String, Object> uploadResult = getInstance().uploader().upload(
                imageBytes, 
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "chrionline/products",
                    "resource_type", "image",
                    "quality", "auto",
                    "fetch_format", "auto"
                )
            );
            
            String url = (String) uploadResult.get("secure_url");
            System.out.println("[Cloudinary] Image uploadée depuis bytes: " + url);
            return url;
        } catch (Exception e) {
            System.err.println("[Cloudinary] Erreur upload bytes: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Supprime une image de Cloudinary
     * @param publicId ID public de l'image à supprimer
     * @return true si succès, false sinon
     */
    public static boolean deleteImage(String publicId) {
        try {
            Map<String, Object> result = getInstance().uploader().destroy(
                "chrionline/products/" + publicId, 
                ObjectUtils.asMap("resource_type", "image")
            );
            
            boolean success = "ok".equals(result.get("result"));
            System.out.println("[Cloudinary] Image supprimée: " + publicId + " -> " + success);
            return success;
        } catch (Exception e) {
            System.err.println("[Cloudinary] Erreur suppression: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Génère une URL optimisée pour l'affichage
     * @param publicId ID public de l'image
     * @param width Largeur souhaitée
     * @param height Hauteur souhaitée
     * @return URL optimisée
     */
    public static String getOptimizedUrl(String publicId, int width, int height) {
        try {
            // Construire l'URL manuellement avec les transformations
            String baseUrl = "https://res.cloudinary.com/" + CLOUD_NAME + "/image/upload";
            String transformations = String.format("w_%d,h_%d,c_fill,q_auto,f_auto", width, height);
            String url = baseUrl + "/" + transformations + "/" + publicId;
            
            return url;
        } catch (Exception e) {
            System.err.println("[Cloudinary] Erreur génération URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Test la connexion à Cloudinary
     * @return true si connexion OK, false sinon
     */
    public static boolean testConnection() {
        try {
            // Utiliser une méthode simple pour tester la connexion
            // Essayer de lister les ressources (limité à 1 pour minimiser l'impact)
            Map<String, Object> result = getInstance().api().resources(
                ObjectUtils.asMap("max_results", 1)
            );
            return result != null && result.containsKey("resources");
        } catch (Exception e) {
            System.err.println("[Cloudinary] Erreur connexion: " + e.getMessage());
            return false;
        }
    }
}
