package service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de protection contre les attaques DoS au niveau TCP.
 * Gère les limites de connexions simultanées globales et par IP.
 */
public class TcpDosProtectionService {

    private static final Logger logger = LogManager.getLogger(TcpDosProtectionService.class);

    // Seuils de protection
    private static final int MAX_GLOBAL_CONNECTIONS = 10;
    private static final int MAX_IP_CONNECTIONS = 3;

    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> ipConnections = new ConcurrentHashMap<>();

    /**
     * Vérifie si une nouvelle connexion peut être acceptée.
     * @param ip L'adresse IP du client.
     * @return true si la connexion est autorisée, false si elle dépasse les limites.
     */
    public boolean canAcceptConnection(String ip) {
        // 1. Vérification Limite Globale
        if (totalConnections.get() >= MAX_GLOBAL_CONNECTIONS) {
            logger.warn("BLOCKED TCP: Limite globale de {} connexions atteinte. Rejet de {}", MAX_GLOBAL_CONNECTIONS, ip);
            return false;
        }

        // 2. Vérification Limite par IP
        if (!checkIpLimit(ip)) {
            logger.warn("BLOCKED TCP: Limite par IP (max {}) atteinte pour {}", MAX_IP_CONNECTIONS, ip);
            return false;
        }

        // 3. Incrémentation (On réserve la place)
        totalConnections.incrementAndGet();
        ipConnections.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
        
        return true;
    }

    /**
     * Libère les ressources associées à une connexion terminée.
     * @param ip L'adresse IP du client.
     */
    public void releaseConnection(String ip) {
        totalConnections.decrementAndGet();
        AtomicInteger count = ipConnections.get(ip);
        if (count != null) {
            count.decrementAndGet();
            // Nettoyage optionnel si le compteur tombe à zéro pour économiser de la mémoire
            if (count.get() <= 0) {
                ipConnections.remove(ip);
            }
        }
    }

    /**
     * Méthode protégée pour permettre l'override dans les tests (TP3).
     */
    public boolean checkIpLimit(String ip) {
        AtomicInteger count = ipConnections.get(ip);
        return count == null || count.get() < MAX_IP_CONNECTIONS;
    }

    public int getTotalConnections() {
        return totalConnections.get();
    }
}
