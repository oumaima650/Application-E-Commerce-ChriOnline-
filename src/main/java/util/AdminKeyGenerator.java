package util;

import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;
import java.io.File;
import java.math.BigInteger;

/**
 * Utility to generate an Admin RSA KeyPair and store it in a PKCS12 (.p12) file.
 * Also prints the Base64 Public Key to be stored in the database.
 */
public class AdminKeyGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== ChriOnline Admin Key Generator ===");
        System.out.print("Enter Admin Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter Password for .p12 file: ");
        String password;
        if (System.console() != null) {
            password = new String(System.console().readPassword());
        } else {
            // Fallback for IDEs where Console might be null
            password = scanner.nextLine().trim();
            System.out.println("⚠️ Warning: Password visibility could not be masked in this terminal.");
        }

        try {
            // 1. Generate RSA Key Pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            // 2. Create the keys directory if it doesn't exist
            File keysDir = new File("keys");
            if (!keysDir.exists()) {
                keysDir.mkdirs();
            }

            // 3. Generate a dummy self-signed certificate (required for KeyStore)
            // Note: Since we only need the key for signing, the cert details don't matter much
            // We use a simplified approach as standard Java doesn't have an easy X509 builder.
            // For a production app, you'd use BouncyCastle, but here we'll use a hack or just the KeyStore.
            
            // Actually, we can't easily generate a valid X509Certificate without external libs or sun classes.
            // Let's use the keytool approach via ProcessBuilder for the certificate generation
            // but keep the Java wrapper for ease of use and public key extraction.
            
            String p12Path = "keys/admin.p12";
            
            System.out.println("Generating " + p12Path + "...");
            
            ProcessBuilder pb = new ProcessBuilder(
                "keytool", "-genkeypair",
                "-alias", email,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-keystore", p12Path,
                "-storetype", "PKCS12",
                "-storepass", password,
                "-keypass", password,
                "-dname", "CN=" + email + ", OU=Admin, O=ChriOnline, L=Casablanca, C=MA",
                "-validity", "3650"
            );
            
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("\n✅ Successfully generated " + p12Path);
                
                // 4. Extract and print Public Key in Base64
                KeyStore ks = KeyStore.getInstance("PKCS12");
                try (java.io.FileInputStream fis = new java.io.FileInputStream(p12Path)) {
                    ks.load(fis, password.toCharArray());
                }
                
                java.security.cert.Certificate cert = ks.getCertificate(email);
                
                // 4. Store Public Key in HashiCorp Vault
                System.out.println("Enrolling public key in Vault...");
                security.VaultClient.storePublicKey(email, cert.getPublicKey());
                
                System.out.println("\n✅ Successfully enrolled public key in Vault.");
                System.out.println("-----------------------\n");
                
            } else {
                System.err.println("❌ Error: keytool execution failed.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
