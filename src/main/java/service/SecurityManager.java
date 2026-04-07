package service;

import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import java.util.Map;

/**
 * Gestionnaire central de la sécurité de l'application.
 * Orchestre les différents services de protection (Brute Force, SQLi, etc.).
 */
public class SecurityManager {

    private final LoginAttemptLimitService loginAttemptService = new LoginAttemptLimitService();
    private final ReplayProtectionService replayProtectionService = new ReplayProtectionService();
    private final IpProtectionService ipProtectionService = new IpProtectionService();

    /**
     * Valide une requête entrante avant son traitement par les services métiers.
     * @param requete La requête reçue.
     * @param clientIp L'adresse IP du client (couche TCP).
     * @return Une Reponse d'erreur si la requête est bloquée, sinon null.
     */
    public Reponse validateRequest(Requete requete, String clientIp) {
        // 1. Détection de l'IP Spoofing (Anti-Spoofing Applicatif)
        // On vérifie si l'attaquant tente de mentir sur sa source via des en-têtes/paramètres
        if (ipProtectionService.detectSpoofing(requete, clientIp)) {
            System.err.println("[SECURITY] ATTENTION : tentative d'IP Spoofing détectée pour " + clientIp);
            return new Reponse(false, "SPOOFING_ATTEMPT_DETECTED: Votre adresse IP réelle ne correspond pas à l'adresse envoyée.", null);
        }

        // 2. Vérification du blocage IP (Global pour toutes les requêtes - Brute Force)
        LoginAttemptLimitService.CheckResult ipCheck = loginAttemptService.checkAccess(clientIp, "IP");
        if (!ipCheck.allowed) {
            return new Reponse(false, ipCheck.message, null);
        }

        // 3. Restriction d'accès Admin par IP
        // Si la requête est une opération d'administration, on exige une IP interne
        if (requete.getType() != null && requete.getType().name().startsWith("ADMIN_")) {
            if (!ipProtectionService.isInternalIp(clientIp)) {
                System.err.println("[SECURITY] Accès Admin REFUSÉ depuis une IP externe : " + clientIp);
                return new Reponse(false, "ACCÈS RESTREINT : Cette opération nécessite une connexion depuis le réseau interne.", null);
            }
        }

        // 4. Vérification spécifique au LOGIN (Blocage par Email)
        if (requete.getType() == RequestType.LOGIN) {
            Map<String, Object> params = requete.getParametres();
            if (params != null && params.containsKey("email")) {
                String email = (String) params.get("email");
                LoginAttemptLimitService.CheckResult emailCheck = loginAttemptService.checkAccess(email, "EMAIL");
                if (!emailCheck.allowed) {
                    return new Reponse(false, emailCheck.message, null);
                }
            }
        }

        // 5. Vérification de l'attaque par Rejeu (Replay Attack)
        if (replayProtectionService.isReplayAttack(requete)) {
            return new Reponse(false, "REPLAY_ATTACK_DETECTED", null);
        }

        return null; // OK - Requête autorisée
    }

    public LoginAttemptLimitService getLoginAttemptService() {
        return loginAttemptService;
    }
}
