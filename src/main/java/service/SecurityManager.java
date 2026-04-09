package service;

import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import java.util.Map;

//importation des classes Log4j2
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gestionnaire central de la sécurité de l'application.
 * Orchestre les différents services de protection (Brute Force, SQLi, etc.).
 */
public class SecurityManager {

    //creer loger de cette classe
    private static final Logger logger = LogManager.getLogger(SecurityManager.class);

    private final LoginAttemptLimitService loginAttemptService = new LoginAttemptLimitService();
    private final ReplayProtectionService replayProtectionService = new ReplayProtectionService();

    /**
     * Valide une requête entrante avant son traitement par les services métiers.
     * 
     * @param requete  La requête reçue.
     * @param clientIp L'adresse IP du client.
     * @return Une Reponse d'erreur si la requête est bloquée, sinon null.
     */
    public Reponse validateRequest(Requete requete, String clientIp) {
        // 1. Vérification du blocage IP (Global pour toutes les requêtes)
        LoginAttemptLimitService.CheckResult ipCheck = loginAttemptService.checkAccess(clientIp, "IP");
        if (!ipCheck.allowed) {
            //enregistrer l'attaque de niveau WARN pour l'IP
            logger.warn("ALERTE SÉCURITÉ de niveau WARN: l'IP a été rejetée . Motif: {}", clientIp, ipCheck.message);
            return new Reponse(false, ipCheck.message, null);
        }

        // 2. Vérification spécifique au LOGIN (Blocage par Email)
        if (requete.getType() == RequestType.LOGIN) {
            Map<String, Object> params = requete.getParametres();
            if (params != null && params.containsKey("email")) {
                String email = (String) params.get("email");
                LoginAttemptLimitService.CheckResult emailCheck = loginAttemptService.checkAccess(email, "EMAIL");
                if (!emailCheck.allowed) {
                    //enregistrer attaque ciblee sur un email
                    logger.warn("ALERTE BRUTE FORCE: Le compte lié à l'email '{}' est ciblé depuis l'IP {}. Rejet de la requête.", email, clientIp);
                    return new Reponse(false, emailCheck.message, null);
                }
            }
        }

        // 3. Vérification de l'attaque par Rejeu (Replay Attack)
        if (replayProtectionService.isReplayAttack(requete)) {
            // DETECTION DE REPLAY ATTACK (GRAVE)
            logger.error("ALERTE CRITIQUE DE SÉCURITÉ: Attaque par Rejeu (Replay) détectée depuis l'IP {} pour la requête {}", clientIp, requete.getType());
            return new Reponse(false, "REPLAY_ATTACK_DETECTED", null);
        }

        // Ici, les autres membres de l'équipe pourront ajouter leurs propres
        // vérifications :
        // if (!sqlInjectionService.isSafe(requete)) return new Reponse(false, "ALERTE
        // SQLi", null);
        // if (!geoBlockService.isIpAllowed(clientIp)) return new Reponse(false, "Région
        // bloquée", null);

        //Enregistrer les requetes autorisees
        logger.info("Requête autorisée: {} depuis l'IP {}", requete.getType(), clientIp);

        return null; // OK - Requête autorisée
    }

    public LoginAttemptLimitService getLoginAttemptService() {
        return loginAttemptService;
    }
}
