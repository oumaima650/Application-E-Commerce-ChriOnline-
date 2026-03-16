package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Adresse implements Serializable {
    private int idAdresse;
    private int idClient;
    private String addresseComplete;
    private String ville;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Adresse() {}

    public Adresse(int idAdresse, int idClient, String addresseComplete, String ville, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.idAdresse = idAdresse;
        this.idClient = idClient;
        this.addresseComplete = addresseComplete;
        this.ville = ville;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public int getIdAdresse() { return idAdresse; }
    public void setIdAdresse(int idAdresse) { this.idAdresse = idAdresse; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public String getAddresseComplete() { return addresseComplete; }
    public void setAddresseComplete(String addresseComplete) { this.addresseComplete = addresseComplete; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
