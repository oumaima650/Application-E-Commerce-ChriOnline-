package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import model.enums.StatutCommande;

public class Commande implements Serializable {
    private int idCommande;
    private int idClient;
    private Integer idAdresse;
    private StatutCommande statut;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Commande() {}

    public Commande(int idCommande, int idClient, Integer idAdresse, 
                    StatutCommande statut, LocalDateTime dateLivraisonPrevue, 
                    LocalDateTime dateLivraisonReelle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCommande = idCommande;
        this.idClient = idClient;
        this.idAdresse = idAdresse;
        this.statut = statut;
        this.dateLivraisonPrevue = dateLivraisonPrevue;
        this.dateLivraisonReelle = dateLivraisonReelle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public Integer getIdAdresse() { return idAdresse; }
    public void setIdAdresse(Integer idAdresse) { this.idAdresse = idAdresse; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public LocalDateTime getDateLivraisonReelle() { return dateLivraisonReelle; }
    public void setDateLivraisonReelle(LocalDateTime dateLivraisonReelle) { this.dateLivraisonReelle = dateLivraisonReelle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
