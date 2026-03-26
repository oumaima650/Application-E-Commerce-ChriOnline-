package model;

import java.time.LocalDateTime;
import java.io.Serializable;

public abstract class Utilisateur implements Serializable {
    private int idUtilisateur;
    private String email;
    private String motDePasse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Utilisateur() {}

    public Utilisateur(int idUtilisateur, String email, String motDePasse, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idUtilisateur = idUtilisateur;
        this.email = email;
        this.motDePasse = motDePasse;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    // Alias pour compatibilité UI
    public int getId() { return idUtilisateur; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
