package service;

import dao.UtilisateurDAO;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background service that runs once every 24 hours at server startup.
 * It scans the database for Client accounts with status EN_ATTENTE
 * that are older than MAX_PENDING_HOURS and permanently deletes them.
 *
 * This prevents "ghost" accounts from accumulating — users who registered,
 * received the verification email, but never clicked the code.
 */
public class CleanupService {

    private static final int MAX_PENDING_HOURS = 24;
    private static final int RUN_EVERY_HOURS   = 24;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CleanupService-Thread");
        t.setDaemon(true); // won't prevent JVM shutdown
        return t;
    });

    /**
     * Starts the daily cleanup task.
     * Runs immediately on start, then every 24 hours after that.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::runCleanup,
            0,                  // initial delay: run at server startup
            RUN_EVERY_HOURS,    // period
            TimeUnit.HOURS
        );

        System.out.println("[CleanupService] Service de nettoyage démarré. "
            + "Comptes EN_ATTENTE supprimés après " + MAX_PENDING_HOURS + "h.");
    }

    private void runCleanup() {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[CleanupService] [" + time + "] Recherche de comptes abandonnés...");

        try {
            int deleted = UtilisateurDAO.deleteOldPendingAccounts(MAX_PENDING_HOURS);

            if (deleted == 0) {
                System.out.println("[CleanupService] Aucun compte abandonné trouvé.");
            } else {
                System.out.println("[CleanupService] ✓ " + deleted
                    + " compte(s) EN_ATTENTE supprimé(s) (inactif depuis >" + MAX_PENDING_HOURS + "h).");
            }

        } catch (SQLException e) {
            System.err.println("[CleanupService] Erreur SQL lors du nettoyage : " + e.getMessage());
        }
    }

    /**
     * Gracefully shuts down the scheduler (call on server stop).
     */
    public void stop() {
        scheduler.shutdownNow();
        System.out.println("[CleanupService] Service de nettoyage arrêté.");
    }
}
