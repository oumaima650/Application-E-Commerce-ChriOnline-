package model;

import java.io.Serializable;

public class CarteBancaire implements Serializable {
    private int idCarte;
    private int idClient;
    private String numeroCarte;
    private String typeCarte;

    public CarteBancaire() {}

    public CarteBancaire(int idCarte, int idClient, String numeroCarte, String typeCarte) {
        this.idCarte = idCarte;
        this.idClient = idClient;
        this.numeroCarte = numeroCarte;
        this.typeCarte = typeCarte;
    }

    public int getIdCarte() { return idCarte; }
    public void setIdCarte(int idCarte) { this.idCarte = idCarte; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public String getNumeroCarte() { return numeroCarte; }
    public void setNumeroCarte(String numeroCarte) { this.numeroCarte = numeroCarte; }

    public String getTypeCarte() { return typeCarte; }
    public void setTypeCarte(String typeCarte) { this.typeCarte = typeCarte; }
}
