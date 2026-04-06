package service;

import shared.Requete;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReplayProtectionService {
    private static final long MAX_TIME_WINDOW_MS = 30 * 1000L; // 30 secondes
    private final ConcurrentHashMap<String, Long> usedNonces = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ReplayProtectionService() {
        // Nettoyage périodique pour éviter la fuite de mémoire
        scheduler.scheduleAtFixedRate(this::cleanupExpiredNonces, 1, 1, TimeUnit.MINUTES);
    }

    public boolean isReplayAttack(Requete req) {
        long currentServerTime = System.currentTimeMillis();
        long reqTimestamp = req.getTimestamp();

        // Si le timestamp n'est pas envoyé, on refuse (protection contre les clients modifiés qui n'enverraient pas le timestamp)
        if (reqTimestamp == 0 || req.getNonce() == null || req.getNonce().isEmpty()) {
             return true;
        }

        // Vérifier l'âge de la requête
        if (Math.abs(currentServerTime - reqTimestamp) > MAX_TIME_WINDOW_MS) {
            return true; // Requête trop vieille (ou excessivement dans le futur)
        }

        // Vérifier le nonce
        if (usedNonces.putIfAbsent(req.getNonce(), reqTimestamp) != null) {
            return true; // Ce nonce est déjà utilisé (Replay Attack !)
        }

        return false;
    }

    private void cleanupExpiredNonces() {
        long currentServerTime = System.currentTimeMillis();
        usedNonces.entrySet().removeIf(entry -> 
            Math.abs(currentServerTime - entry.getValue()) > MAX_TIME_WINDOW_MS
        );
    }
}
