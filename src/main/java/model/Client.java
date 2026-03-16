package model;

import java.time.LocalDateTime;

public class Client extends Utilisateur {
    private String nom;
    private String prenom;
    private String telephone;
    private LocalDateTime deletedAt;

    public Client() {
        super();
    }

    public Client(int idUtilisateur, String email, String motDePasse, LocalDateTime createdAt, LocalDateTime updatedAt,
                  String nom, String prenom, String telephone, LocalDateTime deletedAt) {
        super(idUtilisateur, email, motDePasse, createdAt, updatedAt);
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.deletedAt = deletedAt;
    }

    // Getters and Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
