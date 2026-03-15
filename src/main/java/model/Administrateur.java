package model;

import java.time.LocalDateTime;

public class Administrateur extends Utilisateur {
    
    public Administrateur() {
        super();
    }

    public Administrateur(int idUtilisateur, String email, String motDePasse, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(idUtilisateur, email, motDePasse, createdAt, updatedAt);
    }
}
