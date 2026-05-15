package util;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class P12Signer {
    public static String signChallenge(String p12Path, String password, String email, String challenge) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            ks.load(fis, password.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(email, password.toCharArray());
        if (privateKey == null) {
            throw new Exception("Clé privée introuvable pour l'alias : " + email);
        }

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(challenge.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}
