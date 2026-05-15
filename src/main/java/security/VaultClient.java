package security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class VaultClient {

    private static final String VAULT_ADDR = System.getenv("VAULT_ADDR");
    private static final String VAULT_TOKEN = System.getenv("VAULT_TOKEN");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static void checkConfig() {
        if (VAULT_ADDR == null || VAULT_TOKEN == null) {
            throw new SecurityException("VAULT_ADDR or VAULT_TOKEN environment variables are not set");
        }
    }

    public static void storePublicKey(String email, PublicKey publicKey) {
        checkConfig();
        try {
            byte[] encodedKey = publicKey.getEncoded();
            String base64Key = Base64.getEncoder().encodeToString(encodedKey);
            String fingerprint = computeFingerprint(encodedKey);

            ObjectNode dataNode = OBJECT_MAPPER.createObjectNode();
            dataNode.put("public_key", base64Key);
            dataNode.put("fingerprint", fingerprint);

            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.set("data", dataNode);

            String jsonPayload = OBJECT_MAPPER.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VAULT_ADDR + "/v1/secret/data/admins/" + email))
                    .header("X-Vault-Token", VAULT_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                throw new SecurityException("Failed to store public key in Vault. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SecurityException("Error communicating with Vault: " + e.getMessage(), e);
        }
    }

    public static PublicKey getPublicKey(String email) {
        checkConfig();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VAULT_ADDR + "/v1/secret/data/admins/" + email))
                    .header("X-Vault-Token", VAULT_TOKEN)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new SecurityException("Public key for " + email + " not found in Vault");
            }

            if (response.statusCode() != 200) {
                throw new SecurityException("Failed to fetch public key from Vault. Status code: " + response.statusCode());
            }

            JsonNode rootNode = OBJECT_MAPPER.readTree(response.body());
            JsonNode dataNode = rootNode.path("data").path("data");
            
            String base64Key = dataNode.path("public_key").asText();
            if (base64Key.isEmpty()) {
                throw new SecurityException("Public key not found in Vault response for " + email);
            }

            byte[] publicBytes = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new SecurityException("Error fetching public key from Vault: " + e.getMessage(), e);
        }
    }

    public static void deletePublicKey(String email) {
        checkConfig();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VAULT_ADDR + "/v1/secret/data/admins/" + email))
                    .header("X-Vault-Token", VAULT_TOKEN)
                    .DELETE()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new SecurityException("Failed to delete public key from Vault. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new SecurityException("Error deleting public key from Vault: " + e.getMessage(), e);
        }
    }

    private static String computeFingerprint(byte[] keyBytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(keyBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
