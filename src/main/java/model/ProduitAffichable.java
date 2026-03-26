package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour représenter un produit avec toutes les informations nécessaires pour l'affichage
 * Combine Produit + SKU + Categorie + Avis
 */
public class ProduitAffichable implements Serializable {
    private int idProduit;
    private String nom;
    private String description;
    private String sku;
    private BigDecimal prix;
    private BigDecimal prixOriginal; // Pour les promotions
    private int stock;
    private String image;
    private String categorie;
    private int idCategorie;
    private double noteMoyenne; // Moyenne des avis
    private int nombreAvis;
    private LocalDateTime createdAt;
    private boolean estPromotion; // Indicateur de promotion

    public ProduitAffichable() {}

    public ProduitAffichable(int idProduit, String nom, String description, 
                           String sku, BigDecimal prix, int stock, String image,
                           String categorie, int idCategorie) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.sku = sku;
        this.prix = prix;
        this.prixOriginal = prix; // Par défaut, pas de promotion
        this.stock = stock;
        this.image = image;
        this.categorie = categorie;
        this.idCategorie = idCategorie;
        this.noteMoyenne = 0.0;
        this.nombreAvis = 0;
        this.createdAt = LocalDateTime.now();
        this.estPromotion = false;
    }

    // Getters et Setters
    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public BigDecimal getPrixOriginal() { return prixOriginal; }
    public void setPrixOriginal(BigDecimal prixOriginal) { this.prixOriginal = prixOriginal; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isEstPromotion() { return estPromotion; }
    public void setEstPromotion(boolean estPromotion) { this.estPromotion = estPromotion; }

    // Méthodes utilitaires
    public void appliquerPromotion(BigDecimal prixPromo) {
        if (prixPromo.compareTo(prix) < 0) {
            this.prixOriginal = this.prix;
            this.prix = prixPromo;
            this.estPromotion = true;
        }
    }

    public double getPourcentageReduction() {
        if (!estPromotion) return 0.0;
        return prixOriginal.subtract(prix)
                .divide(prixOriginal, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    @Override
    public String toString() {
        return "ProduitAffichable{" +
                "idProduit=" + idProduit +
                ", nom='" + nom + '\'' +
                ", prix=" + prix + (estPromotion ? " (promo de " + getPourcentageReduction() + "%)" : "") +
                ", stock=" + stock +
                ", categorie='" + categorie + '\'' +
                ", note=" + noteMoyenne + " (" + nombreAvis + " avis)" +
                '}';
    }
}
