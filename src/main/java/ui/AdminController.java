package ui;

import client.ClientSocket;
import model.Commande;
//import model.Produit;
//import model.Utilisateur;
import model.enums.StatutCommande;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.shape.SVGPath;
import javafx.util.Callback;
//import ui.utils.IconLibrary;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.List;

public class AdminController {

    //@FXML private TableView<Produit> tableProduits;
    @FXML private TableView<Commande> tableCommandes;
    //@FXML private TableView<Utilisateur> tableUtilisateurs;

    //@FXML private TableColumn<Produit, Void> colActionsProduit;
    @FXML private TableColumn<Commande, Void> colActionsCommande;
    //@FXML private TableColumn<Utilisateur, Void> colActionsUtilisateur;

    @FXML
    private void ouvrirNotifications() {
        SceneManager.switchToCached("notifications.fxml", "ChriOnline - Notifications");
    }

    @FXML
    public void initialize() {
        // Mettre en cache la vue notifications pour un accès rapide
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/chrionline/fxml/notifications.fxml"));
            javafx.scene.Parent notificationsRoot = loader.load();
            SceneManager.cacheScene("notifications.fxml", notificationsRoot);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement en cache de notifications.fxml: " + e.getMessage());
        }
        
        // Charger les données depuis le backend via TCP
        loadRealData();
        
        // Configurer les colonnes interactives
        //setupProduitActions();
        setupCommandeActions();
        //setupUtilisateurActions();
    }

    /**
     * Charge les vraies données depuis le serveur via ClientSocket
     */
    private void loadRealData() {
/*
        // Charger les produits
        new Thread(() -> {
            try {
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_PRODUCTS, null, "ADMIN_TOKEN");
                Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSuccess() && reponse.getData() != null) {
                    @SuppressWarnings("unchecked")
                    List<Produit> produits = (List<Produit>) reponse.getData();
                    ObservableList<Produit> produitsList = FXCollections.observableArrayList(produits);
                    javafx.application.Platform.runLater(() -> {
                        tableProduits.setItems(produitsList);
                        System.out.println("[AdminController] " + produitsList.size() + " produits chargés depuis la BDD");
                    });
                } else {
                    System.err.println("Erreur chargement produits: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        tableProduits.setItems(FXCollections.observableArrayList());
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des produits: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    tableProduits.setItems(FXCollections.observableArrayList());
                });
            }
        }).start();
*/
        // Charger les commandes
        new Thread(() -> {
            try {
                String adminToken = SessionManager.getInstance().getSession().getToken();
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_ORDERS, null, adminToken);
                shared.Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSucces() && reponse.getDonnees() != null) {
                    // Les commandes sont dans une Map avec clé "commandes"
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) reponse.getDonnees();
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> commandesData = (java.util.List<java.util.Map<String, Object>>) dataMap.get("commandes");

                    // Créer des commandes simples pour l'affichage
                    ObservableList<Commande> commandesList = FXCollections.observableArrayList();
                    for (java.util.Map<String, Object> map : commandesData) {
                        Commande cmd = new Commande();
                        cmd.setIdCommande(((Number) map.get("rawId")).intValue());
                        cmd.setReference((String) map.get("id"));
                        
                        // Parser le statut
                        String statutStr = (String) map.get("statut");
                        StatutCommande statut = StatutCommande.VALIDEE;
                        if ("Expédiée".equals(statutStr)) statut = StatutCommande.EXPEDIEE;
                        else if ("Livrée".equals(statutStr)) statut = StatutCommande.LIVREE;
                        else if ("Validée".equals(statutStr)) statut = StatutCommande.VALIDEE;
                        cmd.setStatut(statut);

                        commandesList.add(cmd);
                    }

                    javafx.application.Platform.runLater(() -> {
                        tableCommandes.setItems(commandesList);
                        System.out.println("[AdminController] " + commandesList.size() + " commandes chargées depuis la BDD");
                    });
                } else {
                    System.err.println("Erreur chargement commandes: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        tableCommandes.setItems(FXCollections.observableArrayList());
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des commandes: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    tableCommandes.setItems(FXCollections.observableArrayList());
                });
            }
        }).start();
    }
