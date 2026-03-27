package service;

import dao.CommandeDAO;
import dao.PaiementDAO;
import dao.AdresseDAO;
import dao.ClientDAO;
import dao.SKUDAO;
import model.Commande;
import model.LigneCommande;
import model.Paiement;
import model.Adresse;
import model.Client;
import model.SKU;
import model.enums.StatutCommande;
import model.enums.MethodePaiement;
import model.enums.StatutPaiement;
import service.NotificationService;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CommandeService {
    private CommandeDAO commandeDAO;

    public CommandeService() {
        this.commandeDAO = new CommandeDAO();
    }


    public shared.Reponse getCommandesByClient(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            List<Commande> commandes = commandeDAO.findByClientId(idClient);
            List<Map<String, Object>> commandesData = new ArrayList<>();
            
            PaiementDAO paiementDAO = new PaiementDAO();
            AdresseDAO adresseDAO = new AdresseDAO();

            for (Commande commande : commandes) {
                Map<String, Object> commandeMap = enrichCommandeMap(commande, paiementDAO, adresseDAO);
                commandesData.add(commandeMap);
            }
            
            Map<String, Object> donnees = new java.util.HashMap<>();
            donnees.put("commandes", commandesData);
            donnees.put("total", commandesData.size());
            
            return new shared.Reponse(true, commandesData.size() + " commandes trouvées.", donnees);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la récupération des commandes: " + e.getMessage(), null);
        }
    }

    /**
     * Récupérer une commande par sa référence
     * Paramètres : "reference" (String)
     */
    public shared.Reponse getCommandeByReference(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        String reference = (String) params.get("reference");

        if (reference == null || reference.isBlank()) {
            return new shared.Reponse(false, "Référence manquante.", null);
        }

        try {
            Commande commande = commandeDAO.findByReference(reference);
            if (commande == null) {
                return new shared.Reponse(false, "Commande introuvable.", null);
            }

            // Charger les lignes
            commande.setLignes(commandeDAO.findLignesByCommandeId(commande.getIdCommande()));

            PaiementDAO paiementDAO = new PaiementDAO();
            AdresseDAO adresseDAO = new AdresseDAO();
            SKUDAO skuDAO = new SKUDAO();

            Map<String, Object> commandeMap = enrichCommandeMap(commande, paiementDAO, adresseDAO);

            // Ajouter les lignes détaillées avec images
            List<Map<String, Object>> lignesData = new ArrayList<>();
            for (model.LigneCommande ligne : commande.getLignes()) {
                Map<String, Object> ligneMap = new java.util.HashMap<>();
                ligneMap.put("nomProduit", ligne.getNomProduit());
                ligneMap.put("quantite", ligne.getQuantite());
                ligneMap.put("prixAchat", ligne.getPrixAchat());
                ligneMap.put("sousTotal", ligne.getPrixAchat() * ligne.getQuantite());
                
                SKU sku = skuDAO.getBySku(ligne.getSku());
                ligneMap.put("image", sku != null ? sku.getImage() : null);
                
                lignesData.add(ligneMap);
            }
            commandeMap.put("lignes", lignesData);

            Map<String, Object> finalData = new java.util.HashMap<>();
            finalData.put("commande", commandeMap);

            return new shared.Reponse(true, "Détails de la commande récupérés.", finalData);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la récupération de la commande : " + e.getMessage(), null);
        }
    }

    public shared.Reponse getCommandesFiltrees(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String statutFilter = (String) params.get("statut");
        String dateFilter = (String) params.get("date");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            List<Commande> commandes = commandeDAO.findWithFilters(idClient, statutFilter, dateFilter);
            List<Map<String, Object>> commandesData = new ArrayList<>();
            
            PaiementDAO paiementDAO = new PaiementDAO();
            AdresseDAO adresseDAO = new AdresseDAO();

            for (Commande commande : commandes) {
                commandesData.add(enrichCommandeMap(commande, paiementDAO, adresseDAO));
            }
            
            Map<String, Object> donnees = new java.util.HashMap<>();
            donnees.put("commandes", commandesData);
            donnees.put("total", commandesData.size());
            donnees.put("filtres", Map.of(
                "statut", statutFilter,
                "date", dateFilter
            ));
            
            return new shared.Reponse(true, commandesData.size() + " commandes trouvées avec filtres.", donnees);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la récupération des commandes filtrées: " + e.getMessage(), null);
        }
    }

    /**
     * Mettre à jour le statut d'une commande via une requête
     * Paramètres attendus : "idCommande" (Integer), "nouveauStatut" (String)
     */
    public shared.Reponse updateStatutCommande(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idCommande = (Integer) params.get("idCommande");
        String nouveauStatutStr = (String) params.get("nouveauStatut");

        if (idCommande == null || nouveauStatutStr == null) {
            return new shared.Reponse(false, "Paramètres manquants : idCommande ou nouveauStatut.", null);
        }

        try {
            StatutCommande nouveauStatut = StatutCommande.valueOf(nouveauStatutStr.toUpperCase());
            boolean success = commandeDAO.updateStatus(idCommande, nouveauStatut);
            
            if (success) {
                return new shared.Reponse(true, "Statut de la commande mis à jour avec succès.", null);
            } else {
                return new shared.Reponse(false, "Impossible de mettre à jour le statut de la commande.", null);
            }
        } catch (IllegalArgumentException e) {
            return new shared.Reponse(false, "Statut invalide: " + nouveauStatutStr, null);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la mise à jour du statut: " + e.getMessage(), null);
        }
    }

    /    /**
     * Passer une commande à partir du panier
     * Gère la création de nouvelles commandes ou la validation de commandes existantes (Draft)
     */
    public shared.Reponse passerCommande(shared.Requete requete) {
        System.out.println("[CommandeService] passerCommande appelé");
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        @SuppressWarnings("unchecked")
        List<String> selectedSkus = (List<String>) params.get("skus");
        String statutParam = (String) params.get("statut");
        String existingReference = (String) params.get("reference");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            PanierService panierService = new PanierService();
            List<model.LignePanier> lignesPanier;
            
            if (selectedSkus != null && !selectedSkus.isEmpty()) {
                lignesPanier = panierService.getLignesPanier(idClient, selectedSkus);
            } else {
                lignesPanier = panierService.getLignesPanier(idClient);
            }

            if (lignesPanier == null || lignesPanier.isEmpty()) {
                return new shared.Reponse(false, "Aucun article sélectionné ou panier vide.", null);
            }

            String reference;
            int idCommande;
            Commande commande;
            StatutCommande statutFinal = StatutCommande.VALIDEE;
            
            if (statutParam != null) {
                try { statutFinal = StatutCommande.valueOf(statutParam); } catch (Exception ignored) {}
            }

            if (existingReference != null && !existingReference.isEmpty()) {
                // --- REPRISE D'UNE COMMANDE EXISTANTE ---
                reference = existingReference;
                commande = commandeDAO.findByReference(reference);
                if (commande == null) return new shared.Reponse(false, "Commande introuvable.", null);
                
                idCommande = commande.getIdCommande();
                commande.setStatut(statutFinal);
                commandeDAO.updateStatus(idCommande, statutFinal);
            } else {
                // --- CRÉATION D'UNE NOUVELLE COMMANDE ---
                reference = "CHR-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + 
                                 String.format("%04d", (int)(Math.random() * 9999));
                
                commande = new Commande();
                commande.setIdClient(idClient);
                commande.setReference(reference);
                commande.setStatut(statutFinal);
                commande.setDateLivraisonPrevue(java.time.LocalDateTime.now().plusDays(5));
                
                Commande nouvelleCommande = commandeDAO.create(commande);
                idCommande = nouvelleCommande.getIdCommande();
                
                // Ajouter les lignes de commande
                for (model.LignePanier lp : lignesPanier) {
                    double prixAchat = lp.getSousTotal().doubleValue() / lp.getQuantite();
                    commandeDAO.addLigneCommande(idCommande, lp.getSku(), lp.getQuantite(), prixAchat);
                }
            }

            // Gestion des stocks si la commande est validée
            if (statutFinal == StatutCommande.VALIDEE) {
                SKUDAO skuDAO = new SKUDAO();
                List<model.LigneCommande> lignes = (List<model.LigneCommande>) commandeDAO.findLignesByCommandeId(idCommande);
                for (model.LigneCommande lc : lignes) {
                    skuDAO.reduireStock(lc.getSku(), lc.getQuantite());
                }
                
                // Vider le panier après validation
                if (selectedSkus != null && !selectedSkus.isEmpty()) {
                    panierService.supprimerArticles(idClient, selectedSkus);
                } else {
                    panierService.viderPanier(idClient);
                }
            }

            // Récupérer les données pour la réponse UI
            double totalFinal = commandeDAO.getMontantTotal(idCommande);
            List<Map<String, Object>> itemsSummary = new ArrayList<>();
            List<model.LigneCommande> toutesLesLignes = (List<model.LigneCommande>) commandeDAO.findLignesByCommandeId(idCommande);
            SKUDAO skuDAO = new SKUDAO();
            
            for (model.LigneCommande lc : toutesLesLignes) {
                Map<String, Object> item = new HashMap<>();
                item.put("nom", lc.getSku());
                item.put("quantite", lc.getQuantite());
                item.put("prixUnitaire", lc.getPrixAchat());
                try {
                    model.SKU s = skuDAO.getBySku(lc.getSku());
                    if (s != null) item.put("image", s.getImage());
                } catch (Exception ignored) {}
                itemsSummary.add(item);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("idCommande", idCommande);
            result.put("reference", reference);
            result.put("total", totalFinal);
            result.put("items", itemsSummary);
            result.put("dateLivraison", (commande.getDateLivraisonPrevue() != null ? 
                        commande.getDateLivraisonPrevue().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"));

            // --- NOTIFICATIONS ---
            NotificationService notifService = new NotificationService();
            if (statutFinal == StatutCommande.VALIDEE) {
                notifService.notifierAdmins("Nouvelle commande validée ! Réf: " + reference + " - Total: " + totalFinal + " MAD");
                notifService.creerNotification(idClient, "Votre commande " + reference + " a été validée avec succès !");
            } else if (statutFinal == StatutCommande.EN_ATTENTE) {
                notifService.creerNotification(idClient, "Votre commande " + reference + " est en brouillon.");
            }

            return new shared.Reponse(true, "Commande traitée avec succès.", result);

        } catch (Exception e) {
            e.printStackTrace();
            return new shared.Reponse(false, "Erreur : " + e.getMessage(), null);
        }
    }


    private Map<String, Object> enrichCommandeMap(Commande commande, PaiementDAO paiementDAO, AdresseDAO adresseDAO) throws SQLException {
        Map<String, Object> commandeMap = new java.util.HashMap<>();

        commandeMap.put("idCommande", commande.getIdCommande());
        commandeMap.put("reference", commande.getReference());
        commandeMap.put("statut", commande.getStatut().name());
        commandeMap.put("status_display", formatStatut(commande.getStatut()));
        
        if (commande.getCreatedAt() != null) {
            commandeMap.put("created_at", commande.getCreatedAt().toString());
            commandeMap.put("date", commande.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            commandeMap.put("created_at", null);
            commandeMap.put("date", "N/A");
        }
        
        if (commande.getDateLivraisonReelle() != null) {
            commandeMap.put("date_livraison_reelle", commande.getDateLivraisonReelle().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            commandeMap.put("date_livraison_reelle", "");
        }
        
        // Get Payment Method
        List<Paiement> paiements = paiementDAO.findByCommande(commande.getIdCommande());
        if (paiements != null && !paiements.isEmpty()) {
            commandeMap.put("methode_paiement", formatMethodePaiement(paiements.get(0).getMethodePaiement()));
        } else {
            commandeMap.put("methode_paiement", "N/A");
        }

        // Get Address
        if (commande.getIdAdresse() != null) {
            Adresse addr = adresseDAO.findById(commande.getIdAdresse());
            if (addr != null) {
                commandeMap.put("adresse_complete", addr.getAddresseComplete() + ", " + addr.getVille());
            } else {
                commandeMap.put("adresse_complete", "Adresse introuvable");
            }
        } else {
            commandeMap.put("adresse_complete", "Non spécifiée");
        }

        // --- NOUVEAU : Ajouter les infos du client (prenom, nom, telephone) ---
        ClientDAO clientDAO = new ClientDAO();
        Client client = clientDAO.findById(commande.getIdClient());
        if (client != null) {
            commandeMap.put("prenom", client.getPrenom());
            commandeMap.put("nom", client.getNom());
            commandeMap.put("telephone", client.getTelephone());
        }

        // Get Total
        double total = commandeDAO.getMontantTotal(commande.getIdCommande());
        commandeMap.put("total", total);
        commandeMap.put("total_formatted", String.format("%.2f MAD", total).replace(",", " "));
        
        return commandeMap;
    }

    private String formatMethodePaiement(model.enums.MethodePaiement methode) {
        if (methode == null) return "N/A";
        switch (methode) {
            case CARD: return "Carte Bancaire";
            case CASH: return "Cash à la livraison";
            default: return methode.name();
        }
    }

    /**
     * Formater le statut pour l'affichage
     */
    private String formatStatut(StatutCommande statut) {
        if (statut == null) return "N/A";
        switch (statut) {
            case EN_ATTENTE: return "En attente";
            case VALIDEE: return "Validée";
            case EXPEDIEE: return "Expédiée";
            case LIVREE: return "Livrée";
            default: return statut.name();
        }
    }
}

