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

    /**
     * Valide une requête entrante avant son traitement par les services métiers.
     * @param requete La requête reçue.
     * @param clientIp L'adresse IP du client.
     * @return Une Reponse d'erreur si la requête est bloquée, sinon null.
     */
    public Reponse validateRequest(Requete requete, String clientIp) {
        // 1. Vérification du blocage IP (Global pour toutes les requêtes)
        LoginAttemptLimitService.CheckResult ipCheck = loginAttemptService.checkAccess(clientIp, "IP");
        if (!ipCheck.allowed) {
            return new Reponse(false, ipCheck.message, null);
        }

        // 2. Vérification spécifique au LOGIN (Blocage par Email)
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

        // Ici, les autres membres de l'équipe pourront ajouter leurs propres vérifications :
        // if (!sqlInjectionService.isSafe(requete)) return new Reponse(false, "ALERTE SQLi", null);
        // if (!geoBlockService.isIpAllowed(clientIp)) return new Reponse(false, "Région bloquée", null);

        return null; // OK - Requête autorisée
    }

    public LoginAttemptLimitService getLoginAttemptService() {
        return loginAttemptService;
    }
}
