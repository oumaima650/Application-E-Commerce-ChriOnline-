package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class SKU implements Serializable {
    private String sku;
    private BigDecimal prix;
    private int quantite;
    private String image;

    public SKU() {}

    public SKU(String sku, BigDecimal prix, int quantite, String image) {
        this.sku = sku;
        this.prix = prix;
        this.quantite = quantite;
        this.image = image;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return "SKU{" +
                "sku='" + sku + '\'' +
                ", prix=" + prix +
                ", quantite=" + quantite +
                ", image='" + image + '\'' +
                '}';
    }
}
