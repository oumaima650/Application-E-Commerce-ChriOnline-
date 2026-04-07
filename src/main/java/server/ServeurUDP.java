package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur UDP charge de l'envoi des notifications (Systeme Push) aux clients connectes.
 * Contrairement a TCP, UDP ne necessite pas de maintenir une connexion constante, il "tire"
 * le message vers le port ecouteur du client.
 */
public class ServeurUDP {
    private static ServeurUDP instance;
    private final ConcurrentHashMap<Integer, ClientEndPoint> clientsUdp;
    private DatagramSocket socketUdp;
     //integrer le service de securite
    private final service.UdpSecurityService securityService = new service.UdpSecurityService();
    
    // Constructeur prive (Design Pattern Singleton)
    private ServeurUDP() {
        this.clientsUdp = new ConcurrentHashMap<>();
        try {
            // Creation du Socket UDP pour l'envoi (le systeme choisit un port libre aleatoire)
            this.socketUdp = new DatagramSocket();
            System.out.println("[ServeurUDP] Serveur UDP initialise sur un port aleatoire");
        } catch (Exception e) {
            System.err.println("[ServeurUDP] Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    // Fournit l'unique instance de ServeurUDP a tout le programme
    public static synchronized ServeurUDP getInstance() {
        if (instance == null) {
            instance = new ServeurUDP();
        }
        return instance;
    }
    
    /**
     * Enregistre l'adresse et le port UDP d'un utilisateur specifique.
     * Cette fonction est appelee lorsque le client se connecte via TCP
     * et transmet ses coordonnees UDP.
     */
    public void registerClient(int clientId, String ipAddress, int udpPort) {
        ClientEndPoint endPoint = new ClientEndPoint(ipAddress, udpPort, clientId);
        clientsUdp.put(clientId, endPoint);
        System.out.println("[ServeurUDP] Client enregistre: " + endPoint);
    }
    
    /**
     * Supprime le client du registre UDP (ex: lorsqu'il se deconnecte).
     */
    public void unregisterClient(int clientId) {
        ClientEndPoint removed = clientsUdp.remove(clientId);
        if (removed != null) {
            System.out.println("[ServeurUDP] Client desenregistre: " + removed);
        }
    }
    
    /**
     * Construit et expedie un paquet UDP contenant le message texte
     * vers l'adresse IP et le port UDP d'un client cible par son ID.
     */
    public boolean envoyerNotification(int clientId, String message) {
        ClientEndPoint endPoint = clientsUdp.get(clientId);
        if (endPoint == null) {
            System.err.println("[ServeurUDP] Client " + clientId + " non trouve pour notification UDP");
            return false;
        }

        // 1. VERIFICATION DE SECURITE (Anti-Spam UDP)
        if (!securityService.isSafePacket(clientId, message)) {
            // Si le service retourne false, cela signifie que l'envoi est bloqué
            // Le message d'erreur a deja ete affiche par le service de securite
            return false;
        }
        
        try {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(endPoint.getIpAddress());
            // Creation de l'enveloppe contenant le message et l'adresse de destination
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, endPoint.getUdpPort());
            
            // Lancement du paquet sur le reseau
            socketUdp.send(packet);
            System.out.println("[ServeurUDP] Notification envoyee a " + endPoint + ": " + message);
            return true;
            
        } catch (Exception e) {
            System.err.println("[ServeurUDP] Erreur envoi notification a " + clientId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Diffuse un meme message a l'ensemble des clients enregistres.
     */
    public void envoyerNotificationGlobale(String message) {
        System.out.println("[ServeurUDP] Envoi notification globale a " + clientsUdp.size() + " clients");
        
        clientsUdp.forEach((clientId, endPoint) -> {
            envoyerNotification(clientId, message);
        });
    }
    
    /**
     * Verifie de maniere securisee si le client figure dans le dictionnaire des cibles UDP.
     */
    public boolean isClientRegistered(int clientId) {
        return clientsUdp.containsKey(clientId);
    }
    
    /**
     * Ferme le port UDP lors de l'arret du serveur pour liberer les ressources.
     */
    public void fermer() {
        if (socketUdp != null && !socketUdp.isClosed()) {
            socketUdp.close();
            System.out.println("[ServeurUDP] Serveur UDP ferme");
        }
    }
}
