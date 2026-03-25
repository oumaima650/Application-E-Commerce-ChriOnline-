package service;

import dao.CommandeDAO;
import model.Commande;
import model.enums.StatutCommande;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CommandeService {
    private CommandeDAO commandeDAO;

    public CommandeService() {
        this.commandeDAO = new CommandeDAO();
    }


    /**
     * Récupérer toutes les commandes d'un client via une requête
     * Paramètres attendus : "idClient" (Integer)
     */
    public shared.Reponse getCommandesByClient(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            List<Commande> commandes = commandeDAO.findByClientId(idClient);
            List<Map<String, Object>> commandesData = new ArrayList<>();
            
            for (Commande commande : commandes) {
                // Charger les lignes pour chaque commande
                commande.setLignes(commandeDAO.findLignesByCommandeId(commande.getIdCommande()));
                
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
                
                // Calculer le total et résumé des articles
                if (commande.getLignes() != null && !commande.getLignes().isEmpty()) {
                    double total = commande.getLignes().stream()
                        .mapToDouble(ligne -> ligne.getPrixAchat() * ligne.getQuantite())
                        .sum();
                    commandeMap.put("total", total);
                    commandeMap.put("total_formatted", String.format("%.2f MAD", total).replace(",", " "));
                    
                    String articlesSummary = commande.getLignes().stream()
                        .limit(3)
                        .map(ligne -> ligne.getNomProduit())
                        .collect(Collectors.joining(", "));
                    
                    if (commande.getLignes().size() > 3) {
                        articlesSummary += "...";
                    }
                    commandeMap.put("articles_summary", articlesSummary);
                } else {
                    commandeMap.put("total", 0.0);
                    commandeMap.put("total_formatted", "0.00 MAD");
                    commandeMap.put("articles_summary", "Aucun article");
                }
                
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
     * Récupérer les commandes avec filtres via une requête
     * Paramètres attendus : "idClient" (Integer), "statut" (String, optionnel), "date" (String, optionnel), "categorie" (String, optionnel)
     */
    public shared.Reponse getCommandesFiltrees(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        String statutFilter = (String) params.get("statut");
        String dateFilter = (String) params.get("date");
        String categorieFilter = (String) params.get("categorie");

        if (idClient == null) {
            return new shared.Reponse(false, "Paramètres manquants : idClient.", null);
        }

        try {
            List<Commande> commandes = commandeDAO.findWithFilters(idClient, statutFilter, dateFilter, categorieFilter);
            List<Map<String, Object>> commandesData = new ArrayList<>();
            
            for (Commande commande : commandes) {
                // Charger les lignes pour chaque commande
                commande.setLignes(commandeDAO.findLignesByCommandeId(commande.getIdCommande()));
                
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
                
                // Calculer le total et résumé des articles
                if (commande.getLignes() != null && !commande.getLignes().isEmpty()) {
                    double total = commande.getLignes().stream()
                        .mapToDouble(ligne -> ligne.getPrixAchat() * ligne.getQuantite())
                        .sum();
                    commandeMap.put("total", total);
                    commandeMap.put("total_formatted", String.format("%.2f MAD", total).replace(",", " "));
                    
                    String articlesSummary = commande.getLignes().stream()
                        .limit(3)
                        .map(ligne -> ligne.getNomProduit())
                        .collect(Collectors.joining(", "));
                    
                    if (commande.getLignes().size() > 3) {
                        articlesSummary += "...";
                    }
                    commandeMap.put("articles_summary", articlesSummary);
                } else {
                    commandeMap.put("total", 0.0);
                    commandeMap.put("total_formatted", "0.00 MAD");
                    commandeMap.put("articles_summary", "Aucun article");
                }
                
                commandesData.add(commandeMap);
            }
            
            Map<String, Object> donnees = new java.util.HashMap<>();
            donnees.put("commandes", commandesData);
            donnees.put("total", commandesData.size());
            donnees.put("filtres", Map.of(
                "statut", statutFilter,
                "date", dateFilter,
                "categorie", categorieFilter
            ));
            
            return new shared.Reponse(true, commandesData.size() + " commandes trouvées avec filtres.", donnees);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la récupération des commandes filtrées: " + e.getMessage(), null);
        }
    }

    /**
     * Récupérer une commande par sa référence via une requête
     * Paramètres attendus : "reference" (String)
     */
    public shared.Reponse getCommandeByReference(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        String reference = (String) params.get("reference");

        if (reference == null) {
            return new shared.Reponse(false, "Paramètres manquants : reference.", null);
        }

        try {
            Commande commande = commandeDAO.findByReference(reference);
            
            if (commande == null) {
                return new shared.Reponse(false, "Commande non trouvée.", null);
            }
            
            // Charger les lignes
            commande.setLignes(commandeDAO.findLignesByCommandeId(commande.getIdCommande()));

            
            Map<String, Object> commandeMap = new java.util.HashMap<>();
            commandeMap.put("idCommande", commande.getIdCommande());
            commandeMap.put("reference", commande.getReference());
            commandeMap.put("statut", commande.getStatut().name());
            commandeMap.put("status_display", formatStatut(commande.getStatut()));
            commandeMap.put("idClient", commande.getIdClient());
            
            if (commande.getCreatedAt() != null) {
                commandeMap.put("created_at", commande.getCreatedAt().toString());
                commandeMap.put("date", commande.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } else {
                commandeMap.put("created_at", null);
                commandeMap.put("date", "N/A");
            }
            
            // Détails des lignes de commande
            if (commande.getLignes() != null && !commande.getLignes().isEmpty()) {
                List<Map<String, Object>> lignesData = new ArrayList<>();
                double total = 0.0;
                
                for (model.LigneCommande ligne : commande.getLignes()) {
                    Map<String, Object> ligneMap = new java.util.HashMap<>();
                    ligneMap.put("nomProduit", ligne.getNomProduit());
                    ligneMap.put("quantite", ligne.getQuantite());
                    ligneMap.put("prixAchat", ligne.getPrixAchat());
                    ligneMap.put("sousTotal", ligne.getPrixAchat() * ligne.getQuantite());
                    lignesData.add(ligneMap);
                    total += ligne.getPrixAchat() * ligne.getQuantite();
                }
                
                commandeMap.put("lignes", lignesData);
                commandeMap.put("total", total);
                commandeMap.put("total_formatted", String.format("%.2f MAD", total).replace(",", " "));
            } else {
                commandeMap.put("lignes", new ArrayList<>());
                commandeMap.put("total", 0.0);
                commandeMap.put("total_formatted", "0.00 MAD");
            }
            
            return new shared.Reponse(true, "Commande trouvée.", commandeMap);
        } catch (SQLException e) {
            return new shared.Reponse(false, "Erreur lors de la récupération de la commande: " + e.getMessage(), null);
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

    /**
     * Passer une commande à partir du panier
     * Paramètres attendus : "idClient" (Integer), "skus" (List<String>, optionnel)
     */
    public shared.Reponse passerCommande(shared.Requete requete) {
        Map<String, Object> params = requete.getParametres();
        Integer idClient = (Integer) params.get("idClient");
        @SuppressWarnings("unchecked")
        List<String> selectedSkus = (List<String>) params.get("skus");

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

            // Créer l'en-tête de la commande
            String reference = "CHR-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + 
                             String.format("%04d", (int)(Math.random() * 9999));
            
            Commande commande = new Commande();
            commande.setIdClient(idClient);
            commande.setReference(reference);
            commande.setStatut(StatutCommande.EN_ATTENTE);
            commande.setDateLivraisonPrevue(java.time.LocalDateTime.now().plusDays(3));
            
            Commande nouvelleCommande = commandeDAO.create(commande);
            int idCommande = nouvelleCommande.getIdCommande();

            // Ajouter les lignes et calculer le montant total
            double total = 0;
            for (model.LignePanier lp : lignesPanier) {
                double prixAchat = lp.getSousTotal().doubleValue() / lp.getQuantite();
                commandeDAO.addLigneCommande(idCommande, lp.getSku(), lp.getQuantite(), prixAchat);
                total += lp.getSousTotal().doubleValue();
            }

            // Supprimer SEULEMENT les lignes commandées du panier
            if (selectedSkus != null && !selectedSkus.isEmpty()) {
                panierService.supprimerLignes(idClient, selectedSkus);
            } else {
                panierService.viderPanier(idClient);
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("idCommande", idCommande);
            result.put("reference", reference);
            result.put("total", total);

            return new shared.Reponse(true, "Commande " + reference + " créée avec succès !", result);
        } catch (Exception e) {
            return new shared.Reponse(false, "Erreur lors de la validation de la commande: " + e.getMessage(), null);
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

