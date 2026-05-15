package service;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeResponseService {
    private final Map<String, ChallengeInfo> pendingChallenges = new ConcurrentHashMap<>();
    private static final long CHALLENGE_EXPIRY_MS = 120000; // 120 secondes (2 minutes)

    private record ChallengeInfo(String challenge, long timestamp) {}

    public String generateChallenge(String email) {
        String challenge = UUID.randomUUID().toString();
        pendingChallenges.put(email, new ChallengeInfo(challenge, System.currentTimeMillis()));
        return challenge;
    }

    public boolean verifyChallengeSignature(String email, String signatureBase64) {
        ChallengeInfo info = pendingChallenges.remove(email);
        if (info == null || (System.currentTimeMillis() - info.timestamp()) > CHALLENGE_EXPIRY_MS) {
            return false;
        }

        try {
            PublicKey publicKey = security.VaultClient.getPublicKey(email);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(info.challenge().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            System.err.println("[ChallengeResponseService] Erreur de vérification : " + e.getMessage());
            return false;
        }
    }
}
