package model;

import java.io.Serializable;

public class LigneCommande implements Serializable {
    private int idCommande;
    private String sku;
    private String nomProduit;
    private int quantite;
    private double prixAchat;

    public LigneCommande() {}

    public LigneCommande(int idCommande, String sku, int quantite, double prixAchat) {
        this.idCommande = idCommande;
        this.sku = sku;
        this.quantite = quantite;
        this.prixAchat = prixAchat;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixAchat() { return prixAchat; }
    public void setPrixAchat(double prixAchat) { this.prixAchat = prixAchat; }
}


