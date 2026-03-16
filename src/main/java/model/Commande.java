package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import model.enums.StatutCommande;

public class Commande implements Serializable {
    private int idCommande;
    private int idClient;
    private Integer idAdresse;
    private String reference;
    private StatutCommande statut;
    private LocalDateTime dateLivraisonPrevue;
    private LocalDateTime dateLivraisonReelle;
    private List<LigneCommande> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Commande() {
        this.lignes = new ArrayList<>();
    }

    public Commande(int idCommande, int idClient, Integer idAdresse, String reference,
                    StatutCommande statut, LocalDateTime dateLivraisonPrevue, 
                    LocalDateTime dateLivraisonReelle, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCommande = idCommande;
        this.idClient = idClient;
        this.idAdresse = idAdresse;
        this.reference = reference;
        this.statut = statut;
        this.dateLivraisonPrevue = dateLivraisonPrevue;
        this.dateLivraisonReelle = dateLivraisonReelle;
        this.lignes = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public Integer getIdAdresse() { return idAdresse; }
    public void setIdAdresse(Integer idAdresse) { this.idAdresse = idAdresse; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public LocalDateTime getDateLivraisonReelle() { return dateLivraisonReelle; }
    public void setDateLivraisonReelle(LocalDateTime dateLivraisonReelle) { this.dateLivraisonReelle = dateLivraisonReelle; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
