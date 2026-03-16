package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Panier implements Serializable {
    private int idPanier;
    private int idClient;
    private List<LignePanier> lignes;
    private LocalDateTime createdAt;

    public Panier() {
        this.lignes = new ArrayList<>();
    }

    public Panier(int idPanier, int idClient, LocalDateTime createdAt) {
        this.idPanier = idPanier;
        this.idClient = idClient;
        this.lignes = new ArrayList<>();
        this.createdAt = createdAt;
    }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public List<LignePanier> getLignes() { return lignes; }
    public void setLignes(List<LignePanier> lignes) { this.lignes = lignes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
