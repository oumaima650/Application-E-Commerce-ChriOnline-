package util;

import config.CloudinaryConfig;
import service.ImageService;

/**
 * Utilitaire de test pour la configuration Cloudinary
 */
public class CloudinaryTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test de Configuration Cloudinary ===\n");
        
        // 1. Test de connexion
        testConnection();
        
        // 2. Test d'upload
        testUpload();
        
        // 3. Test de génération d'URL
        testUrlGeneration();
        
        // 4. Test de suppression
        testDeletion();
    }
    
    private static void testConnection() {
        System.out.println("1. Test de connexion à Cloudinary...");
        try {
            boolean isConnected = CloudinaryConfig.testConnection();
            System.out.println("   Résultat: " + (isConnected ? "✅ Connexion réussie" : "❌ Échec de connexion"));
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testUpload() {
        System.out.println("2. Test d'upload d'image...");
        try {
            // Créer une image de test (1x1 pixel PNG)
            byte[] testImage = createTestImage();
            
            String url = ImageService.uploadProductImage(testImage, 999);
            if (url != null) {
                System.out.println("   ✅ Upload réussi: " + url);
                
                // Test de validation d'URL
                boolean isValid = ImageService.isImageUrlValid(url);
                System.out.println("   Validation URL: " + (isValid ? "✅ Valide" : "❌ Invalide"));
                
                // Test d'URL optimisée
                String optimizedUrl = ImageService.getProductImageUrl(url, 150, 150);
                System.out.println("   URL optimisée: " + optimizedUrl);
                
            } else {
                System.out.println("   ❌ Échec de l'upload");
            }
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testUrlGeneration() {
        System.out.println("3. Test de génération d'URL optimisée...");
        try {
            String testUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg";
            String optimized = ImageService.getProductImageUrl(testUrl, 200, 200);
            System.out.println("   URL originale: " + testUrl);
            System.out.println("   URL optimisée: " + (optimized != null ? optimized : "❌ Erreur"));
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testDeletion() {
        System.out.println("4. Test de suppression d'image...");
        try {
            // D'abord upload une image
            byte[] testImage = createTestImage();
            String url = ImageService.uploadProductImage(testImage, 998);
            
            if (url != null) {
                System.out.println("   Image uploadée pour test: " + url);
                
                // Puis la supprimer
                boolean deleted = ImageService.deleteProductImage(url);
                System.out.println("   Suppression: " + (deleted ? "✅ Réussie" : "❌ Échec"));
            } else {
                System.out.println("   ❌ Impossible d'uploader l'image pour le test");
            }
        } catch (Exception e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Crée une image de test simple (1x1 pixel PNG)
     */
    private static byte[] createTestImage() {
        // PNG header pour un image 1x1 pixel transparente
        byte[] pngData = {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length (13)
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // Width: 1
            0x00, 0x00, 0x00, 0x01, // Height: 1
            0x08, 0x06, 0x00, 0x00, 0x00, // Bit depth, color type, compression, filter, interlace
            (byte)0x1F, 0x15, (byte)0xC4, (byte)0x89, // CRC
            0x00, 0x00, 0x00, 0x0A, // IDAT chunk length (10)
            0x49, 0x44, 0x41, 0x54, // IDAT
            0x78, (byte)0x9C, (byte)0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, // Compressed data
            (byte)0x0D, (byte)0x7A, (byte)0x6E, (byte)0x66, // CRC
            0x00, 0x00, 0x00, 0x00, // IEND chunk length (0)
            0x49, 0x45, 0x4E, 0x44, // IEND
            (byte)0xAE, 0x42, 0x60, (byte)0x82  // CRC
        };
        return pngData;
    }
    
    /**
     * Affiche les informations de configuration actuelles
     */
    public static void showConfiguration() {
        System.out.println("=== Configuration Cloudinary Actuelle ===");
        System.out.println("Cloud Name: [CONFIGURÉ]");
        System.out.println("API Key: [CONFIGURÉ]");
        System.out.println("API Secret: [CONFIGURÉ]");
        System.out.println();
        System.out.println("Pour modifier la configuration, éditez:");
        System.out.println("src/main/java/config/CloudinaryConfig.java");
        System.out.println();
    }
}
