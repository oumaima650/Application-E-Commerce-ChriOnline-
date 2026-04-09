package model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Client extends Utilisateur {
    private String nom;
    private String prenom;
    private String telephone;
    private List<Adresse> adresses;
    private LocalDateTime deletedAt;
    private String statut; 
    private java.time.LocalDate dateNaissance;

    public Client() {
        super();
        this.adresses = new ArrayList<>();
    }

    public Client(int idUtilisateur, String email, String motDePasse, boolean twoFactorEnabled, LocalDateTime createdAt, LocalDateTime updatedAt,
                  String nom, String prenom, String telephone, java.time.LocalDate dateNaissance, LocalDateTime deletedAt) {
        super(idUtilisateur, email, motDePasse, twoFactorEnabled, createdAt, updatedAt);
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.dateNaissance = dateNaissance;
        this.adresses = new ArrayList<>();
        this.deletedAt = deletedAt;
        this.statut = "EN_ATTENTE";
    }


    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public List<Adresse> getAdresses() { return adresses; }
    public void setAdresses(List<Adresse> adresses) { this.adresses = adresses; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public java.time.LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(java.time.LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
}
