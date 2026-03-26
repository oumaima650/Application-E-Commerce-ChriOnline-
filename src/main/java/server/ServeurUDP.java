package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur UDP pour l'envoi de notifications aux clients
 */
public class ServeurUDP {
    private static ServeurUDP instance;
    private final ConcurrentHashMap<Integer, ClientEndPoint> clientsUdp;
    private DatagramSocket socketUdp;
    
    private ServeurUDP() {
        this.clientsUdp = new ConcurrentHashMap<>();
        try {
            // Socket UDP pour l'envoi
            this.socketUdp = new DatagramSocket();
            System.out.println("[ServeurUDP] Serveur UDP initialisé sur un port aléatoire");
        } catch (Exception e) {
            System.err.println("[ServeurUDP] Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    public static synchronized ServeurUDP getInstance() {
        if (instance == null) {
            instance = new ServeurUDP();
        }
        return instance;
    }
    
    /**
     * Enregistre un client pour les notifications UDP
     */
    public void registerClient(int clientId, String ipAddress, int udpPort) {
        ClientEndPoint endPoint = new ClientEndPoint(ipAddress, udpPort, clientId);
        clientsUdp.put(clientId, endPoint);
        System.out.println("[ServeurUDP] Client enregistré: " + endPoint);
    }
    
    /**
     * Désenregistre un client
     */
    public void unregisterClient(int clientId) {
        ClientEndPoint removed = clientsUdp.remove(clientId);
        if (removed != null) {
            System.out.println("[ServeurUDP] Client désenregistré: " + removed);
        }
    }
    
    /**
     * Envoie une notification UDP à un client spécifique
     */
    public boolean envoyerNotification(int clientId, String message) {
        ClientEndPoint endPoint = clientsUdp.get(clientId);
        if (endPoint == null) {
            System.err.println("[ServeurUDP] Client " + clientId + " non trouvé pour notification UDP");
            return false;
        }
        
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(endPoint.getIpAddress());
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, endPoint.getUdpPort());
            
            socketUdp.send(packet);
            System.out.println("[ServeurUDP] Notification envoyée à " + endPoint + ": " + message);
            return true;
            
        } catch (Exception e) {
            System.err.println("[ServeurUDP] Erreur envoi notification à " + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envoie une notification à tous les clients connectés
     */
    public void envoyerNotificationGlobale(String message) {
        System.out.println("[ServeurUDP] Envoi notification globale à " + clientsUdp.size() + " clients");
        
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
     * Ferme le serveur UDP
     */
    public void fermer() {
        if (socketUdp != null && !socketUdp.isClosed()) {
            socketUdp.close();
            System.out.println("[ServeurUDP] Serveur UDP fermé");
        }
    }
}
