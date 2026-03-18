package client.models;

import java.util.ArrayList;
import java.util.List;

public class MockDataService {
    
    // Simule la classe Produit pour l'UI
    public static class ProduitMock {
        public String id;
        public String image;
        public String nom;
        public String categorie;
        public String prix;
        public int stock;

        public ProduitMock(String id, String image, String nom, String categorie, String prix, int stock) {
            this.id = id; this.image = image; this.nom = nom; 
            this.categorie = categorie; this.prix = prix; this.stock = stock;
        }
        
        // Getters (requis pour PropertyValueFactory dans TableView)
        public String getId() { return id; }
        public String getImage() { return image; }
        public String getNom() { return nom; }
        public String getCategorie() { return categorie; }
        public String getPrix() { return prix; }
        public int getStock() { return stock; }
    }
    
    // Simule la classe Commande
    public static class CommandeMock {
        public String id;
        public String client;
        public String date;
        public String total;
        public String statut;

        public CommandeMock(String id, String client, String date, String total, String statut) {
            this.id = id; this.client = client; this.date = date; this.total = total; this.statut = statut;
        }
        
        public String getId() { return id; }
        public String getClient() { return client; }
        public String getDate() { return date; }
        public String getTotal() { return total; }
        public String getStatut() { return statut; }
        
        public void setStatut(String statut) { this.statut = statut; }
    }
    
    // Simule la classe Utilisateur
    public static class UserMock {
        public String id;
        public String nom;
        public String email;
        public String role;

        public UserMock(String id, String nom, String email, String role) {
            this.id = id; this.nom = nom; this.email = email; this.role = role;
        }
        
        public String getId() { return id; }
        public String getNom() { return nom; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }

    public static List<ProduitMock> getMockProducts() {
        List<ProduitMock> realProducts = new ArrayList<>();
        try {
            dao.ProduitDAO pDao = new dao.ProduitDAO();
            List<model.Produit> dbProducts = pDao.getAll();
            
            for (model.Produit p : dbProducts) {
                // On mappe les données de la base vers notre modèle d'interface graphique
                realProducts.add(new ProduitMock(
                    String.valueOf(p.getIdProduit()),
                    "🏷️", // Pas d'image dans la DB pour l'instant
                    p.getNom() != null ? p.getNom() : "Produit Inconnu",
                    "Générique", // Pas de catégorie dans Produit entity 
                    "0 MAD", // Pas de prix dans Produit entity
                    0 // Pas de stock dans Produit entity
                ));
            }
        } catch (Exception e) {
            System.err.println("Impossible de charger les produits depuis la BDD: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Si la base est vide ou erreur, on garde quelques mocks pour ne pas casser l'UI
        if (realProducts.isEmpty()) {
            return List.of(
                new ProduitMock("1", "📱", "iPhone 15 Pro", "Smartphones", "12500 MAD", 24),
                new ProduitMock("2", "📱", "Samsung Galaxy S24", "Smartphones", "11000 MAD", 15)
            );
        }
        
        return realProducts;
    }
    
    public static List<CommandeMock> getMockOrders() {
        return List.of(
            new CommandeMock("CMD-001", "Amine", "15/03/2026", "25000 MAD", "Livrée"),
            new CommandeMock("CMD-002", "Sara", "16/03/2026", "2800 MAD", "Expédiée"),
            new CommandeMock("CMD-003", "Youssef", "17/03/2026", "11000 MAD", "En attente")
        );
    }
    
    public static List<UserMock> getMockUsers() {
        return List.of(
            new UserMock("1", "Amine ADMIN", "admin@chri.ma", "Admninistrateur"),
            new UserMock("2", "Sara Client", "sara@gmail.com", "Client"),
            new UserMock("3", "Youssef Client", "youssef@test.com", "Client")
        );
    }
}
