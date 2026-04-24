package model;

import java.time.LocalDateTime;

public class Administrateur extends Utilisateur {
    
    public Administrateur() {
        super();
    }

    public Administrateur(int idUtilisateur, String email, String motDePasse, String encryptionSalt, String wrappedDek, boolean twoFactorEnabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(idUtilisateur, email, motDePasse, encryptionSalt, wrappedDek, twoFactorEnabled, createdAt, updatedAt);
    }
}
