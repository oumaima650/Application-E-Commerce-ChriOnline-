package ui;

import client.models.MockDataService.CommandeMock;
import client.models.MockDataService.ProduitMock;
import client.models.MockDataService.UserMock;
import client.models.MockDataService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class AdminController {

    @FXML private TableView<ProduitMock> tableProduits;
    @FXML private TableView<CommandeMock> tableCommandes;
    @FXML private TableView<UserMock> tableUtilisateurs;

    @FXML private TableColumn<ProduitMock, Void> colActionsProduit;
    @FXML private TableColumn<CommandeMock, Void> colActionsCommande;
    @FXML private TableColumn<UserMock, Void> colActionsUtilisateur;

    @FXML
    public void initialize() {
        // Charger les données de Mock
        ObservableList<ProduitMock> produits = FXCollections.observableArrayList(MockDataService.getMockProducts());
        tableProduits.setItems(produits);

        ObservableList<CommandeMock> commandes = FXCollections.observableArrayList(MockDataService.getMockOrders());
        tableCommandes.setItems(commandes);

        ObservableList<UserMock> utilisateurs = FXCollections.observableArrayList(MockDataService.getMockUsers());
        tableUtilisateurs.setItems(utilisateurs);

        // Configurer les colonnes interactives
        setupProduitActions();
        setupCommandeActions();
        setupUtilisateurActions();
    }

    private void setupProduitActions() {
        colActionsProduit.setCellFactory(new Callback<>() {
            @Override
            public TableCell<ProduitMock, Void> call(final TableColumn<ProduitMock, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit = new Button("✏️");
                    private final Button btnDelete = new Button("🗑");
                    private final HBox pane = new HBox(10, btnEdit, btnDelete);

                    {
                        btnEdit.setStyle("-fx-background-color: #F8FFA1; -fx-background-radius: 5px; -fx-cursor: hand;");
                        btnDelete.setStyle("-fx-background-color: #F6D5EE; -fx-background-radius: 5px; -fx-cursor: hand; -fx-text-fill: red;");
                        
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
                                    
                                    // Simulation: Si "Expédiée" -> Envoyer requête UDP au client
                                    if(newVal.equals("Expédiée")) {
                                        System.out.println("-> UDP Simulation: Envoi notif au client de la commande...");
                                        // UDP envoi local sur le port 9090 juste pour tester `ClientUDP`
                                        try {
                                           java.net.DatagramSocket ds = new java.net.DatagramSocket();
                                           String payload = "Votre ChriCommande " + item.getId() + " a été expédiée !";
                                           java.net.DatagramPacket dp = new java.net.DatagramPacket(payload.getBytes(), payload.length(), java.net.InetAddress.getLocalHost(), 9090);
                                           ds.send(dp);
                                           ds.close();
                                        } catch(Exception e) { e.printStackTrace(); }
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
}

