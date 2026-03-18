package client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientUDP extends Thread {
    private int portEcoute;
    private boolean running = true;

    public ClientUDP(int port) {
        this.portEcoute = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(portEcoute)) {
            System.out.println("Écoute des notifications sur UDP " + portEcoute);
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Bloquant
                
                String message = new String(packet.getData(), 0, packet.getLength());
                
                System.out.println("\n[🔔 NOTIFICATION UDP] => " + message);
                
                // Mettre à jour l'UI JavaFX de manière sécurisée (depuis un autre thread)
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Nouvelle Notification");
                    alert.setHeaderText("ChriOnline - Info Rapide");
                    alert.setContentText(message);
                    
                    // Style direct pour respecter la charte si besoin
                    alert.getDialogPane().setStyle("-fx-background-color: #F6D5EE; -fx-text-fill: #24316B;");
                    alert.show();
                });
            }
        } catch (Exception e) {
            System.err.println("Erreur de Thread UDP: " + e.getMessage());
        }
    }
}
