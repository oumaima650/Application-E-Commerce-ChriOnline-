package model;

import java.time.LocalDateTime;

public class Administrateur extends Utilisateur {
    
    public Administrateur() {
        super();
    }

    public Administrateur(int idUtilisateur, String email, String motDePasse, boolean twoFactorEnabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(idUtilisateur, email, motDePasse, twoFactorEnabled, createdAt, updatedAt);
    }
}
