package service;

import dao.CommandeDAO;
import dao.PaiementDAO;
import model.Commande;
import model.Paiement;
import model.enums.MethodePaiement;
import model.enums.StatutPaiement;
import shared.Reponse;
import shared.Requete;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaiementService {

    private final PaiementDAO paiementDAO;
    private final NotificationService notificationService;

    public PaiementService() {
        this.paiementDAO = new PaiementDAO();
        this.notificationService = new NotificationService();
    }

    public Reponse processPayment(Requete requete) {
        Integer idCommande = (Integer) requete.getBody().get("idCommande");
        Integer idCarte = (Integer) requete.getBody().get("idCarte");
        String montantStr = (String) requete.getBody().get("montant");
        String methode = (String) requete.getBody().get("methodePaiement");

        if (idCommande == null || montantStr == null || methode == null) {
            return new Reponse(false, "Paramètres de paiement manquants.", null);
        }

        BigDecimal montant;
        try {
            montant = new BigDecimal(montantStr);
        } catch (NumberFormatException e) {
            return new Reponse(false, "Montant invalide.", null);
        }

        ModePaiement modePaiement;
        try {
            modePaiement = ModePaiement.valueOf(methode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return new Reponse(false, "Méthode de paiement invalide.", null);
        }

        if (modePaiement == ModePaiement.CARD && idCarte == null) {
            return new Reponse(false, "Une carte bancaire est requise pour le paiement par carte.", null);
        }

        Paiement paiement = new Paiement();
        paiement.setIdCommande(idCommande);
        paiement.setIdCarte(idCarte);
        paiement.setMontant(montant);
        paiement.setMethodePaiement(modePaiement);
        // Simulation d'un paiement qui réussit toujours:
        paiement.setStatutPaiement(StatutPaiement.APPROUVE);
        paiement.setDatePaiement(LocalDateTime.now());

        boolean success = paiementDAO.create(paiement);
        if (success) {
            // Dans un système réel, il faudrait d'abord appeler CommandeDAO pour récupérer le clientId
            // Commande commande = commandeDAO.findById(idCommande);
            // int clientId = commande.getIdClient();
            // notificationService.creerNotification(clientId, "Votre paiement de " + montantStr + " a été approuvé.");
            
            return new Reponse(true, "Paiement traité avec succès.", paiement);
        } else {
            return new Reponse(false, "Échec du traitement du paiement.", null);
        }
    }
}
