package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Categorie implements Serializable {
    private int idCategorie;
    private String nom;
    private String description;
    private List<Produit> produits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Categorie() {
        this.produits = new ArrayList<>();
    }

    public Categorie(int idCategorie, String nom, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCategorie = idCategorie;
        this.nom = nom;
        this.description = description;
        this.produits = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Produit> getProduits() { return produits; }
    public void setProduits(List<Produit> produits) { this.produits = produits; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
