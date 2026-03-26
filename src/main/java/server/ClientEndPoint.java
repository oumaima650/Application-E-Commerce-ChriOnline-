package server;

import java.io.Serializable;

/**
 * Représente un point de terminaison client pour les notifications UDP
 */
public class ClientEndPoint implements Serializable {
    private final String ipAddress;
    private final int udpPort;
    private final int clientId;
    
    public ClientEndPoint(String ipAddress, int udpPort, int clientId) {
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
        this.clientId = clientId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    @Override
    public String toString() {
        return "ClientEndPoint{" +
                "ipAddress='" + ipAddress + '\'' +
                ", udpPort=" + udpPort +
                ", clientId=" + clientId +
                '}';
    }
}
