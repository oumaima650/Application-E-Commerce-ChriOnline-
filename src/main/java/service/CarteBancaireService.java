package service;

import dao.CarteBancaireDAO;
import model.CarteBancaire;
import shared.Reponse;
import shared.Requete;

import java.util.List;

public class CarteBancaireService {

    private final CarteBancaireDAO carteBancaireDAO;

    public CarteBancaireService() {
        this.carteBancaireDAO = new CarteBancaireDAO();
    }

    public Reponse addCard(Requete requete) {
        Integer idClient = (Integer) requete.getParametres().get("idClient");
        String numeroCarte = (String) requete.getParametres().get("numeroCarte");
        String typeCarte = (String) requete.getParametres().get("typeCarte");
        
        if (idClient == null || numeroCarte == null || typeCarte == null) {
            return new Reponse(false, "Données de la carte manquantes.", null);
        }

        CarteBancaire carte = new CarteBancaire();
        carte.setIdClient(idClient);
        carte.setNumeroCarte(numeroCarte);
        carte.setTypeCarte(typeCarte);

        boolean success = carteBancaireDAO.create(carte);
        if (success) {
            return new Reponse(true, "Carte bancaire ajoutée avec succès.", java.util.Map.of("carte", carte));
        } else {
            return new Reponse(false, "Échec lors de l'ajout de la carte bancaire.", null);
        }
    }

    public Reponse getCards(Requete requete) {
        Integer idClient = (Integer) requete.getParametres().get("idClient");
        if (idClient == null) {
            return new Reponse(false, "ID Client manquant.", null);
        }

        List<CarteBancaire> cartes = carteBancaireDAO.findByClient(idClient);
        return new Reponse(true, "Cartes récupérées avec succès.", java.util.Map.of("cartes", cartes));
    }
    
    public Reponse removeCard(Requete requete) {
        Integer idCarte = (Integer) requete.getParametres().get("idCarte");
        if (idCarte == null) {
            return new Reponse(false, "ID Carte manquant.", null);
        }

        boolean success = carteBancaireDAO.delete(idCarte);
        if (success) {
            return new Reponse(true, "Carte bancaire supprimée avec succès.", null);
        } else {
            return new Reponse(false, "Échec lors de la suppression de la carte bancaire.", null);
        }
    }
}
