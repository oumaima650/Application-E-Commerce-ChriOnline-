package service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de protection du trafic UDP contre le spam et les attaques volumétriques.
 * le fichier contient la logique du Rate Limiting et du Filtrage
 */
public class UdpSecurityService {

    private static final Logger logger = LogManager.getLogger(UdpSecurityService.class);

    // Limite stricte : Un client ne peut recevoir/déclencher plus de 3 notifications par seconde
    private static final int MAX_PACKETS_PER_SECOND = 3;
    
    // Limite de taille : on refuse d'envoyer un paquet UDP trop gros (ex: > 1 Ko)
    private static final int MAX_PAYLOAD_SIZE_BYTES = 1024;

    // Dictionnaire pour se souvenir du nombre de paquets par ClientID
    // Format : [ID du client] -> [Tableau contenant le Compteur de paquets et le Timestamp]
    private final ConcurrentHashMap<Integer, long[]> packetCounters = new ConcurrentHashMap<>();

    /**
     * Valide si le paquet UDP respecte nos règles de sécurité.
     * @param clientId L'identifiant cible.
     * @param message Le message à transmettre.
     * @return true si le paquet est sain, false s'il s'agit d'une attaque UDP.
     */
    public boolean isSafePacket(int clientId, String message) {
        
        // 1. FILTRAGE : Vérification de la taille (Prévention Volumétrique)
        if (message == null || message.trim().isEmpty()) {
            logger.warn("ALERTE UDP : Message vide détecté pour le client ID {}.", clientId);
            return false;
        }
        
        byte[] payload = message.getBytes();
        if (payload.length > MAX_PAYLOAD_SIZE_BYTES) {
            logger.warn("ATTAQUE UDP FLOOD DÉTECTÉE : Payload trop lourd ({} bytes) rejeté pour le client ID {}", payload.length, clientId);
            return false;
        }

        // 2. RATE LIMITING (Limitation de débit)
        long currentTimeMillis = System.currentTimeMillis();
        
        // On récupère le compteur de ce client
        packetCounters.putIfAbsent(clientId, new long[]{0, currentTimeMillis});
        long[] data = packetCounters.get(clientId);

        // Si plus d'une seconde s'est écoulée depuis le dernier message, on réinitialise son compteur
        if (currentTimeMillis - data[1] > 1000) {
            data[0] = 1; // 1er paquet de la "nouvelle" seconde
            data[1] = currentTimeMillis;
            return true;
        }

        // S'il est toujours dans la même seconde, on incrémente son compteur
        data[0]++;
        
        if (data[0] > MAX_PACKETS_PER_SECOND) {
            // 3. SURVEILLANCE & LOGS : On consigne l'attaque
            logger.error("DÉFENSE ACTIVE : Blocage UDP Flood. Le client ID {} a dépassé la limite de {} requêtes/seconde.", clientId, MAX_PACKETS_PER_SECOND);
            return false; // ON BLOQUE L'ENVOI UDP !
        }

        return true; // Le message est "propre"
    }
}
