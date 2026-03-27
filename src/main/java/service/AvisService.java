package service;

import dao.AvisDAO;
import model.Avis;
import shared.Reponse;
import shared.Requete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvisService {

    private final AvisDAO avisDAO;

    public AvisService() {
        this.avisDAO = new AvisDAO();
    }

    public Reponse getAvisByProduit(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idProduit = (Integer) params.get("idProduit");

        if (idProduit == null) {
            return new Reponse(false, "Paramètres manquants : idProduit", null);
        }

        try {
            List<Avis> avisList = avisDAO.getAvisByProduitId(idProduit);
            Map<String, Object> donnees = new HashMap<>();
            donnees.put("avis", avisList);
            return new Reponse(true, "Avis récupérés avec succès.", donnees);
        } catch (Exception e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la récupération des avis.", null);
        }
    }

    public Reponse addAvis(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        Integer idProduit = (Integer) params.get("idProduit");
        String contenu = (String) params.get("contenu");
        Number evaluationNum = (Number) params.get("evaluation");
        Integer evaluation = evaluationNum != null ? evaluationNum.intValue() : null;

        if (idClient == null || idProduit == null || contenu == null || contenu.trim().isEmpty() || evaluation == null) {
            return new Reponse(false, "Paramètres manquants ou invalides.", null);
        }

        if (evaluation < 1 || evaluation > 5) {
            return new Reponse(false, "L'évaluation doit être entre 1 et 5.", null);
        }

        Avis avis = new Avis();
        avis.setIdClient(idClient);
        avis.setIdProduit(idProduit);
        avis.setContenu(contenu);
        avis.setEvaluation(evaluation);

        try {
            boolean success = avisDAO.addAvis(avis);
            if (success) {
                return new Reponse(true, "Avis ajouté avec succès.", null);
            } else {
                return new Reponse(false, "Échec de l'ajout de l'avis.", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur interne lors de l'ajout de l'avis.", null);
        }
    }

    public Reponse getUserAvisForProduct(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        Integer idProduit = (Integer) params.get("idProduit");

        if (idClient == null || idProduit == null) {
            return new Reponse(false, "Paramètres manquants : idClient ou idProduit", null);
        }

        try {
            List<Avis> avisList = avisDAO.getAvisByClientAndProduit(idClient, idProduit);
            Map<String, Object> donnees = new HashMap<>();
            donnees.put("avis", avisList);
            return new Reponse(true, "Avis de l'utilisateur récupérés avec succès.", donnees);
        } catch (Exception e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la récupération des avis de l'utilisateur.", null);
        }
    }

    public Reponse updateAvis(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        Integer idProduit = (Integer) params.get("idProduit");
        String contenu = (String) params.get("contenu");
        Number evaluationNum = (Number) params.get("evaluation");
        Integer evaluation = evaluationNum != null ? evaluationNum.intValue() : null;

        if (idClient == null || idProduit == null || contenu == null || contenu.trim().isEmpty() || evaluation == null) {
            return new Reponse(false, "Paramètres manquants ou invalides.", null);
        }

        if (evaluation < 1 || evaluation > 5) {
            return new Reponse(false, "L'évaluation doit être entre 1 et 5.", null);
        }

        try {
            Avis avis = new Avis();
            avis.setIdClient(idClient);
            avis.setIdProduit(idProduit);
            avis.setContenu(contenu);
            avis.setEvaluation(evaluation);

            boolean success = avisDAO.updateAvis(avis);
            if (success) {
                return new Reponse(true, "Avis mis à jour avec succès.", null);
            } else {
                return new Reponse(false, "Échec de la mise à jour de l'avis.", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur interne lors de la mise à jour de l'avis.", null);
        }
    }
}
