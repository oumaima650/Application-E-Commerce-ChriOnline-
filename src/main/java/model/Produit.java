package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Produit implements Serializable {
    private int idProduit;
    private int idCategorie;
    private String nom;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Produit() {}

    public Produit(int idProduit, int idCategorie, String nom, String description, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.idProduit = idProduit;
        this.idCategorie = idCategorie;
        this.nom = nom;
        this.description = description;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        return "Produit{" +
                "idProduit=" + idProduit +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
