package ui;

import client.ClientSocket;
import model.Commande;
//import model.Produit;
import model.Client;
import model.Notification;
import model.enums.StatutCommande;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class AdminController {
    private static AdminController instance;
    
    public static void refreshBadge() {
        if (instance != null) {
            instance.loadUnreadCount();
        }
    }
    
    // Stockage temporaire des données brutes pour les cellValueFactory
    private List<Map<String, Object>> rawCommandesData = new ArrayList<>();

    //@FXML private TableView<Produit> tableProduits;
    @FXML private TableView<Commande> tableCommandes;
    @FXML private TableView<Client> tableClients;
    
    @FXML private TextField searchCommandeField;
    @FXML private TextField searchClientField;
    @FXML private javafx.scene.layout.StackPane notifBadge;
    @FXML private Label notifCount;

    // Colonnes pour les commandes
    @FXML private TableColumn<Commande, String> colId;
    @FXML private TableColumn<Commande, String> colClient;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, String> colAdresse;
    @FXML private TableColumn<Commande, String> colTotal;
    @FXML private TableColumn<Commande, String> colDateLivraisonPrevue;
    @FXML private TableColumn<Commande, String> colDateLivraisonReelle;
    @FXML private TableColumn<Commande, Void> colActionsCommande;
    
    //@FXML private TableColumn<Produit, Void> colActionsProduit;
    @FXML private TableColumn<Client, String> colStatutClient;
    @FXML private TableColumn<Client, Void> colActionsClient;

    @FXML
    private void ouvrirNotifications() {
        SceneManager.clearCache("notifications.fxml");
        SceneManager.switchTo("notifications.fxml", "ChriOnline - Notifications");
    }

    @FXML
    public void initialize() {
        instance = this;
        // Configurer les colonnes
        setupColumns();
        
        // Charger les données depuis le backend via TCP
        loadCommandes("");
        loadClients("");
        loadUnreadCount();
        
        // Configurer recherche
        if (searchCommandeField != null) {
            searchCommandeField.textProperty().addListener((observable, oldValue, newValue) -> {
                loadCommandes(newValue);
            });
        }
        if (searchClientField != null) {
            searchClientField.textProperty().addListener((observable, oldValue, newValue) -> {
                loadClients(newValue);
            });
        }
        
        // Configurer les colonnes interactives
        //setupProduitActions();
        setupCommandeActions();
        setupClientActions();
    }

    private void loadUnreadCount() {
        if (!SessionManager.getInstance().isAuthenticated()) return;
        
        int userId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        String token = SessionManager.getInstance().getSession().getToken();

        javafx.concurrent.Task<Reponse> task = new javafx.concurrent.Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new java.util.HashMap<>();
                params.put("idUtilisateur", userId);
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.GET_NOTIFICATIONS, params, token));
            }
        };

        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                @SuppressWarnings("unchecked")
                List<Notification> notifs = (List<Notification>) rep.getDonnees().get("notifications");
                if (notifs != null) {
                    long unread = notifs.stream().filter(n -> n.getStatut() == Notification.StatutNotification.NON_LU).count();
                    javafx.application.Platform.runLater(() -> {
                        if (notifCount != null) notifCount.setText(String.valueOf(unread));
                        if (notifBadge != null) notifBadge.setVisible(unread > 0);
                    });
                }
            }
        });
        new Thread(task).start();
    }

    private void setupColumns() {
 
        // -- Colonne "#ID / Référence" ---------------------------------------
        colId.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                c.getReference() != null ? c.getReference() : "—"
            );
        });

        // -- Colonne "Client" ------------------------------------------------
        colClient.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                c.getIdClient() > 0 ? String.valueOf(c.getIdClient()) : "—"
            );
        });

        // -- Colonne "Adresse de livraison" ----------------------------------
        // La valeur n'est pas dans Commande.java → on passe par userData de la ligne
        colAdresse.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            if (rawCommandesData == null) return new javafx.beans.property.SimpleStringProperty("—");
            for (Map<String, Object> m : rawCommandesData) {
                if (m.get("rawId") instanceof Number &&
                        ((Number) m.get("rawId")).intValue() == c.getIdCommande()) {
                    Object adr = m.get("adresseLivraison");
                    return new javafx.beans.property.SimpleStringProperty(
                            adr != null ? adr.toString() : "—");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
 
        // -- Colonne "Total" -------------------------------------------------
        colTotal.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            if (rawCommandesData == null) return new javafx.beans.property.SimpleStringProperty("—");
            for (Map<String, Object> m : rawCommandesData) {
                if (m.get("rawId") instanceof Number &&
                        ((Number) m.get("rawId")).intValue() == c.getIdCommande()) {
                    Object total = m.get("total");
                    return new javafx.beans.property.SimpleStringProperty(
                            total != null ? total.toString() : "—");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
 
        // -- Colonne "Date création" formatée --------------------------------
        colDate.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            if (rawCommandesData == null) return new javafx.beans.property.SimpleStringProperty("—");
            for (Map<String, Object> m : rawCommandesData) {
                if (m.get("rawId") instanceof Number &&
                        ((Number) m.get("rawId")).intValue() == c.getIdCommande()) {
                    Object date = m.get("date");
                    return new javafx.beans.property.SimpleStringProperty(
                            date != null ? date.toString() : "—");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
 
        // -- Colonne "Date livraison prévue" ---------------------------------
        colDateLivraisonPrevue.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            if (rawCommandesData == null) return new javafx.beans.property.SimpleStringProperty("—");
            for (Map<String, Object> m : rawCommandesData) {
                if (m.get("rawId") instanceof Number &&
                        ((Number) m.get("rawId")).intValue() == c.getIdCommande()) {
                    Object d = m.get("dateLivraisonPrevue");
                    return new javafx.beans.property.SimpleStringProperty(
                            d != null ? d.toString() : "—");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
 
        // -- Colonne "Date livraison réelle" ---------------------------------
        colDateLivraisonReelle.setCellValueFactory(cellData -> {
            Commande c = cellData.getValue();
            if (rawCommandesData == null) return new javafx.beans.property.SimpleStringProperty("—");
            for (Map<String, Object> m : rawCommandesData) {
                if (m.get("rawId") instanceof Number &&
                        ((Number) m.get("rawId")).intValue() == c.getIdCommande()) {
                    Object d = m.get("dateLivraisonReelle");
                    return new javafx.beans.property.SimpleStringProperty(
                            d != null ? d.toString() : "—");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
 
        // -- Colonnes Client ------------------------------------------------
        if (colStatutClient != null) {
            colStatutClient.setCellValueFactory(cellData -> {
                Client c = cellData.getValue();
                String status = c.getStatut() != null ? c.getStatut() : "CLIENT";
                return new javafx.beans.property.SimpleStringProperty(status);
            });
        }
    }

    /**
     * Charge les commandes depuis le serveur selon le texte de recherche
     */
    private void loadCommandes(String searchText) {
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
                
                Requete requete;
                if (searchText == null || searchText.trim().isEmpty()) {
                    requete = new Requete(RequestType.ADMIN_GET_ALL_ORDERS, null, adminToken);
                } else {
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    params.put("query", searchText.trim());
                    // Use ADMIN_GET_ALL_ORDERS to avoid class version mismatch on the server
                    requete = new Requete(RequestType.ADMIN_GET_ALL_ORDERS, params, adminToken);
                }
                
                shared.Reponse reponse = ClientSocket.getInstance().envoyer(requete);
 
                if (reponse.isSucces() && reponse.getDonnees() != null) {
 
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) reponse.getDonnees();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> commandesData =
                            (List<Map<String, Object>>) dataMap.get("commandes");
 
                    // Garder les maps brutes pour les cellValueFactory
                    rawCommandesData = commandesData;
 
                    ObservableList<Commande> commandesList = FXCollections.observableArrayList();
                    for (Map<String, Object> map : commandesData) {
 
                        Commande cmd = new Commande();
 
                        // ── IDs ─────────────────────────────────────────
                        cmd.setIdCommande(((Number) map.get("rawId")).intValue());
                        cmd.setIdClient(((Number) map.get("idClient")).intValue());  // ← AJOUT
                        cmd.setReference((String) map.get("id"));
 
                        // ── idAdresse (peut être null) ───────────────────
                        if (map.get("idAdresse") != null) {
                            cmd.setIdAdresse(((Number) map.get("idAdresse")).intValue());
                        }
 
                        // ── Statut ───────────────────────────────────────
                        String statutStr = (String) map.get("statut");
                        StatutCommande statut = StatutCommande.VALIDEE;
                        try {
                            if (statutStr != null) statut = StatutCommande.valueOf(statutStr);
                        } catch (Exception e) {}
                        cmd.setStatut(statut);
 
                        commandesList.add(cmd);
                    }
 
                    javafx.application.Platform.runLater(() -> {
                        tableCommandes.setItems(commandesList);
                        System.out.println("[AdminController] " + commandesList.size() + " commandes chargées.");
                    });
 
                } else {
                    System.err.println("Erreur chargement commandes: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() ->
                            tableCommandes.setItems(FXCollections.observableArrayList()));
                }
            } catch (Exception e) {
                System.err.println("Exception chargement commandes: " + e.getMessage());
                javafx.application.Platform.runLater(() ->
                        tableCommandes.setItems(FXCollections.observableArrayList()));
            }
        }).start();
    }

    private void loadClients(String query) {
        if (!SessionManager.getInstance().isAuthenticated()) return;
        
        String token = SessionManager.getInstance().getSession().getToken();
        
        javafx.concurrent.Task<Reponse> task = new javafx.concurrent.Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new HashMap<>();
                if (query != null && !query.isEmpty()) {
                    params.put("query", query);
                }
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.ADMIN_GET_ALL_USERS, params, token));
            }
        };

        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                @SuppressWarnings("unchecked")
                List<Client> clients = (List<Client>) rep.getDonnees().get("clients");
                javafx.application.Platform.runLater(() -> {
                    tableClients.setItems(FXCollections.observableArrayList(clients));
                });
            } else {
                System.err.println("[AdminController] Échec chargement clients: " + (rep != null ? rep.getMessage() : "Inconnu"));
            }
        });
        new Thread(task).start();
    }
