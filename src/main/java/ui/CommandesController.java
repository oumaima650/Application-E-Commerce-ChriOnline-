package ui;

import client.utils.SceneManager;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import java.util.HashMap;
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
    @FXML private TableColumn<OrderRow, String> deliveryDateReelleColumn;
    @FXML private TableColumn<OrderRow, String> totalColumn;
    @FXML private TableColumn<OrderRow, Void> statusColumn;
    @FXML private TableColumn<OrderRow, Void> actionsColumn;
    
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label paginationLabel;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker dateFilter;

    private ObservableList<OrderRow> allOrders = FXCollections.observableArrayList();
    private ObservableList<OrderRow> filteredOrders = FXCollections.observableArrayList();
    private final int PAGE_SIZE = 5;
    private int currentPage = 0;


    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }
        setupTableColumns();
        setupFilters();
        loadCommandes();
    }

    private void setupTableColumns() {
        // Liaison des colonnes simples via les propriétés de OrderRow
        refColumn.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        deliveryDateReelleColumn.setCellValueFactory(cellData -> cellData.getValue().dateLivraisonReelleProperty());
        totalColumn.setCellValueFactory(cellData -> cellData.getValue().totalProperty());
        
        setupStatusColumn();
        setupActionsColumn();
        ordersTable.setItems(FXCollections.observableArrayList()); // Initial empty list
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("Tous", "En attente", "Validée", "Expédiée", "Livrée");
        statusFilter.setValue("Tous");
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterOrders());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOrders());
    }

    private void loadCommandes() {
        Task<Reponse> loadTask = new Task<>() {
            @Override
            protected Reponse call() throws Exception {
                Requete req = new Requete(RequestType.GET_ORDERS,
                        Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()),
                        SessionManager.getInstance().getSession().getAccessToken());
                return ClientSocket.getInstance().envoyer(req);
            }
        };

        loadTask.setOnSucceeded(e -> {
            Reponse rep = loadTask.getValue();
            if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) rep.getDonnees().get("commandes");

                allOrders.clear();
                if (list != null) {
                    for (Map<String, Object> map : list) {
                        allOrders.add(new OrderRow(map));
                    }
                }
                updateFilteredOrders();
            } else {
                System.err.println(
                        "Erreur chargement commandes : " + (rep != null ? rep.getMessage() : "Pas de rÃ©ponse"));
                loadTestData();
            }
        });

        loadTask.setOnFailed(e -> {
            loadTask.getException().printStackTrace();
            loadTestData();
        });

        new Thread(loadTask).start();
    }

    private void loadTestData() {
        allOrders.clear();
        allOrders.add(new OrderRow("#CHR-20260327-0042", "27/03/2026", "", "12 500 MAD", "Livrée"));
        allOrders.add(new OrderRow("#CHR-20260325-0038", "25/03/2026", "", "18 750 MAD", "Expédiée"));
        updateFilteredOrders();
    }

    @FXML private void filterOrders() {
        currentPage = 0;
        updateFilteredOrders();
    }

    private void updateFilteredOrders() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String dateValue = dateFilter.getValue() != null ? dateFilter.getValue().toString() : null;

        filteredOrders.clear();
        for (OrderRow row : allOrders) {
            boolean matchesSearch = searchText.isEmpty() || 
                                    row.getOrderId().toLowerCase().contains(searchText);
            boolean matchesStatus = statusValue.equals("Tous") || row.getStatus().equalsIgnoreCase(statusValue);
            
            String rowDate = row.getDate();
            boolean matchesDate = dateValue == null;
            if (dateValue != null && rowDate != null) {
                String[] parts = dateValue.split("-");
                if (parts.length == 3) {
                    String formattedPickerDate = parts[2] + "/" + parts[1] + "/" + parts[0];
                    matchesDate = rowDate.equals(formattedPickerDate);
                }
            }

            if (matchesSearch && matchesStatus && matchesDate) filteredOrders.add(row);
        }
        updatePagination();
    }

    private void updatePagination() {
        int totalItems = filteredOrders.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        ObservableList<OrderRow> pageData = FXCollections.observableArrayList();
        if (fromIndex < totalItems) {
            pageData.addAll(filteredOrders.subList(fromIndex, toIndex));
        }
        
        ordersTable.setItems(pageData);
        paginationLabel.setText(String.format("Page %d sur %d", currentPage + 1, totalPages));
        
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= totalPages - 1);
    }

    @FXML private void nextPage() { currentPage++; updatePagination(); }
    @FXML private void prevPage() { currentPage--; updatePagination(); }

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
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    OrderRow order = getTableView().getItems().get(getIndex());
                    
                    Button actionButton;
                    if ("EN_ATTENTE".equalsIgnoreCase(order.getStatusRaw())) {
                        actionButton = createIconButton(IconLibrary.CART, "Commander");
                        actionButton.getStyleClass().add("btn-pay");
                        actionButton.setOnAction(event -> resumeOrder(order));
                    } else {
                        actionButton = createIconButton(IconLibrary.ARROW_R, "Détail");
                        actionButton.setOnAction(event -> viewOrderDetails(order));
                    }
                    
                    HBox pane = new HBox(8, actionButton);
                    pane.setStyle("-fx-alignment: CENTER_LEFT;");
                    setGraphic(pane);
                }
            }
        });
    }

    private void resumeOrder(OrderRow order) {
        try {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Reprise de la Commande");
            dialog.setHeaderText("Récapitulatif de votre commande #" + order.getOrderId());
            
            ButtonType checkoutButton = new ButtonType("Procéder au paiement", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(checkoutButton, cancelButton);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setPrefWidth(450);

            Label lblIntro = new Label("Voici les produits de votre commande en attente :");
            lblIntro.setStyle("-fx-font-weight: bold;");

            VBox productsBox = new VBox(10);
            ScrollPane scrollPane = new ScrollPane(productsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(200);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            // Fetch order lines for summary
            ClientSocket client = ClientSocket.getInstance();
            Requete req = new Requete(RequestType.GET_ORDER, Map.of("reference", order.getOrderId()), SessionManager.getInstance().getSession().getAccessToken());
            Reponse rep = client.envoyer(req);

            if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = (Map<String, Object>) rep.getDonnees().get("commande");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lignes = (List<Map<String, Object>>) orderData.get("lignes");
                if (lignes != null) {
                    for (Map<String, Object> line : lignes) {
                        productsBox.getChildren().add(createOrderProductItem(line));
                    }
                }
            } else {
                productsBox.getChildren().add(new Label("Impossible de charger le récapitulatif."));
            }

            Label lblTotal = new Label("Total : " + order.getTotal());
            lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #24316B;");
            
            HBox totalContainer = new HBox(lblTotal);
            totalContainer.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(lblIntro, scrollPane, new Separator(), totalContainer);
            dialog.getDialogPane().setContent(root);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == checkoutButton) return true;
                return false;
            });

            var result = dialog.showAndWait();
            if (result.isPresent() && result.get()) {
                // Set reference for CheckoutController and redirect
                CheckoutController.setResumingOrderReference(order.getOrderId());
                SceneManager.switchTo("checkout.fxml", "ChriOnline - Finaliser la commande");
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la reprise de la commande : " + e.getMessage()).showAndWait();
        }
    }

    private VBox createStatusBadge(String status) {
        VBox badge = new VBox();
        badge.getStyleClass().add("status-badge");
        Label label = new Label(status);
        label.getStyleClass().add("status-text");
        
        String s = status != null ? status.toUpperCase().trim() : "";
        
        if (s.contains("ATTENTE")) {
            badge.getStyleClass().add("status-waiting");
        } else if (s.contains("VALID")) {
            badge.getStyleClass().add("status-validated");
        } else if (s.contains("EXP")) {
            badge.getStyleClass().add("status-shipped");
        } else if (s.contains("LIVR")) {
            badge.getStyleClass().add("status-delivered");
        } else if (s.contains("ANNUL")) {
            badge.getStyleClass().add("status-cancelled");
        }
        
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
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la Commande");
            dialog.setHeaderText("Référence : " + order.getOrderId());
            
            ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setPrefWidth(500);
            root.getStyleClass().add("order-details-root");

            // Info Section (Payment & Address)
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(20);
            infoGrid.setVgap(10);
            
            Label lblPayTitle = new Label("Paiement :");
            lblPayTitle.setStyle("-fx-font-weight: bold;");
            Label lblPayVal = new Label(order.getMethodePaiement());
            
            Label lblAddrTitle = new Label("Adresse de livraison :");
            lblAddrTitle.setStyle("-fx-font-weight: bold;");
            Label lblAddrVal = new Label(order.getAdresseComplete());
            lblAddrVal.setWrapText(true);
            lblAddrVal.setMaxWidth(300);

            infoGrid.add(lblPayTitle, 0, 0);
            infoGrid.add(lblPayVal, 1, 0);
            infoGrid.add(lblAddrTitle, 0, 1);
            infoGrid.add(lblAddrVal, 1, 1);

            // Products Section
            Label lblProducts = new Label("Produits commandés :");
            lblProducts.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #24316B;");
            
            VBox productsBox = new VBox(10);
            ScrollPane scrollPane = new ScrollPane(productsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(250);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            // Request full details from server
            ClientSocket client = ClientSocket.getInstance();
            Requete req = new Requete();
            req.setType(RequestType.GET_ORDER);
            Map<String, Object> params = new HashMap<>();
            params.put("reference", order.getOrderId());
            req.setParametres(params);
            
            Reponse rep = client.envoyer(req);

            if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = (Map<String, Object>) rep.getDonnees().get("commande");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lignes = (List<Map<String, Object>>) orderData.get("lignes");
                
                if (lignes != null) {
                    for (Map<String, Object> ligne : lignes) {
                        productsBox.getChildren().add(createOrderProductItem(ligne));
                    }
                }
            } else {
                productsBox.getChildren().add(new Label("Erreur lors du chargement des produits."));
            }

            // Total Section
            Separator sep = new Separator();
            HBox totalBox = new HBox();
            totalBox.setAlignment(Pos.CENTER_RIGHT);
            Label lblTotal = new Label("Total : " + order.getTotal());
            lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #24316B;");
            totalBox.getChildren().add(lblTotal);

            root.getChildren().addAll(infoGrid, new Separator(), lblProducts, scrollPane, sep, totalBox);
            dialog.getDialogPane().setContent(root);
            
            // Add styles - checking for null to avoid NPE if resource missing
            var cssResource = getClass().getResource("/css/styles.css");
            if (cssResource != null) {
                dialog.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("[CommandesController] styles.css not found at /css/styles.css");
            }
            
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("[CommandesController] Error showing order details: " + e.getMessage());
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Impossible d'afficher les détails : " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    private HBox createOrderProductItem(Map<String, Object> ligne) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
        imageView.setPreserveRatio(true);
        String imageUrl = ligne != null ? (String) ligne.get("image") : null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                imageView.setImage(new Image(imageUrl, true));
            } catch (Exception e) {
                // Ignore and use placeholder
                var placeholder = getClass().getResourceAsStream("/img/placeholder.png");
                if (placeholder != null) imageView.setImage(new Image(placeholder));
            }
        } else {
            var placeholder = getClass().getResourceAsStream("/img/placeholder.png");
            if (placeholder != null) imageView.setImage(new Image(placeholder));
        }

        VBox details = new VBox(2);
        String productName = ligne != null ? (String) ligne.getOrDefault("nomProduit", "Produit inconnu") : "Produit inconnu";
        Label name = new Label(productName);
        name.setStyle("-fx-font-weight: bold;");
        
        int quantite = 0;
        double prixAchat = 0.0;
        double lineTotal = 0.0;
        
        if (ligne != null) {
            Object q = ligne.get("quantite");
            if (q instanceof Number) quantite = ((Number) q).intValue();
            
            Object p = ligne.get("prixAchat");
            if (p instanceof Number) prixAchat = ((Number) p).doubleValue();
            
            Object s = ligne.get("sousTotal");
            if (s instanceof Number) lineTotal = ((Number) s).doubleValue();
        }

        Label qtyPrice = new Label(String.format("Quantité: %d x %.2f MAD", quantite, prixAchat));
        qtyPrice.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        details.getChildren().addAll(name, qtyPrice);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label subtotal = new Label(String.format("%.2f MAD", lineTotal));
        subtotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #24316B;");

        item.getChildren().addAll(imageView, details, spacer, subtotal);
        return item;
    }

    @FXML private void goBack() { SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier"); }
    @FXML private void refreshOrders() { loadCommandes(); }
    @FXML private void goToProfile() { SceneManager.switchTo("profile.fxml", "ChriOnline - Mon Profil"); }

    /**
     * Classe interne pour l'affichage uniquement : Simplifie le binding TableView
     * sans nécessiter de DTO architectural dans tout le projet.
     */
    public static class OrderRow {
        private final StringProperty orderId;
        private final StringProperty date;
        private final StringProperty dateLivraisonReelle;
        private final StringProperty total;
        private final StringProperty status;
        private final String statusRaw;
        private final String methodePaiement;
        private final String adresseComplete;

        public OrderRow(Map<String, Object> map) {
            this.orderId = new SimpleStringProperty((String) map.get("reference"));
            this.date = new SimpleStringProperty((String) map.getOrDefault("date", "N/A"));
            this.dateLivraisonReelle = new SimpleStringProperty((String) map.getOrDefault("date_livraison_reelle", ""));
            this.total = new SimpleStringProperty((String) map.getOrDefault("total_formatted", "0.00 MAD"));
            this.status = new SimpleStringProperty((String) map.getOrDefault("status_display", "Inconnu"));
            this.statusRaw = (String) map.getOrDefault("statut", "");
            this.methodePaiement = (String) map.getOrDefault("methode_paiement", "N/A");
            this.adresseComplete = (String) map.getOrDefault("adresse_complete", "N/A");
        }

        public OrderRow(String id, String date, String deliveryDate, String total, String status) {
            this.orderId = new SimpleStringProperty(id);
            this.date = new SimpleStringProperty(date);
            this.dateLivraisonReelle = new SimpleStringProperty(deliveryDate);
            this.total = new SimpleStringProperty(total);
            this.status = new SimpleStringProperty(status);
            this.statusRaw = "";
            this.methodePaiement = "N/A";
            this.adresseComplete = "N/A";
        }

        public String getOrderId() { return orderId.get(); }
        public StringProperty orderIdProperty() { return orderId; }
        public String getDate() { return date.get(); }
        public StringProperty dateProperty() { return date; }
        public String getDateLivraisonReelle() { return dateLivraisonReelle.get(); }
        public StringProperty dateLivraisonReelleProperty() { return dateLivraisonReelle; }
        public String getTotal() { return total.get(); }
        public StringProperty totalProperty() { return total; }
        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
        public String getStatusRaw() { return statusRaw; }
        public String getMethodePaiement() { return methodePaiement; }
        public String getAdresseComplete() { return adresseComplete; }
    }
}

