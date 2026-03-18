package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Produit implements Serializable {
    private int idProduit;
    private String nom;
    private String description;
    private LocalDateTime createdAt;

    public Produit() {}

    public Produit(int idProduit, String nom, String description, LocalDateTime createdAt) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Produit{" +
                "idProduit=" + idProduit +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
