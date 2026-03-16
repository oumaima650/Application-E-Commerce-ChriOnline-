package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Panier implements Serializable {
    private int idPanier;
    private int idClient;
    private List<LignePanier> lignes;
    private double total;
    private LocalDateTime createdAt;

    public Panier() {
        this.lignes = new ArrayList<>();
    }

    public Panier(int idPanier, int idClient, double total, LocalDateTime createdAt) {
        this.idPanier = idPanier;
        this.idClient = idClient;
        this.lignes = new ArrayList<>();
        this.total = total;
        this.createdAt = createdAt;
    }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public List<LignePanier> getLignes() { return lignes; }
    public void setLignes(List<LignePanier> lignes) { this.lignes = lignes; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
