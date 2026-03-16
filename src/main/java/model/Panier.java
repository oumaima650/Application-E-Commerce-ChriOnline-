package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Panier implements Serializable {
    private int idPanier;
    private int idClient;
    private LocalDateTime createdAt;

    public Panier() {}

    public Panier(int idPanier, int idClient, LocalDateTime createdAt) {
        this.idPanier = idPanier;
        this.idClient = idClient;
        this.createdAt = createdAt;
    }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
