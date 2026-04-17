package test;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class SynFloodTest {

    public static void main(String[] args) {
        System.out.println("=== TP3: SYN Flood Test (Avec ServeurTCPTestable) ===");

        // Démarrage du serveur de test en Background
        Thread serverThread = new Thread(() -> ServeurTCPTestable.main(new String[]{}));
        serverThread.setDaemon(true);
        serverThread.start();
        
        try {
            // Patienter pour l'initialisation du serveur
            Thread.sleep(3000);

            // Test 1 : Lancement de l'attaque simulée
            System.out.println("\n--- TEST 1 : Lancement attaque de 20 connexions simultanées ---");
            TestSimulationResult result = runSimulationAndWait(20);
            
            System.out.println("[Assert] Connexions réussies observées : " + result.success);
            if (result.success <= 10) {
                System.out.println("[Test 1 OK] Le serveur a correctement limité à 10 max.");
            } else {
                System.err.println("[Test 1 FAIL] Le serveur a accepté plus de 10 connexions.");
            }

            // Test 2 : Récupération du serveur (après les 10s du SLEEP)
            System.out.println("\n--- TEST 2 : Wait 12s et Test d'un client légitime ---");
            System.out.println("Patienter que les connexions bloquantes sois libérées...");
            Thread.sleep(12500); 

            boolean legitimateSuccess = launchLegitimateClient();
            if (legitimateSuccess) {
                System.out.println("[Test 2 OK] Un client légitime a été accepté sans problème post-attaque.");
            } else {
                System.err.println("[Test 2 FAIL] Un client légitime a été rejeté ou timeout.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("=== FIN DES TESTS ===");
        System.exit(0);
    }
    
    public static class TestSimulationResult {
        int success, blocked;
        public TestSimulationResult(int s, int b) { this.success = s; this.blocked = b; }
    }

    public static TestSimulationResult runSimulationAndWait(int count) throws Exception {
        java.util.concurrent.ExecutorService attackExecutor = java.util.concurrent.Executors.newFixedThreadPool(count);
        java.util.concurrent.atomic.AtomicInteger succ = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger block = new java.util.concurrent.atomic.AtomicInteger();

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();

        for (int i=0; i<count; i++) {
            attackExecutor.submit(() -> {
                try {
                    SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443);
                    socket.startHandshake();
                    succ.incrementAndGet();
                    Thread.sleep(12000);
                    socket.close();
                } catch(Exception e) {
                    block.incrementAndGet();
                }
            });
        }
        attackExecutor.shutdown();
        attackExecutor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS);
        return new TestSimulationResult(succ.get(), block.get());
    }

    public static boolean launchLegitimateClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();

            SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443);
            socket.setSoTimeout(15000); // 15 secondes pour attendre la fin du sleep de 10s du serveur!
            socket.startHandshake();
            socket.close();
            return true;
        } catch (Exception e) {
            System.err.println("Client légitime refusé: " + e.getMessage());
            return false;
        }
    }
}
