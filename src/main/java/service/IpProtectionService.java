package service;

import shared.Requete;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service de protection contre l'IP Spoofing et gestion des IP de confiance.
 * Ce service aide à identifier si une IP est interne et à détecter les tentatives
 * d'usurpation d'IP via des paramètres de requête.
 */
public class IpProtectionService {

    // Regex pour les IP internes (127.0.0.1, 192.168.x.x, 10.x.x.x, 172.16.x.x-172.31.x.x)
    private static final Pattern INTERNAL_IP_PATTERN = Pattern.compile(
        "^(127\\.0\\.0\\.1|192\\.168\\..*|10\\..*|172\\.(1[6-9]|2[0-9]|3[0-1])\\..*)$"
    );

    /**
     * Vérifie si une adresse IP appartient au réseau interne/local.
     * @param ip L'adresse IP à vérifier.
     * @return true si l'IP est interne, false sinon.
     */
    public boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        // Gérer le cas IPv6 localhost si nécessaire
        if (ip.equals("0:0:0:0:0:0:0:1")) return true;
        return INTERNAL_IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Détecte si la requête contient des tentatives d'usurpation d'IP.
     * Un attaquant pourrait essayer d'injecter des paramètres comme 'X-Forwarded-For'
     * dans la carte de paramètres pour tromper le serveur.
     * 
     * @param req La requête reçue.
     * @param realIp L'IP réelle provenant de la connexion Socket (couche TCP).
     * @return true si une tentative de spoofing est détectée.
     */
    public boolean detectSpoofing(Requete req, String realIp) {
        Map<String, Object> params = req.getParametres();
        if (params == null) return false;

        // Liste des paramètres souvent utilisés pour injecter de fausses IPs
        String[] suspiciousParams = {"X-Forwarded-For", "X-Real-IP", "Client-IP", "spoofedIp", "originIp"};
        
        for (String key : suspiciousParams) {
            // On vérifie si la clé existe (insensible à la casse par précaution)
            for (String actualKey : params.keySet()) {
                if (actualKey.equalsIgnoreCase(key)) {
                    Object val = params.get(actualKey);
                    if (val instanceof String) {
                        String claimedIp = (String) val;
                        // Si l'IP déclarée ne correspond pas à l'IP réelle de la connexion
                        if (!claimedIp.equals(realIp)) {
                            System.err.println("[IP_SPOOF_DETECTION] ALERTE : Tentative d'usurpation détectée !");
                            System.err.println("  -> IP Réelle (TCP) : " + realIp);
                            System.err.println("  -> IP Déclarée (App) : " + claimedIp);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
