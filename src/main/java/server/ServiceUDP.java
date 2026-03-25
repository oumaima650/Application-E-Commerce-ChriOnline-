package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service UDP pour l'envoi de notifications aux clients
 */
public class ServiceUDP {
    private static ServiceUDP instance;
    private final ConcurrentHashMap<Integer, ClientEndPoint> clientsUdp;
    private DatagramSocket socketUdp;
    
    private ServiceUDP() {
        this.clientsUdp = new ConcurrentHashMap<>();
        try {
            // Socket UDP pour l'envoi (pas de bind spécifique nécessaire)
            this.socketUdp = new DatagramSocket();
            System.out.println("[ServiceUDP] Service UDP initialisé sur un port aléatoire");
        } catch (Exception e) {
            System.err.println("[ServiceUDP] Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    public static synchronized ServiceUDP getInstance() {
        if (instance == null) {
            instance = new ServiceUDP();
        }
        return instance;
    }
    
    /**
     * Enregistre un client pour les notifications UDP
     */
    public void registerClient(int clientId, String ipAddress, int udpPort) {
        ClientEndPoint endPoint = new ClientEndPoint(ipAddress, udpPort, clientId);
        clientsUdp.put(clientId, endPoint);
        System.out.println("[ServiceUDP] Client enregistré: " + endPoint);
    }
    
    /**
     * Désenregistre un client
     */
    public void unregisterClient(int clientId) {
        ClientEndPoint removed = clientsUdp.remove(clientId);
        if (removed != null) {
            System.out.println("[ServiceUDP] Client désenregistré: " + removed);
        }
    }
    
    /**
     * Envoie une notification UDP à un client spécifique
     */
    public boolean envoyerNotification(int clientId, String message) {
        ClientEndPoint endPoint = clientsUdp.get(clientId);
        if (endPoint == null) {
            System.err.println("[ServiceUDP] Client " + clientId + " non trouvé pour notification UDP");
            return false;
        }
        
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(endPoint.getIpAddress());
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, endPoint.getUdpPort());
            
            socketUdp.send(packet);
            System.out.println("[ServiceUDP] Notification envoyée à " + endPoint + ": " + message);
            return true;
            
        } catch (Exception e) {
            System.err.println("[ServiceUDP] Erreur envoi notification à " + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envoie une notification à tous les clients connectés
     */
    public void envoyerNotificationGlobale(String message) {
        System.out.println("[ServiceUDP] Envoi notification globale à " + clientsUdp.size() + " clients");
        
        clientsUdp.forEach((clientId, endPoint) -> {
            envoyerNotification(clientId, message);
        });
    }
    
    /**
     * Vérifie si un client est enregistré pour UDP
     */
    public boolean isClientRegistered(int clientId) {
        return clientsUdp.containsKey(clientId);
    }
    
    /**
     * Ferme le service UDP
     */
    public void fermer() {
        if (socketUdp != null && !socketUdp.isClosed()) {
            socketUdp.close();
            System.out.println("[ServiceUDP] Service UDP fermé");
        }
    }
}
