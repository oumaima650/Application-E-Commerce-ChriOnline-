package client;

import javafx.application.Platform;
import ui.NotificationsController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientUDP extends Thread {
    private int portEcoute;
    private boolean running = true;

    // Référence optionnelle au contrôleur de la page Notifications
    // Si non null, les messages UDP alimentent directement la page de notifs
    // Si null, on affiche juste dans la console
    private NotificationsController notificationsController;

    public ClientUDP(int port) {
        this.portEcoute = port;
        setDaemon(true); // S'arrête automatiquement quand le programme se ferme
    }

    public void setNotificationsController(NotificationsController controller) {
        this.notificationsController = controller;
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

                // Mettre à jour l'UI JavaFX depuis le thread JavaFX
                Platform.runLater(() -> {
                    if (notificationsController != null) {
                        // Afficher dans la page Notifications dédiée
                        notificationsController.addNotification("Notification reçue", message);
                    } else {
                        // Fallback : afficher une Alert si la page n'est pas encore ouverte
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Nouvelle Notification UDP");
                        alert.setHeaderText("ChriOnline - Nouvelle Info");
                        alert.setContentText(message);
                        alert.getDialogPane().setStyle("-fx-background-color: #FFF0EB;");
                        alert.show();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Erreur de Thread UDP: " + e.getMessage());
        }
    }

    public void stopListening() {
        running = false;
        interrupt();
    }
    
    /**
     * Retourne le port d'écoute UDP pour l'envoyer au serveur
     */
    public int getPortEcoute() {
        return portEcoute;
    }
}
