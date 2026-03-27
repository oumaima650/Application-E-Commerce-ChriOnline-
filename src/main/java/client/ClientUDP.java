package client;

import javafx.application.Platform;
import ui.NotificationsController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Classe gerant la reception des notifications en temps reel pour le client via le protocole UDP.
 * Elle s'execute en arriere-plan (Thread) pour ecouter en permanence le serveur.
 */
public class ClientUDP extends Thread {
    private int portEcoute;
    private boolean running = true;

    // Reference optionnelle au controleur de la page Notifications
    // Si non null, les messages UDP alimentent directement la page de notifs
    // Si null, on affiche une alerte contextuelle
    private NotificationsController notificationsController;

    public ClientUDP(int port) {
        this.portEcoute = port;
        // setDaemon(true) permet d'arreter le thread automatiquement quand le programme principal se ferme
        setDaemon(true); 
    }

    public void setNotificationsController(NotificationsController controller) {
        this.notificationsController = controller;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(portEcoute)) {
            System.out.println("Ecoute des notifications sur UDP " + portEcoute);
            byte[] buffer = new byte[1024];

            while (running) {
                // Creation d'un paquet pour recevoir les donnees
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Attente bloquante : le thread se met en pause ici jusqu'a reception

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("\n[🔔 NOTIFICATION UDP] => " + message);

                // Obligatoire de passer par Platform.runLater() pour modifier l'interface graphique JavaFX
                Platform.runLater(() -> {
                    if (notificationsController != null) {
                        // Si l'utilisateur est sur la page des notifications, on ajoute le message a la liste
                        notificationsController.addNotification("Nouvelle mise a jour", message);
                    } else {
                        // Solution de repli : afficher une alerte flottante (popup) n'importe ou dans l'application
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
        interrupt(); // Force l'arret de la methode bloquante socket.receive()
    }
    
    /**
     * Retourne le port d'ecoute UDP pour pouvoir l'envoyer au serveur principal (TCP)
     */
    public int getPortEcoute() {
        return portEcoute;
    }
}
