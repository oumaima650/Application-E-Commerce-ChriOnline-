package service;

import dao.ClientDAO;
import dao.AdresseDAO;
import model.Adresse;
import shared.Requete;
import shared.Reponse;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientService {

    private final ClientDAO clientDAO = new ClientDAO();
    private final AdresseDAO adresseDAO = new AdresseDAO();

    /**
     * Retourne le profil (nom, prénom, téléphone) du client connecté.
     * Paramètres attendus : "idClient" (Integer)
     */
    public Reponse getProfile(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        if (idClient == null) {
            return new Reponse(false, "Paramètre manquant : idClient.", null);
        }

        try {
            model.Client client = clientDAO.findById(idClient);
            if (client == null) {
                return new Reponse(false, "Client non trouvé.", null);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("nom", client.getNom());
            data.put("prenom", client.getPrenom());
            data.put("telephone", client.getTelephone());
            return new Reponse(true, "Profil récupéré.", data);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return new Reponse(false, "Erreur lors de la récupération du profil.", null);
        }
    }

    /**
     * Retourne les adresses du client.
     * Paramètres attendus : "idClient" (Integer)
     */
    public Reponse getAdresses(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        if (idClient == null) {
            return new Reponse(false, "Paramètre manquant : idClient.", null);
        }

        List<Adresse> adresses = adresseDAO.findByClient(idClient);
        List<Map<String, Object>> adressesList = new ArrayList<>();
        for (Adresse a : adresses) {
            Map<String, Object> map = new HashMap<>();
            map.put("idAdresse", a.getIdAdresse());
            map.put("addresseComplete", a.getAddresseComplete());
            map.put("ville", a.getVille());
            map.put("codePostal", a.getCodePostal());
            adressesList.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("adresses", adressesList);
        return new Reponse(true, adressesList.size() + " adresse(s) trouvée(s).", data);
    }

    /**
     * Ajoute une nouvelle adresse pour le client.
     * Paramètres attendus : "idClient" (Integer), "addresseComplete" (String), "ville" (String)
     */
    public Reponse addAdresse(Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String addresseComplete = (String) params.get("addresseComplete");
        String ville = (String) params.get("ville");
        String codePostal = (String) params.get("codePostal");

        if (idClient == null || addresseComplete == null || ville == null || codePostal == null) {
            return new Reponse(false, "Paramètres manquants : idClient, addresseComplete, ville, codePostal.", null);
        }

        Adresse adresse = new Adresse();
        adresse.setIdClient(idClient);
        adresse.setAddresseComplete(addresseComplete);
        adresse.setVille(ville);
        adresse.setCodePostal(codePostal);

        boolean success = adresseDAO.create(adresse);
        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("idAdresse", adresse.getIdAdresse());
            return new Reponse(true, "Adresse ajoutée avec succès.", data);
        }
        return new Reponse(false, "Erreur lors de l'ajout de l'adresse.", null);
    }
}
