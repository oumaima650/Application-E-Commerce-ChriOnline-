package model;

import java.io.Serializable;

public class ProduitVarValeur implements Serializable {
    private int idPVV;
    private int idProduit;
    private int idVariante;
    private String valeur;

    public ProduitVarValeur() {}

    public ProduitVarValeur(int idPVV, int idProduit, int idVariante, String valeur) {
        this.idPVV = idPVV;
        this.idProduit = idProduit;
        this.idVariante = idVariante;
        this.valeur = valeur;
    }

    public int getIdPVV() { return idPVV; }
    public void setIdPVV(int idPVV) { this.idPVV = idPVV; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getIdVariante() { return idVariante; }
    public void setIdVariante(int idVariante) { this.idVariante = idVariante; }

    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }

    @Override
    public String toString() {
        return "ProduitVarValeur{" +
                "idPVV=" + idPVV +
                ", idProduit=" + idProduit +
                ", idVariante=" + idVariante +
                ", valeur='" + valeur + '\'' +
                '}';
    }
}
