package service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Service to handle JWT Access Tokens (RS256) and Refresh Tokens (UUID).
 */
public class JWTService {

    private static final String ISSUER = "chrionline";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000; // 15 minutes
    private static final String KEYS_DIR = "keys";
    private static final String PRIVATE_KEY_PATH = KEYS_DIR + "/private.pem";
    private static final String PUBLIC_KEY_PATH = KEYS_DIR + "/public.pem";

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    static {
        try {
            initKeys();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize RSA keys for JWT", e);
        }
    }

    private static void initKeys() throws Exception {
        File dir = new File(KEYS_DIR);
        if (!dir.exists()) {
            System.out.println("[JWTService] Creating missing keys directory: " + dir.getAbsolutePath());
            dir.mkdir();
        }

        File privFile = new File(PRIVATE_KEY_PATH);
        File pubFile = new File(PUBLIC_KEY_PATH);

        if (privFile.exists() && pubFile.exists()) {
            System.out.println("[JWTService] Persistence check: RSA key files found.");
            loadKeys();
        } else {
            System.err.println("[JWTService] WARNING: RSA keys not found in path: " + privFile.getAbsolutePath() + ". Generating new ones.");
            generateKeys();
        }
    }

    private static void generateKeys() throws Exception {
        System.out.println("[JWTService] Generating new RSA 2048-bit key pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        privateKey = kp.getPrivate();
        publicKey = kp.getPublic();

        // Save Private Key (Base64)
        String privBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        Files.write(new File(PRIVATE_KEY_PATH).toPath(), privBase64.getBytes(StandardCharsets.UTF_8));

        // Save Public Key (Base64)
        String pubBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        Files.write(new File(PUBLIC_KEY_PATH).toPath(), pubBase64.getBytes(StandardCharsets.UTF_8));
    }

    private static void loadKeys() throws Exception {
        System.out.println("[JWTService] Loading existing RSA keys...");
        byte[] privBytes = Base64.getDecoder().decode(new String(Files.readAllBytes(new File(PRIVATE_KEY_PATH).toPath()), StandardCharsets.UTF_8));
        byte[] pubBytes = Base64.getDecoder().decode(new String(Files.readAllBytes(new File(PUBLIC_KEY_PATH).toPath()), StandardCharsets.UTF_8));

        KeyFactory kf = KeyFactory.getInstance("RSA");
        privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        publicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
    }

    public static String generateAccessToken(String userId, String role, String sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_MS);

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("sessionId", sessionId)
                .setIssuer(ISSUER)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public static String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public record TokenClaims(String userId, String role, String sessionId) {}

    public static TokenClaims verifyAccessToken(String token) throws ExpiredJwtException, SignatureException, Exception {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!ISSUER.equals(claims.getIssuer())) {
            throw new Exception("Invalid issuer");
        }

        return new TokenClaims(
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("sessionId", String.class)
        );
    }

    /**
     * UNSAFE — do not trust the content, used only for lookup during refresh.
     */
    public static String extractUserIdFromExpiredToken(String token) {
        try {
            // Split JWT (header.payload.signature)
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // Simple manual extraction to avoid library validation for expired tokens
            if (payload.contains("\"sub\":\"")) {
                int start = payload.indexOf("\"sub\":\"") + 7;
                int end = payload.indexOf("\"", start);
                return payload.substring(start, end);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
