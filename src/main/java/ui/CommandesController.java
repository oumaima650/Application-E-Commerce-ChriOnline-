package ui;

import client.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import ui.utils.IconLibrary;
import client.utils.SessionManager;


import client.ClientSocket;
import shared.RequestType;
import shared.Requete;
import shared.Reponse;

import java.util.List;
import java.util.Map;

public class CommandesController {

    @FXML private TableView<OrderRow> ordersTable;
    @FXML private TableColumn<OrderRow, String> refColumn;
    @FXML private TableColumn<OrderRow, String> dateColumn;
    @FXML private TableColumn<OrderRow, String> summaryColumn;
    @FXML private TableColumn<OrderRow, String> totalColumn;
    @FXML private TableColumn<OrderRow, Void> statusColumn;
    @FXML private TableColumn<OrderRow, Void> actionsColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker dateFilter;

    private ObservableList<OrderRow> ordersData = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadCommandes();
    }

    private void setupTableColumns() {
        // Liaison des colonnes simples via les propriétés de OrderRow
        refColumn.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        summaryColumn.setCellValueFactory(cellData -> cellData.getValue().articlesSummaryProperty());
        totalColumn.setCellValueFactory(cellData -> cellData.getValue().totalProperty());
        
        setupStatusColumn();
        setupActionsColumn();
        ordersTable.setItems(ordersData);
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("Tous", "En attente", "Validée", "Expédiée", "Livrée", "Annulée");
        statusFilter.setValue("Tous");
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterOrders());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders());
    }

    private void loadCommandes() {
        try {
            // Envoyer la requête au serveur avec l'ID de session réel
            Requete req = new Requete(RequestType.GET_ORDERS, 
                Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()), 
                SessionManager.getInstance().getSession().getAccessToken());

            Reponse rep = ClientSocket.getInstance().envoyer(req);
            
            if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) rep.getDonnees().get("commandes");
                
                ordersData.clear();
                if (list != null) {
                    for (Map<String, Object> map : list) {
                        ordersData.add(new OrderRow(map));
                    }
                }
            } else {
                System.err.println("Erreur chargement commandes : " + (rep != null ? rep.getMessage() : "Pas de réponse"));
                loadTestData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadTestData();
        }
    }

    private void loadTestData() {
        ordersData.clear();
        ordersData.add(new OrderRow("#CHR-20260327-0042", "27/03/2026", "iPhone 15 Pro, AirPods Pro", "12 500 MAD", "Livrée"));
        ordersData.add(new OrderRow("#CHR-20260325-0038", "25/03/2026", "MacBook Air M2, Coque", "18 750 MAD", "Expédiée"));
    }

    private void filterOrders() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String dateValue = dateFilter.getValue() != null ? dateFilter.getValue().toString() : null;

        ObservableList<OrderRow> filteredData = FXCollections.observableArrayList();
        for (OrderRow row : ordersData) {
            boolean matchesSearch = searchText.isEmpty() || 
                                    row.getOrderId().toLowerCase().contains(searchText) ||
                                    row.getArticlesSummary().toLowerCase().contains(searchText);
            boolean matchesStatus = statusValue.equals("Tous") || row.getStatus().equalsIgnoreCase(statusValue);
            boolean matchesDate = dateValue == null || row.getDate().contains(dateValue);

            if (matchesSearch && matchesStatus && matchesDate) filteredData.add(row);
        }
        ordersTable.setItems(filteredData);
    }

    private void setupStatusColumn() {
        statusColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    OrderRow order = getTableView().getItems().get(getIndex());
                    setGraphic(createStatusBadge(order.getStatus()));
                }
            }
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = createIconButton(IconLibrary.ARROW_R, "Voir détail");
            private final HBox pane = new HBox(8, viewButton);
            {
                viewButton.setOnAction(event -> {
                    OrderRow order = getTableView().getItems().get(getIndex());
                    viewOrderDetails(order);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private VBox createStatusBadge(String status) {
        VBox badge = new VBox();
        badge.getStyleClass().add("status-badge");
        Label label = new Label(status);
        label.getStyleClass().add("status-text");
        
        String s = status != null ? status.toLowerCase() : "";
        if (s.contains("attente")) badge.getStyleClass().add("status-waiting");
        else if (s.contains("valid")) badge.getStyleClass().add("status-validated");
        else if (s.contains("exp")) badge.getStyleClass().add("status-shipped");
        else if (s.contains("livr")) badge.getStyleClass().add("status-delivered");
        else if (s.contains("annul")) badge.getStyleClass().add("status-cancelled");
        
        badge.getChildren().add(label);
        return badge;
    }

    private Button createIconButton(String iconConstant, String text) {
        SVGPath icon = IconLibrary.getIcon(iconConstant, 14, "#24316B");
        Button button = new Button(text);
        button.setGraphic(icon);
        button.getStyleClass().add("btn-action");
        return button;
    }

    private void viewOrderDetails(OrderRow order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la commande");
        alert.setHeaderText(order.getOrderId());
        alert.setContentText(String.format("Date: %s\nArticles: %s\nTotal: %s\nStatut: %s", 
                            order.getDate(), order.getArticlesSummary(), order.getTotal(), order.getStatus()));
        alert.showAndWait();
    }

    @FXML private void goBack() { SceneManager.clearCache("panier.fxml"); SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier"); }
    @FXML private void refreshOrders() { loadCommandes(); }
    @FXML private void goToProfile() { SceneManager.switchTo("profile.fxml", "ChriOnline - Mon Profil"); }

    /**
     * Classe interne pour l'affichage uniquement : Simplifie le binding TableView
     * sans nécessiter de DTO architectural dans tout le projet.
     */
    public static class OrderRow {
        private final StringProperty orderId;
        private final StringProperty date;
        private final StringProperty articlesSummary;
        private final StringProperty total;
        private final StringProperty status;

        public OrderRow(Map<String, Object> map) {
            this.orderId = new SimpleStringProperty((String) map.get("reference"));
            this.date = new SimpleStringProperty((String) map.getOrDefault("date", "N/A"));
            this.articlesSummary = new SimpleStringProperty((String) map.getOrDefault("articles_summary", "Articles..."));
            this.total = new SimpleStringProperty((String) map.getOrDefault("total_formatted", "0.00 MAD"));
            this.status = new SimpleStringProperty((String) map.getOrDefault("status_display", "Inconnu"));
        }

        public OrderRow(String id, String date, String summary, String total, String status) {
            this.orderId = new SimpleStringProperty(id);
            this.date = new SimpleStringProperty(date);
            this.articlesSummary = new SimpleStringProperty(summary);
            this.total = new SimpleStringProperty(total);
            this.status = new SimpleStringProperty(status);
        }

        public String getOrderId() { return orderId.get(); }
        public StringProperty orderIdProperty() { return orderId; }
        public String getDate() { return date.get(); }
        public StringProperty dateProperty() { return date; }
        public String getArticlesSummary() { return articlesSummary.get(); }
        public StringProperty articlesSummaryProperty() { return articlesSummary; }
        public String getTotal() { return total.get(); }
        public StringProperty totalProperty() { return total; }
        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
    }
}

