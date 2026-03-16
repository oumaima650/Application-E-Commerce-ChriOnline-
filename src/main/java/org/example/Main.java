package com.dao;

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
                    System.out.println("\n👋 Goodbye!");
                    running = false;
                }
                default -> System.out.println("\n⚠️  Choix invalide. Entrez 1, 2 ou 3.\n");
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

        System.out.println("\n  ⏳ Sending request to server...");
        Reponse reponse = sendToServer(requete);

        if (reponse == null) return;   // connection error already printed

        if (reponse.isSucces()) {
            System.out.println("  ✅ " + reponse.getMessage());
            System.out.println("  ┌─────────────────────────────");
            System.out.println("  │ ID        : " + reponse.getDonnees().get("userId"));
            System.out.println("  │ Email     : " + reponse.getDonnees().get("email"));
            System.out.println("  │ Name      : " + reponse.getDonnees().get("prenom")
                    + " " + reponse.getDonnees().get("nom"));
            System.out.println("  │ Created at: " + reponse.getDonnees().get("createdAt"));
            System.out.println("  └─────────────────────────────");
        } else {
            System.out.println("  ❌ " + reponse.getMessage());
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

        System.out.println("\n  ⏳ Sending request to server...");
        Reponse reponse = sendToServer(requete);

        if (reponse == null) return;

        if (reponse.isSucces()) {
            System.out.println("  ✅ " + reponse.getMessage());
            System.out.println("  ┌─────────────────────────────");
            System.out.println("  │ userId : " + reponse.getDonnees().get("userId"));
            System.out.println("  │ Type   : " + reponse.getDonnees().get("typeUtilisateur"));
            System.out.println("  │ Token  : " + reponse.getDonnees().get("token"));
            System.out.println("  └─────────────────────────────");
        } else {
            System.out.println("  ❌ " + reponse.getMessage());
        }
        System.out.println();
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
            System.out.println("  ❌ Cannot reach server at "
                    + SERVER_HOST + ":" + SERVER_PORT + " — " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("  ❌ Unknown response type: " + e.getMessage());
            return null;
        }
    }
}
