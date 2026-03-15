package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class LigneCommande implements Serializable {
    private int idCommande;
    private String sku;
    private int quantite;
    private BigDecimal prixAchat;

    public LigneCommande() {}

    public LigneCommande(int idCommande, String sku, int quantite, BigDecimal prixAchat) {
        this.idCommande = idCommande;
        this.sku = sku;
        this.quantite = quantite;
        this.prixAchat = prixAchat;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public BigDecimal getPrixAchat() { return prixAchat; }
    public void setPrixAchat(BigDecimal prixAchat) { this.prixAchat = prixAchat; }
}
