package model;

import java.io.Serializable;

public class LignePanier implements Serializable {
    private int idPanier;
    private String sku;
    private int quantite;

    public LignePanier() {}

    public LignePanier(int idPanier, String sku, int quantite) {
        this.idPanier = idPanier;
        this.sku = sku;
        this.quantite = quantite;
    }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
}