/*
        // Charger les utilisateurs
        new Thread(() -> {
            try {
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_USERS, null, "ADMIN_TOKEN");
                Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSuccess() && reponse.getData() != null) {
                    @SuppressWarnings("unchecked")
                    List<Utilisateur> utilisateurs = (List<Utilisateur>) reponse.getData();
                    ObservableList<Utilisateur> utilisateursList = FXCollections.observableArrayList(utilisateurs);
                    javafx.application.Platform.runLater(() -> {
                        tableUtilisateurs.setItems(utilisateursList);
                        System.out.println("[AdminController] " + utilisateursList.size() + " utilisateurs chargés depuis la BDD");
                    });
                } else {
                    System.err.println("Erreur chargement utilisateurs: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        tableUtilisateurs.setItems(FXCollections.observableArrayList());
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des utilisateurs: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    tableUtilisateurs.setItems(FXCollections.observableArrayList());
                });
            }
        }).start();
    }

    private void setupProduitActions() {
        colActionsProduit.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Produit, Void> call(final TableColumn<Produit, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit = createIconButton(IconLibrary.SETTINGS, 16, "#24316B");
                    private final Button btnDelete = createIconButton(IconLibrary.TRASH, 16, "#E74C3C");
                    private final HBox pane = new HBox(10, btnEdit, btnDelete);

                    {
                        btnEdit.setStyle("-fx-background-color: #F8FFA1; -fx-background-radius: 5px; -fx-cursor: hand; -fx-padding: 5px;");
                        btnDelete.setStyle("-fx-background-color: #F6D5EE; -fx-background-radius: 5px; -fx-cursor: hand; -fx-padding: 5px;");
                        
                        btnEdit.setOnAction(event -> {
                            Produit item = getTableView().getItems().get(getIndex());
                            openEditModal(item);
                        });

                        btnDelete.setOnAction(event -> {
                            Produit item = getTableView().getItems().get(getIndex());
                            tableProduits.getItems().remove(item);
                            System.out.println("Produit " + item.getId() + " supprimé.");
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }
*/
    private void setupCommandeActions() {
        colActionsCommande.setCellFactory(new Callback<TableColumn<Commande, Void>, TableCell<Commande, Void>>() {
            @Override
            public TableCell<Commande, Void> call(final TableColumn<Commande, Void> param) {
                return new TableCell<Commande, Void>() {
                    private final ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("Validée", "Expédiée", "Livrée"));
                    
                    {
                        statusCombo.setStyle("-fx-background-radius: 5px; -fx-border-color: #ABA3F6; -fx-border-radius: 5px;");
                        statusCombo.setOnAction(event -> {
                            Commande item = getTableView().getItems().get(getIndex());
                            if(item != null) {
                                String oldVal = formatStatut(item.getStatut());
                                String newVal = statusCombo.getValue();
                                if(newVal != null && !newVal.equals(oldVal)) {
                                    
                                    System.out.println("-> Admin: MAJ statut Commande DB ID " + item.getIdCommande() + " vers " + newVal);
                                    
                                    // Envoyer requête TCP pour MAJ
                                    try {
                                        java.util.Map<String, Object> params = new java.util.HashMap<>();
                                        params.put("orderId", item.getIdCommande());
                                        params.put("status", newVal);
                                        
                                        String adminToken = SessionManager.getInstance().getSession().getToken();
                                        Requete requete = new Requete(RequestType.ADMIN_UPDATE_ORDER_STATUS, params, adminToken);
                                        shared.Reponse reponse = client.ClientSocket.getInstance().envoyer(requete);
                                        
                                        if (reponse.isSucces()) {
                                            System.out.println("-> MAJ serveur réussie.");
                                            // Mettre à jour l'objet Commande
                                            StatutCommande newStatut = StatutCommande.VALIDEE;
                                            if ("Expédiée".equals(newVal)) newStatut = StatutCommande.EXPEDIEE;
                                            else if ("Livrée".equals(newVal)) newStatut = StatutCommande.LIVREE;
                                            item.setStatut(newStatut);
                                            tableCommandes.refresh();
                                        } else {
                                            System.err.println("-> Erreur Serveur: " + reponse.getMessage());
                                            javafx.application.Platform.runLater(() -> statusCombo.setValue(oldVal));
                                        }
                                    } catch(Exception e) { 
                                        e.printStackTrace(); 
                                        javafx.application.Platform.runLater(() -> statusCombo.setValue(oldVal));
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Commande commande = getTableView().getItems().get(getIndex());
                            // Temporarily remove listener to avoid triggering it
                            javafx.event.EventHandler<javafx.event.ActionEvent> handler = statusCombo.getOnAction();
                            statusCombo.setOnAction(null);
                            
                            String statutAffiche = formatStatut(commande.getStatut());
                            // Si le statut est "En attente" (normalement filtré), on l'ajoute provisoirement
                            if (!statusCombo.getItems().contains(statutAffiche)) {
                                statusCombo.getItems().add(statutAffiche);
                            }
                            
                            statusCombo.setValue(statutAffiche);
                            statusCombo.setOnAction(handler);
                            setGraphic(statusCombo);
                        }
                    }
                };
            }
        });
    }
/*
    private void setupUtilisateurActions() {
        colActionsUtilisateur.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Utilisateur, Void> call(final TableColumn<Utilisateur, Void> param) {
                return new TableCell<>() {
                    private final Button btnToggle = new Button("Bannir");

                    {
                        btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                        btnToggle.setOnAction(event -> {
                            Utilisateur item = getTableView().getItems().get(getIndex());
                            if(btnToggle.getText().equals("Bannir")) {
                                btnToggle.setText("Débannir");
                                btnToggle.setStyle("-fx-background-color: gray; -fx-text-fill: white; -fx-background-radius: 10;");
                                System.out.println("Utilisateur " + item.getId() + " banni.");
                            } else {
                                btnToggle.setText("Bannir");
                                btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                                System.out.println("Utilisateur " + item.getId() + " ré-autorisé.");
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btnToggle);
                    }
                };
            }
        });
    }
/*
    private void setupUtilisateurActions() {
        colActionsUtilisateur.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Utilisateur, Void> call(final TableColumn<Utilisateur, Void> param) {
                return new TableCell<>() {
                    private final Button btnToggle = new Button("Bannir");

                    {
                        btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                        btnToggle.setOnAction(event -> {
                            Utilisateur item = getTableView().getItems().get(getIndex());
                            if(btnToggle.getText().equals("Bannir")) {
                                btnToggle.setText("Débannir");
                                btnToggle.setStyle("-fx-background-color: gray; -fx-text-fill: white; -fx-background-radius: 10;");
                                System.out.println("Utilisateur " + item.getId() + " banni.");
                            } else {
                                btnToggle.setText("Bannir");
                                btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                                System.out.println("Utilisateur " + item.getId() + " ré-autorisé.");
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btnToggle);
                    }
                };
            }
        });
    }

    private void openEditModal(Produit item) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/chrionline/fxml/produit_form.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ProduitFormController controller = loader.getController();
            controller.setProduit(item, () -> {
                tableProduits.refresh();
                System.out.println("Vue Produit Rofraîchie : " + item.getNom());
            });
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifier Produit");
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/com/chrionline/css/styles.css").toExternalForm());
            } catch (Exception ignored) {}
            
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.err.println("Impossible de charger la modale produit_form.fxml");
        }
    }
 */   
    /**
     * Formate le statut pour l'affichage dans l'interface
     */
    private String formatStatut(StatutCommande statut) {
        if (statut == null) return "N/A";
        
        switch (statut) {
            case VALIDEE: return "Validée";
            case EXPEDIEE: return "Expédiée";
            case LIVREE: return "Livrée";
            case EN_ATTENTE: return "En attente";
            default: return statut.name();
        }
    }
}

