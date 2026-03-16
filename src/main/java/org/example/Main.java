package org.example;

import shared.RequestType;
import shared.Requete;
import shared.Reponse;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int    SERVER_PORT = 5555;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║       Welcome to the App     ║");
        System.out.println("╚══════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleRegister();
                case "2" -> handleLogin();
                case "3" -> {
                    System.out.println("\nGoodbye!");
                    running = false;
                }
                default -> System.out.println("\nChoix invalide. Entrez 1, 2 ou 3.\n");
            }
        }
        scanner.close();
    }

    // ── MENU ──────────────────────────────────────────────────────────────
    private static void printMainMenu() {
        System.out.println("┌──────────────────────────────┐");
        System.out.println("│  1. Register                 │");
        System.out.println("│  2. Login                    │");
        System.out.println("│  3. Quit                     │");
        System.out.println("└──────────────────────────────┘");
        System.out.print("Your choice: ");
    }

    // ── REGISTER ──────────────────────────────────────────────────────────
    private static void handleRegister() {
        System.out.println("\n── Register ─────────────────────");

        System.out.print("  Email     : ");
        String email = scanner.nextLine().trim();

        System.out.print("  Password  : ");
        String password = scanner.nextLine().trim();

        System.out.print("  Last name : ");
        String nom = scanner.nextLine().trim();

        System.out.print("  First name: ");
        String prenom = scanner.nextLine().trim();

        System.out.print("  Phone     : ");
        String telephone = scanner.nextLine().trim();

        // build the Requete the same way login does it
        Map<String, Object> params = new HashMap<>();
        params.put("email",      email);
        params.put("motDePasse", password);
        params.put("nom",        nom);
        params.put("prenom",     prenom);
        params.put("telephone",  telephone);

        Requete requete = new Requete(RequestType.REGISTER, params, null);

        System.out.println("\nSending request to server...");
        Reponse reponse = sendToServer(requete);

        if (reponse == null) return;   // connection error already printed

        if (reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
            System.out.println("  ┌─────────────────────────────");
            System.out.println("  │ ID        : " + reponse.getDonnees().get("userId"));
            System.out.println("  │ Email     : " + reponse.getDonnees().get("email"));
            System.out.println("  │ Name      : " + reponse.getDonnees().get("prenom")
                    + " " + reponse.getDonnees().get("nom"));
            System.out.println("  │ Created at: " + reponse.getDonnees().get("createdAt"));
            System.out.println("  └─────────────────────────────");
        } else {
            System.out.println("failed," + reponse.getMessage());
        }
        System.out.println();
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────
    private static void handleLogin() {
        System.out.println("\n── Login ────────────────────────");

        System.out.print("  Email   : ");
        String email = scanner.nextLine().trim();

        System.out.print("  Password: ");
        String password = scanner.nextLine().trim();

        Map<String, Object> params = new HashMap<>();
        params.put("email",      email);
        params.put("motDePasse", password);

        Requete requete = new Requete(RequestType.LOGIN, params, null);

        System.out.println("\nSending request to server...");
        Reponse reponse = sendToServer(requete);

        if (reponse == null) return;

        if (reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
            System.out.println("  ┌─────────────────────────────");
            System.out.println("  │ userId : " + reponse.getDonnees().get("userId"));
            System.out.println("  │ Type   : " + reponse.getDonnees().get("typeUtilisateur"));
            System.out.println("  │ Token  : " + reponse.getDonnees().get("token"));
            System.out.println("  └─────────────────────────────");
            
            // Go to user menu
            String token = (String) reponse.getDonnees().get("token");
            menuUtilisateur(token);
        } else {
            System.out.println("failed," + reponse.getMessage());
        }
    }

    // ── USER MENU ─────────────────────────────────────────────────────────
    private static void menuUtilisateur(String token) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║         User Menu          ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.println("┌──────────────────────────────┐");
            System.out.println("│  1. Mon Panier               │");
            System.out.println("│  2. Voir les produits        │");
            System.out.println("│  3. Se déconnecter           │");
            System.out.println("└──────────────────────────────┘");
            System.out.print("Your choice: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> handlePanierMenu(token);
                case "2" -> System.out.println("\n  (Fonctionnalité Produits à venir...)");
                case "3" -> {
                    handleLogout(token);
                    inMenu = false;
                }
                default -> System.out.println("\nChoix invalide.");
            }
        }
    }

    private static void handleLogout(String token) {
        Requete requete = new Requete(RequestType.LOGOUT, null, token);
        System.out.println("\nDisconnecting...");
        Reponse reponse = sendToServer(requete);
        if (reponse != null && reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
        } else {
            System.out.println("Failed to logout.");
        }
    }

    // ── PANIER MENU ───────────────────────────────────────────────────────
    private static void handlePanierMenu(String token) {
        boolean inPanier = true;
        while (inPanier) {
            System.out.println("\n  ── Mon Panier ─────────────");
            System.out.println("  1. Voir mon panier");
            System.out.println("  2. Ajouter un produit");
            System.out.println("  3. Retirer un produit");
            System.out.println("  4. Vider le panier");
            System.out.println("  5. Retour");
            System.out.print("  Choix: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> fetchCart(token);
                case "2" -> addToCart(token);
                case "3" -> removeFromCart(token);
                case "4" -> clearCart(token);
                case "5" -> inPanier = false;
                default -> System.out.println("  Choix invalide.");
            }
        }
    }

    private static void fetchCart(String token) {
        Requete requete = new Requete(RequestType.GET_CART, null, token);
        Reponse reponse = sendToServer(requete);
        if (reponse != null && reponse.isSucces()) {
            System.out.println("\n" + reponse.getMessage());
            Map<String, Object> donnees = reponse.getDonnees();
            if (donnees != null) {
                System.out.println("  Total : " + donnees.get("total") + " DH");
                java.util.List<Map<String, Object>> lignes = (java.util.List<Map<String, Object>>) donnees.get("lignes");
                for (Map<String, Object> l : lignes) {
                    System.out.println("   - SKU: " + l.get("sku") + " | Qté: " + l.get("quantite"));
                }
            }
        } else if (reponse != null) {
            System.out.println("failed," + reponse.getMessage());
        }
    }

    private static void addToCart(String token) {
        System.out.print("  SKU du produit: ");
        String sku = scanner.nextLine().trim();
        System.out.print("  Quantité: ");
        int qte = 1;
        try {
            qte = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Quantité invalide, utilisation de 1.");
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("sku", sku);
        params.put("quantite", qte);
        Requete requete = new Requete(RequestType.ADD_TO_CART, params, token);
        Reponse reponse = sendToServer(requete);
        
        if (reponse != null && reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
        } else if (reponse != null) {
            System.out.println("failed," + reponse.getMessage());
        }
    }

    private static void removeFromCart(String token) {
        System.out.print("  SKU du produit à retirer: ");
        String sku = scanner.nextLine().trim();
        
        Map<String, Object> params = new HashMap<>();
        params.put("sku", sku);
        Requete requete = new Requete(RequestType.REMOVE_FROM_CART, params, token);
        Reponse reponse = sendToServer(requete);
        
        if (reponse != null && reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
        } else if (reponse != null) {
            System.out.println("failed," + reponse.getMessage());
        }
    }

    private static void clearCart(String token) {
        Requete requete = new Requete(RequestType.CLEAR_CART, null, token);
        Reponse reponse = sendToServer(requete);
        if (reponse != null && reponse.isSucces()) {
            System.out.println("Success," + reponse.getMessage());
        } else if (reponse != null) {
            System.out.println("failed," + reponse.getMessage());
        }
    }

    // ── TCP SEND ──────────────────────────────────────────────────────────
    /**
     * Opens a TCP connection, sends the Requete as a serialized object,
     * reads back the Reponse, then closes the socket.
     */
    private static Reponse sendToServer(Requete requete) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(requete);   // send Requete to server
            out.flush();

            return (Reponse) in.readObject();   // read Reponse back

        } catch (IOException e) {
            System.out.println("Cannot reach server at "
                    + SERVER_HOST + ":" + SERVER_PORT + " — " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Unknown response type: " + e.getMessage());
            return null;
        }
    }
}
