package ui;

import client.ClientSocket;
import client.models.MockDataService.CommandeMock;
import client.models.MockDataService.ProduitMock;
import client.models.MockDataService.UserMock;
import client.models.MockDataService;
import client.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Callback;
import ui.utils.IconLibrary;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

public class AdminController {

    @FXML private TableView<ProduitMock> tableProduits;
    @FXML private TableView<CommandeMock> tableCommandes;
    @FXML private TableView<UserMock> tableUtilisateurs;

    @FXML private TableColumn<ProduitMock, Void> colActionsProduit;
    @FXML private TableColumn<CommandeMock, Void> colActionsCommande;
    @FXML private TableColumn<UserMock, Void> colActionsUtilisateur;

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
        setupProduitActions();
        setupCommandeActions();
        setupUtilisateurActions();
    }

    /**
     * Charge les vraies données depuis le serveur via ClientSocket
     */
    private void loadRealData() {
        // Charger les produits
        new Thread(() -> {
            try {
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_PRODUCTS, null, "ADMIN_TOKEN");
                Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSuccess() && reponse.getData() != null) {
                    List<?> produits = (List<?>) reponse.getData();
                    // Convertir en ProduitMock pour compatibilité UI
                    ObservableList<ProduitMock> produitMocks = FXCollections.observableArrayList();
                    // TODO: Adapter la conversion selon votre modèle Produit
                    // pour l'instant, garder les mock data pour compatibilité
                    javafx.application.Platform.runLater(() -> {
                        tableProduits.setItems(FXCollections.observableArrayList(MockDataService.getMockProducts()));
                    });
                } else {
                    System.err.println("Erreur chargement produits: " + reponse.getMessage());
                    // Fallback sur mock data
                    javafx.application.Platform.runLater(() -> {
                        tableProduits.setItems(FXCollections.observableArrayList(MockDataService.getMockProducts()));
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des produits: " + e.getMessage());
                // Fallback sur mock data
                javafx.application.Platform.runLater(() -> {
                    tableProduits.setItems(FXCollections.observableArrayList(MockDataService.getMockProducts()));
                });
            }
        }).start();

        // Charger les commandes
        new Thread(() -> {
            try {
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_ORDERS, null, "ADMIN_TOKEN");
                Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSuccess() && reponse.getData() != null) {
                    javafx.application.Platform.runLater(() -> {
                        // TODO: Adapter la conversion selon votre modèle Commande
                        tableCommandes.setItems(FXCollections.observableArrayList(MockDataService.getMockOrders()));
                    });
                } else {
                    System.err.println("Erreur chargement commandes: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        tableCommandes.setItems(FXCollections.observableArrayList(MockDataService.getMockOrders()));
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des commandes: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    tableCommandes.setItems(FXCollections.observableArrayList(MockDataService.getMockOrders()));
                });
            }
        }).start();

        // Charger les utilisateurs
        new Thread(() -> {
            try {
                Requete requete = new Requete(RequestType.ADMIN_GET_ALL_USERS, null, "ADMIN_TOKEN");
                Reponse reponse = ClientSocket.getInstance().envoyer(requete);
                if (reponse.isSuccess() && reponse.getData() != null) {
                    javafx.application.Platform.runLater(() -> {
                        // TODO: Adapter la conversion selon votre modèle Utilisateur
                        tableUtilisateurs.setItems(FXCollections.observableArrayList(MockDataService.getMockUsers()));
                    });
                } else {
                    System.err.println("Erreur chargement utilisateurs: " + reponse.getMessage());
                    javafx.application.Platform.runLater(() -> {
                        tableUtilisateurs.setItems(FXCollections.observableArrayList(MockDataService.getMockUsers()));
                    });
                }
            } catch (Exception e) {
                System.err.println("Exception lors du chargement des utilisateurs: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    tableUtilisateurs.setItems(FXCollections.observableArrayList(MockDataService.getMockUsers()));
                });
            }
        }).start();
    }

    private void setupProduitActions() {
        colActionsProduit.setCellFactory(new Callback<>() {
            @Override
            public TableCell<ProduitMock, Void> call(final TableColumn<ProduitMock, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit = createIconButton(IconLibrary.SETTINGS, 16, "#24316B");
                    private final Button btnDelete = createIconButton(IconLibrary.TRASH, 16, "#E74C3C");
                    private final HBox pane = new HBox(10, btnEdit, btnDelete);

                    {
                        btnEdit.setStyle("-fx-background-color: #F8FFA1; -fx-background-radius: 5px; -fx-cursor: hand; -fx-padding: 5px;");
                        btnDelete.setStyle("-fx-background-color: #F6D5EE; -fx-background-radius: 5px; -fx-cursor: hand; -fx-padding: 5px;");
                        
                        btnEdit.setOnAction(event -> {
                            ProduitMock item = getTableView().getItems().get(getIndex());
                            openEditModal(item);
                        });

                        btnDelete.setOnAction(event -> {
                            ProduitMock item = getTableView().getItems().get(getIndex());
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

    private void setupCommandeActions() {
        colActionsCommande.setCellFactory(new Callback<>() {
            @Override
            public TableCell<CommandeMock, Void> call(final TableColumn<CommandeMock, Void> param) {
                return new TableCell<>() {
                    private final ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("En attente", "Validée", "Expédiée", "Livrée"));
                    
                    {
                        statusCombo.setStyle("-fx-background-radius: 10px; -fx-border-color: #ABA3F6; -fx-border-radius: 10px;");
                        statusCombo.setOnAction(event -> {
                            CommandeMock item = getTableView().getItems().get(getIndex());
                            if(item != null) {
                                String oldVal = item.getStatut();
                                String newVal = statusCombo.getValue();
                                if(!oldVal.equals(newVal)) {
                                    item.setStatut(newVal);
                                    System.out.println("Statut de la commande " + item.getId() + " passé à : " + newVal);
                                    tableCommandes.refresh();
                                    
                                    // Simulation: Si "Expédiée" -> Envoyer notification UDP via le service
                                    if(newVal.equals("Expédiée")) {
                                        System.out.println("-> Admin: Envoi notification UDP pour commande " + item.getId());
                                        
                                        // Envoyer via le système de notification UDP
                                        try {
                                            // Créer une requête pour déclencher la notification UDP
                                            java.util.Map<String, Object> params = new java.util.HashMap<>();
                                            params.put("orderId", item.getId());
                                            params.put("status", newVal);
                                            
                                            shared.Requete requete = new shared.Requete(shared.RequestType.ADMIN_UPDATE_ORDER_STATUS, params, "ADMIN_TOKEN");
                                            shared.Reponse reponse = client.ClientSocket.getInstance().envoyer(requete);
                                            
                                            if (reponse.isSuccess()) {
                                                System.out.println("-> Notification UDP envoyée avec succès");
                                            } else {
                                                System.err.println("-> Erreur envoi notification: " + reponse.getMessage());
                                            }
                                        } catch(Exception e) { 
                                            e.printStackTrace(); 
                                        }
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
                            CommandeMock mock = getTableView().getItems().get(getIndex());
                            statusCombo.setValue(mock.getStatut());
                            setGraphic(statusCombo);
                        }
                    }
                };
            }
        });
    }

    private void setupUtilisateurActions() {
        colActionsUtilisateur.setCellFactory(new Callback<>() {
            @Override
            public TableCell<UserMock, Void> call(final TableColumn<UserMock, Void> param) {
                return new TableCell<>() {
                    private final Button btnToggle = new Button("Bannir");

                    {
                        btnToggle.setStyle("-fx-background-color: #24316B; -fx-text-fill: #F8FFA1; -fx-background-radius: 10; -fx-cursor: hand;");
                        btnToggle.setOnAction(event -> {
                            UserMock item = getTableView().getItems().get(getIndex());
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

    private void openEditModal(ProduitMock item) {
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
    
    /**
     * Crée un bouton avec icône SVG
     */
    private Button createIconButton(String iconConstant, double size, String color) {
        SVGPath icon = IconLibrary.getIcon(iconConstant, size, color);
        Button button = new Button();
        button.setGraphic(icon);
        return button;
    }
}

