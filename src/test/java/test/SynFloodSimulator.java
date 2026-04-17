package test;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SynFloodSimulator {

    public static void main(String[] args) {
        System.out.println("=== Démarrage Simulation d'attaque DoS (TCP SYN Flood) ===");
        runSimulation(200);
    }

    public static void runSimulation(int totalConnections) {
        ExecutorService attackExecutor = Executors.newFixedThreadPool(totalConnections);
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger blockedConnections = new AtomicInteger(0);

        try {
            // Créer un contexte SSL "Trust All" pour éviter les erreurs de certificat
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());

            SSLSocketFactory factory = sslContext.getSocketFactory();

            for (int i = 0; i < totalConnections; i++) {
                attackExecutor.submit(() -> {
                    try {
                        SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443);
                        socket.startHandshake();
                        successfulConnections.incrementAndGet();
                        // Maintenir la connexion ouverte (simulation du DoS)
                        Thread.sleep(12000); 
                        socket.close(); // Fermer pour libérer le serveur après l'attaque
                    } catch (Exception e) {
                        blockedConnections.incrementAndGet();
                    }
                });
            }

            attackExecutor.shutdown();
            attackExecutor.awaitTermination(20, TimeUnit.SECONDS);

            System.out.println("\n--- RESULTAT SIMULATION ---");
            System.out.println("Tentatives totales : " + totalConnections);
            System.out.println("Connexions réussies (Ressource occupée) : " + successfulConnections.get());
            System.out.println("Connexions bloquées/refusées : " + blockedConnections.get());

        } catch (Exception e) {
            System.err.println("Erreur Simulation: " + e.getMessage());
        }
    }
}