/* 
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

    // ═══════════════════════════════════════════════════════════════════════
    // BADGE COLORÉ POUR LA COLONNE STATUT
    // ═══════════════════════════════════════════════════════════════════════

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
                                            
                                            // Mettre à jour le dictionnaire pour rafraîchir l'affichage instantanément
                                            if (rawCommandesData != null) {
                                                for (java.util.Map<String, Object> m : rawCommandesData) {
                                                    if (m.get("rawId") instanceof Number && ((Number) m.get("rawId")).intValue() == item.getIdCommande()) {
                                                        m.put("statut", newStatut.name());
                                                        if (newStatut == StatutCommande.LIVREE) {
                                                            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                                                            m.put("dateLivraisonReelle", java.time.LocalDateTime.now().format(dtf));
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                            
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
    private void setupClientActions() {
        colActionsClient.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Client, Void> call(final TableColumn<Client, Void> param) {
                return new TableCell<>() {
                    private final Button btnToggle = new Button("Bannir");

                    {
                        btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                        btnToggle.setOnAction(event -> {
                            Client item = getTableView().getItems().get(getIndex());
                            boolean isBanned = "BANNI".equals(item.getStatut());
                            
                            try {
                                RequestType type = isBanned ? RequestType.ADMIN_UNBAN_USER : RequestType.ADMIN_BAN_USER;
                                java.util.Map<String, Object> params = new java.util.HashMap<>();
                                params.put("userId", item.getId());
                                
                                String adminToken = SessionManager.getInstance().getSession().getToken();
                                Requete req = new Requete(type, params, adminToken);
                                shared.Reponse rep = ClientSocket.getInstance().envoyer(req);
                                
                                if (rep.isSucces()) {
                                    System.out.println("Client " + item.getId() + (isBanned ? " débanni." : " banni."));
                                    // Mettre à jour l'objet localement pour rafraîchir la vue
                                    item.setStatut(isBanned ? "ACTIF" : "BANNI");
                                    item.setDeletedAt(isBanned ? null : java.time.LocalDateTime.now());
                                    tableClients.refresh();
                                } else {
                                    System.err.println("Erreur action utilisateur: " + rep.getMessage());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Client user = getTableView().getItems().get(getIndex());
                            if ("BANNI".equals(user.getStatut())) {
                                btnToggle.setText("Débannir");
                                btnToggle.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 10;");
                            } else {
                                btnToggle.setText("Bannir");
                                btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                            }
                            setGraphic(btnToggle);
                        }
                    }
                };
            }
        });
    }

/*
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
            default: return statut.name();
        }
    }
}
