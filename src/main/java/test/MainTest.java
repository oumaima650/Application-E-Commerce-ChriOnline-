package test;

import dao.AdresseDAO;
import dao.CarteBancaireDAO;
import dao.ConnexionBDD;
import dao.PaiementDAO;
import model.Adresse;
import model.CarteBancaire;
import model.Paiement;
import model.enums.MethodePaiement;
import model.enums.StatutPaiement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class MainTest {
    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS DAO ===");

        // 1. Tester la connexion à la base de données
        Connection conn = ConnexionBDD.getConnection();
        if (conn == null) {
            System.err.println("❌ Échec de la connexion. Vérifiez que MySQL est lancé et que la base 'chri_online' existe.");
            return;
        }
        System.out.println("✅ Connexion à la base de données réussie.");

        // 2. Tester AdresseDAO
        System.out.println("\n--- Test AdresseDAO ---");
        AdresseDAO adresseDAO = new AdresseDAO();
        Adresse adr = new Adresse();
        // ATTENTION : L'idClient = 1 doit exister dans la table Client de votre base de données!
        adr.setIdClient(3);
        adr.setAddresseComplete("123 Rue de la République");
        adr.setVille("Paris");
        adr.setCreatedAt(LocalDateTime.now());

        boolean isAdrCreated = adresseDAO.create(adr);
        if (isAdrCreated) {
            System.out.println("✅ Adresse insérée avec succès ! Nouvel ID: " + adr.getIdAdresse());
            
            // Tester la récupération
            Adresse fetchedAdr = adresseDAO.findById(adr.getIdAdresse());
            if (fetchedAdr != null) {
                System.out.println("✅ Adresse récupérée : " + fetchedAdr.getAddresseComplete() + " - " + fetchedAdr.getVille());
                
                // Tester la mise à jour
                fetchedAdr.setVille("Lyon");
                if (adresseDAO.update(fetchedAdr)) {
                    System.out.println("✅ Adresse mise à jour (Nouvelle ville: Lyon)");
                } else {
                    System.err.println("❌ Échec de la mise à jour de l'adresse.");
                }
            } else {
                 System.err.println("❌ Impossible de récupérer l'adresse.");
            }
            
            // Tester findByClient
            List<Adresse> listAdr = adresseDAO.findByClient(3);
            System.out.println("✅ Nombre d'adresses pour le client 3 : " + listAdr.size());

            // Tester la suppression (soft delete)
            if (adresseDAO.delete(adr.getIdAdresse())) {
                System.out.println("✅ Suppression (soft delete) de l'adresse réussie.");
            } else {
                System.err.println("❌ Échec de la suppression de l'adresse.");
            }
        } else {
            System.err.println("❌ Échec de l'insertion de l'adresse. (Vérifiez si le Client avec ID=3 existe vraiment)");
        }

        // 3. Tester PaiementDAO
        System.out.println("\n--- Test PaiementDAO ---");
        PaiementDAO paiementDAO = new PaiementDAO();
        Paiement paiement = new Paiement();
        // ATTENTION : L'idCommande = 1 doit exister dans la table Commande de votre base de données!
        paiement.setIdCommande(1); 
        paiement.setMontant(new BigDecimal("99.99"));
        paiement.setStatutPaiement(StatutPaiement.EN_ATTENTE);
        paiement.setMethodePaiement(MethodePaiement.CASH);
        paiement.setDatePaiement(LocalDateTime.now());

        boolean isPaieCreated = paiementDAO.create(paiement);
        if (isPaieCreated) {
            System.out.println("✅ Paiement inséré avec succès ! Nouvel ID: " + paiement.getIdPaiement());

            // Tester la récupération
            Paiement fetchedPaie = paiementDAO.findById(paiement.getIdPaiement());
            if (fetchedPaie != null) {
                 System.out.println("✅ Paiement récupéré - Montant : " + fetchedPaie.getMontant() + " - Statut : " + fetchedPaie.getStatutPaiement());
                 
                 // Tester la mise à jour du statut
                 if (paiementDAO.updateStatut(fetchedPaie.getIdPaiement(), StatutPaiement.APPROUVE)) {
                     System.out.println("✅ Statut du paiement mis à jour (Nouveau statut: APPROUVE)");
                 } else {
                     System.err.println("❌ Échec de la mise à jour du statut du paiement.");
                 }
            } else {
                 System.err.println("❌ Impossible de récupérer le paiement.");
            }

            // Tester findByCommande
            List<Paiement> listPaie = paiementDAO.findByCommande(1);
            System.out.println("✅ Nombre de paiements pour la commande 1 : " + listPaie.size());
        } else {
            System.err.println("❌ Échec de l'insertion du paiement. (Vérifiez si la Commande avec ID=1 existe vraiment)");
        }
        
        // 4. Tester CarteBancaireDAO
        System.out.println("\n--- Test CarteBancaireDAO ---");
        CarteBancaireDAO carteDAO = new CarteBancaireDAO();
        CarteBancaire carte = new CarteBancaire();
        // ATTENTION : L'idClient = 1 doit exister dans la table Client
        carte.setIdClient(4);
        carte.setNumeroCarte("1234");
        carte.setTypeCarte("VISA");
        
        boolean isCarteCreated = carteDAO.create(carte);
        if (isCarteCreated) {
            System.out.println("✅ Carte bancaire insérée ! Nouvel ID: " + carte.getIdCarte());
            
            CarteBancaire fetchedCarte = carteDAO.findById(carte.getIdCarte());
            if (fetchedCarte != null) {
                System.out.println("✅ Carte récupérée : " + fetchedCarte.getNumeroCarte() + " (" + fetchedCarte.getTypeCarte() + ")");
                
                fetchedCarte.setTypeCarte("MASTERCARD");
                if (carteDAO.update(fetchedCarte)) {
                    System.out.println("✅ Carte mise à jour (Nouveau type: MASTERCARD)");
                }
            }
            
            List<CarteBancaire> listCartes = carteDAO.findByClient(4);
            System.out.println("✅ Nombre de cartes pour le client 1 : " + listCartes.size());
            

        } else {
            System.err.println("❌ Échec de l'insertion de la carte bancaire. (Vérifiez si le Client avec ID=1 existe)");
        }

        // Fermer la connexion
        System.out.println();
        ConnexionBDD.closeConnection();
        System.out.println("=== FIN DES TESTS ===");
    }
}
