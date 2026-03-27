package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public class Notification implements Serializable {
    public enum StatutNotification { LU, NON_LU }

    private int idNotification;
    private int idUtilisateur;
    private Integer idCommande;
    private String contenu;
    private StatutNotification statut;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int idNotification, int idUtilisateur, Integer idCommande, String contenu, StatutNotification statut, LocalDateTime createdAt) {
        this.idNotification = idNotification;
        this.idUtilisateur = idUtilisateur;
        this.idCommande = idCommande;
        this.contenu = contenu;
        this.statut = statut;
        this.createdAt = createdAt;
    }

    public int getIdNotification() { return idNotification; }
    public void setIdNotification(int idNotification) { this.idNotification = idNotification; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public Integer getIdCommande() { return idCommande; }
    public void setIdCommande(Integer idCommande) { this.idCommande = idCommande; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public StatutNotification getStatut() { return statut; }
    public void setStatut(StatutNotification statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
