package server;

import java.io.Serializable;

/**
 * Objet de valeur (Value Object) representant les coordonnees reseau d'un client connecte.
 * Il sert de "fiche d'adresse" permettant au ServeurUDP de savoir exactement ou envoyer
 * ses paquets pour contacter un utilisateur specifique.
 * 
 * Champs:
 * - ipAddress : l'adresse IP de la machine cliente (ex: "192.168.1.15")
 * - udpPort   : le numero de port sur lequel le ClientUDP de cet utilisateur ecoute
 * - clientId  : l'identifiant interne de l'utilisateur en base de donnees
 */
public class ClientEndPoint implements Serializable {
    // L'adresse IP de la machine sur laquelle tourne l'application cliente
    private final String ipAddress;
    // Le port UDP que le ClientUDP de l'utilisateur a ouvert pour recevoir les messages
    private final int udpPort;
    // L'identifiant numerique de l'utilisateur (cle primaire en BDD), utile pour le retrouver dans le dictionnaire
    private final int clientId;
    
    // Constructeur : cree un point de contact complet a partir des coordonnees recues au moment de la connexion
    public ClientEndPoint(String ipAddress, int udpPort, int clientId) {
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
        this.clientId = clientId;
    }
    
    // Getters en lecture seule (pas de setters : cet objet est immuable une fois cree)
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    // Representation textuelle utile pour les logs du serveur (ex: Client 3 - 192.168.1.15:9090)
    @Override
    public String toString() {
        return "ClientEndPoint{" +
                "ipAddress='" + ipAddress + '\'' +
                ", udpPort=" + udpPort +
                ", clientId=" + clientId +
                '}';
    }
}
