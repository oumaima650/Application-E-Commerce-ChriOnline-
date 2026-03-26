package service;

import config.CloudinaryConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service pour la gestion des images des produits avec Cloudinary
 */
public class ImageService {
    
    /**
     * Upload une image de produit depuis un fichier local
     * @param filePath Chemin du fichier image
     * @param productId ID du produit
     * @return URL Cloudinary de l'image ou null si erreur
     */
    public static String uploadProductImage(String filePath, int productId) {
        try {
            // Vérifier que le fichier existe
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("[ImageService] Fichier non trouvé: " + filePath);
                return null;
            }
            
            // Générer un ID public unique
            String publicId = "product_" + productId + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Upload sur Cloudinary
            return CloudinaryConfig.uploadImage(filePath, publicId);
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur upload produit: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Upload une image de produit depuis un byte array
     * @param imageBytes Image en bytes
     * @param productId ID du produit
     * @return URL Cloudinary de l'image ou null si erreur
     */
    public static String uploadProductImage(byte[] imageBytes, int productId) {
        try {
            // Générer un ID public unique
            String publicId = "product_" + productId + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Upload sur Cloudinary
            return CloudinaryConfig.uploadImage(imageBytes, publicId);
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur upload bytes produit: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Upload une image de produit depuis une URL (ex: image temporaire)
     * @param imageUrl URL de l'image source
     * @param productId ID du produit
     * @return URL Cloudinary de l'image ou null si erreur
     */
    public static String uploadProductImageFromUrl(String imageUrl, int productId) {
        try {
            // Télécharger l'image depuis l'URL
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            if (imageBytes == null) {
                return null;
            }
            
            // Upload sur Cloudinary
            return uploadProductImage(imageBytes, productId);
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur upload depuis URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Supprime une image de produit
     * @param cloudinaryUrl URL Cloudinary de l'image
     * @return true si succès, false sinon
     */
    public static boolean deleteProductImage(String cloudinaryUrl) {
        try {
            // Extraire le public_id de l'URL Cloudinary
            String publicId = extractPublicIdFromUrl(cloudinaryUrl);
            if (publicId == null) {
                System.err.println("[ImageService] Impossible d'extraire public_id de: " + cloudinaryUrl);
                return false;
            }
            
            return CloudinaryConfig.deleteImage(publicId);
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur suppression image: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Génère une URL optimisée pour l'affichage d'un produit
     * @param cloudinaryUrl URL Cloudinary de l'image
     * @param width Largeur souhaitée
     * @param height Hauteur souhaitée
     * @return URL optimisée ou l'URL originale si erreur
     */
    public static String getProductImageUrl(String cloudinaryUrl, int width, int height) {
        try {
            if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                return getDefaultProductImage();
            }
            
            // Extraire le public_id
            String publicId = extractPublicIdFromUrl(cloudinaryUrl);
            if (publicId == null) {
                return cloudinaryUrl; // Retourner l'URL originale si extraction échoue
            }
            
            // Générer URL optimisée
            String optimizedUrl = CloudinaryConfig.getOptimizedUrl(publicId, width, height);
            return optimizedUrl != null ? optimizedUrl : cloudinaryUrl;
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur génération URL: " + e.getMessage());
            return cloudinaryUrl != null ? cloudinaryUrl : getDefaultProductImage();
        }
    }
    
    /**
     * Retourne une URL d'image par défaut pour les produits
     * @return URL de l'image par défaut
     */
    public static String getDefaultProductImage() {
        return "https://via.placeholder.com/300x300/2A2C41/FFFFFF?text=No+Image";
    }
    
    /**
     * Extrait le public_id d'une URL Cloudinary
     * @param cloudinaryUrl URL Cloudinary
     * @return public_id ou null si extraction échoue
     */
    private static String extractPublicIdFromUrl(String cloudinaryUrl) {
        try {
            // Exemple d'URL: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/chrionline/products/public_id.jpg
            String[] parts = cloudinaryUrl.split("/");
            if (parts.length < 2) return null;
            
            // Trouver l'index de "chrionline"
            int chrionlineIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("chrionline")) {
                    chrionlineIndex = i;
                    break;
                }
            }
            
            if (chrionlineIndex == -1 || chrionlineIndex + 2 >= parts.length) {
                return null;
            }
            
            // Reconstruire le public_id: chrionline/products/public_id
            String publicId = parts[chrionlineIndex] + "/" + parts[chrionlineIndex + 1] + "/" + parts[chrionlineIndex + 2];
            
            // Supprimer l'extension de fichier
            if (publicId.contains(".")) {
                publicId = publicId.substring(0, publicId.lastIndexOf('.'));
            }
            
            return publicId;
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur extraction public_id: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Télécharge une image depuis une URL
     * @param imageUrl URL de l'image
     * @return Image en bytes ou null si erreur
     */
    private static byte[] downloadImageFromUrl(String imageUrl) {
        try {
            // Utiliser Java HTTP Client pour télécharger
            java.net.URI uri = java.net.URI.create(imageUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
            
            java.net.http.HttpResponse<byte[]> response = client.send(
                request, 
                java.net.http.HttpResponse.BodyHandlers.ofByteArray()
            );
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("[ImageService] Erreur HTTP: " + response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("[ImageService] Erreur téléchargement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Test si une URL est valide et accessible
     * @param imageUrl URL à tester
     * @return true si valide, false sinon
     */
    public static boolean isImageUrlValid(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return false;
            }
            
            java.net.URI uri = java.net.URI.create(imageUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                .build();
            
            java.net.http.HttpResponse<Void> response = client.send(
                request, 
                java.net.http.HttpResponse.BodyHandlers.discarding()
            );
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            return false;
        }
    }
}
