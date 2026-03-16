package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Produit implements Serializable {
    private int idProduit;
    private int idCategorie;
    private String nom;
    private String description;
    private String image;
    private List<SKU> skus;
    private List<Avis> avis;
    private LocalDateTime createdAt;

    public Produit() {
        this.skus = new ArrayList<>();
        this.avis = new ArrayList<>();
    }

    public Produit(int idProduit, int idCategorie, String nom, String description, String image, LocalDateTime createdAt) {
        this.idProduit = idProduit;
        this.idCategorie = idCategorie;
        this.nom = nom;
        this.description = description;
        this.image = image;
        this.skus = new ArrayList<>();
        this.avis = new ArrayList<>();
        this.createdAt = createdAt;
    }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public List<SKU> getSkus() { return skus; }
    public void setSkus(List<SKU> skus) { this.skus = skus; }

    public List<Avis> getAvis() { return avis; }
    public void setAvis(List<Avis> avis) { this.avis = avis; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
