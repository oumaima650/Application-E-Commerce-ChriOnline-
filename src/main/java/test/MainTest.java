package test;

// NOUVEAU : On n'importe plus UdpSecurityService directement. On importe le Manager.
import service.SecurityManager;

public class MainTest {

    public static void main(String[] args) {
        System.out.println("=== DÉMARRAGE DU TEST INTÉGRÉ (SECURITY MANAGER) ===");
        
        // NOUVEAU : On passe par la façade !
        SecurityManager manager = new SecurityManager();
        int fauxClientId = 999; 

        // ---------------------------------------------------------
        // TEST 1 : Simulation anti-spam (Rate Limiting)
        // ---------------------------------------------------------
        System.out.println("\n--- TEST 1 : Envoi massif (Flood) ---");
        
        for (int i = 1; i <= 5; i++) {
            // NOUVEAU : On appelle la méthode du SecurityManager
            boolean isSafe = manager.validateUdpRequest(fauxClientId, "Notif N°" + i);
            
            if (isSafe) {
                System.out.println("Requête " + i + " : ACCEPTÉE");
            } else {
                System.out.println("Requête " + i + " : BLOQUÉE par la sécurité !");
            }
        }

        // ---------------------------------------------------------
        // TEST 2 : Simulation Charge Utile Trop Lourde
        // ---------------------------------------------------------
        System.out.println("\n--- TEST 2 : Payload Gigantesque ---");
        
        String malwareText = "A".repeat(2000); 
        
        // NOUVEAU : On appelle la méthode du SecurityManager
        boolean isSafeVolumetric = manager.validateUdpRequest(fauxClientId, malwareText);
        
        if (isSafeVolumetric) {
            System.out.println("ERREUR : Le texte géant est passé !");
        } else {
            System.out.println("SUCCÈS : Le texte géant a été rejeté par la sécurité.");
        }
        
        System.out.println("\n=== FIN DU TEST ===");
    }
}
