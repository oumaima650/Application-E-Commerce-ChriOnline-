package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class LignePanier implements Serializable {
    private int idPanier;
    private String sku;
    private int quantite;
    private BigDecimal sousTotal;
    private String image;
    private boolean selected = true; // Par défaut, tout est sélectionné

    public LignePanier() {}

    public LignePanier(int idPanier, String sku, int quantite, BigDecimal sousTotal) {
        this.idPanier = idPanier;
        this.sku = sku;
        this.quantite = quantite;
        this.sousTotal = sousTotal;
    }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getImage() { return image; }

    public void setImage(String image) { this.image = image; }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public BigDecimal getSousTotal() { return sousTotal; }
    public void setSousTotal(BigDecimal sousTotal) { this.sousTotal = sousTotal; }
}
