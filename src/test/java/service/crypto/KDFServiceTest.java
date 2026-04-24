package service.crypto;

import client.crypto.KDFService;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe de test manuelle pour valider la logique KDF.
 */
public class KDFServiceTest {

    public static void main(String[] args) {
        System.out.println("=== TEST KDF SERVICE (Argon2id) ===\n");

        testSaltUniqueness();
        testKEKDeterminism();
        testKEKDifferentPasswords();

        System.out.println("\n=== TOUS LES TESTS SONT TERMINÉS ===");
    }

    /**
     * Vérifie que les sels générés sont uniques.
     */
    private static void testSaltUniqueness() {
        System.out.print("[Test] Unicité du Sel : ");
        Set<String> salts = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            salts.add(KDFService.generateSalt());
        }
        
        if (salts.size() == 100) {
            System.out.println("OK (100 sels uniques générés)");
        } else {
            System.out.println("ÉCHEC (Doublons détectés !)");
        }
    }

    /**
     * Vérifie que le même mot de passe + même sel = même KEK.
     */
    private static void testKEKDeterminism() {
        System.out.print("[Test] Déterminisme KEK : ");
        String password = "MonSuperMotDePasse123!";
        String salt = KDFService.generateSalt();

        byte[] kek1 = KDFService.deriveKEK(password, salt);
        byte[] kek2 = KDFService.deriveKEK(password, salt);

        String b64_1 = Base64.getEncoder().encodeToString(kek1);
        String b64_2 = Base64.getEncoder().encodeToString(kek2);

        if (b64_1.equals(b64_2)) {
            System.out.println("OK (Clés identiques)");
            System.out.println("      -> KEK (Base64) : " + b64_1);
        } else {
            System.out.println("ÉCHEC (Clés différentes !)");
        }
    }

    /**
     * Vérifie que des mots de passe différents produisent des KEK différentes.
     */
    private static void testKEKDifferentPasswords() {
        System.out.print("[Test] Distinction des mots de passe : ");
        String salt = KDFService.generateSalt();
        
        byte[] kek1 = KDFService.deriveKEK("PasswordA", salt);
        byte[] kek2 = KDFService.deriveKEK("PasswordB", salt);

        String b64_1 = Base64.getEncoder().encodeToString(kek1);
        String b64_2 = Base64.getEncoder().encodeToString(kek2);

        if (!b64_1.equals(b64_2)) {
            System.out.println("OK (Clés distinctes)");
        } else {
            System.out.println("ÉCHEC (Même clé générée pour 2 MDP différents !)");
        }
    }
}
