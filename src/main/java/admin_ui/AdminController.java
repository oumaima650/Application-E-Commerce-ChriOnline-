package admin_ui;

import client.ClientSocket;
import javafx.stage.Stage;
import model.Commande;
import model.Produit;
import model.SKU;
import model.Client;
import model.Notification;
import model.enums.StatutCommande;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class AdminController {
    // Stockage temporaire des données brutes pour les cellValueFactory

    private List<Map<String, Object>> rawCommandesData = new ArrayList<>();

    @FXML private VBox productContainer;
    @FXML private TableView<Commande> tableCommandes;
    @FXML private TableView<Client> tableClients;
    @FXML private TextField searchProduitField;
    @FXML private Button btnAddSku;
    
    @FXML private TextField searchCommandeField;
    @FXML private TextField searchClientField;
    @FXML private javafx.scene.layout.StackPane notifBadge;
    @FXML private Label notifCount;
    @FXML private javafx.scene.shape.Circle notifCircle;


    // Colonnes pour les commandes
    @FXML private TableColumn<Commande, String> colId;
    @FXML private TableColumn<Commande, String> colClient;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, String> colAdresse;
    @FXML private TableColumn<Commande, String> colTotal;
    @FXML private TableColumn<Commande, String> colDateLivraisonPrevue;
    @FXML private TableColumn<Commande, String> colDateLivraisonReelle;
    @FXML private TableColumn<Commande, Void> colActionsCommande;
    
    // --- TAB CLIENTS ---
    @FXML private TableColumn<Client, String> colStatutClient;
    @FXML private TableColumn<Client, Void> colActionsClient;

    @FXML
    private void ouvrirNotifications() {
        SceneManager.switchTo("notifications.fxml", "ChriOnline - Notifications");
    }

    @FXML
    private void handleLogout() {
        System.out.println("[AdminController] Déconnexion en cours...");
        SessionManager.getInstance().fermer();
        SceneManager.clearHistory();
        SceneManager.switchTo("admin_login.fxml", "ChriOnline - Connexion");
    }

    @FXML
    public void initialize() {
        // Configurer les colonnes
        setupColumns();

        // Charger les données depuis le backend via TCP
        loadProduits("");
        loadCommandes("");
        loadClients("");
        loadUnreadCount();
        
        // Configurer recherche
        if (searchProduitField != null) {
            searchProduitField.textProperty().addListener((observable, oldValue, newValue) -> {
                loadProduits(newValue);
            });
        }
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
        
        if (searchProduitField != null) {
            searchProduitField.textProperty().addListener((observable, oldValue, newValue) -> {
                loadProduits(newValue);
            });
        }

        // Configurer les colonnes interactives
        setupCommandeActions();
        setupClientActions();
    }

    private void loadUnreadCount() {
        if (!SessionManager.getInstance().isAuthenticated()) return;
        
        int userId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        String token = SessionManager.getInstance().getSession().getAccessToken();

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
                        if (notifCount != null) {
                            notifCount.setText(String.valueOf(unread));
                            notifCount.setVisible(unread > 0);
                        }
                        if (notifCircle != null) {
                            notifCircle.setVisible(unread > 0);
                        }
                        // notifBadge (la cloche) reste toujours visible
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
 
        // -- Colonnes SKUs partagées? Non, on fera ça dynamiquement --

        // -- Colonnes Client ------------------------------------------------
        if (colStatutClient != null) {
            colStatutClient.setCellValueFactory(cellData -> {
                Client c = cellData.getValue();
                String status = c.getStatut() != null ? c.getStatut() : "CLIENT";
                return new javafx.beans.property.SimpleStringProperty(status);
            });
        }
    }

    private void loadProduits(String searchText) {
        if (!SessionManager.getInstance().isAuthenticated()) return;
        String token = SessionManager.getInstance().getSession().getAccessToken();

        javafx.concurrent.Task<Reponse> task = new javafx.concurrent.Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new HashMap<>();
                if (searchText != null && !searchText.isEmpty()) params.put("query", searchText);
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.ADMIN_GET_ALL_PRODUCTS, params, token));
            }
        };

        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                @SuppressWarnings("unchecked")
                List<Produit> produits = (List<Produit>) rep.getDonnees().get("produits");
                javafx.application.Platform.runLater(() -> {
                    productContainer.getChildren().clear();
                    if (produits != null) {
                        for (Produit p : produits) {
                            productContainer.getChildren().add(createProductBlock(p));
                        }
                    }
                });
            } else {
                String msg = (rep != null) ? rep.getMessage() : "Échec de la communication";
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText("Impossible de charger les produits");
                    alert.setContentText(msg);
                    alert.showAndWait();
                });
            }
        });
        new Thread(task).start();
    }

    private VBox createProductBlock(Produit p) {
        VBox block = new VBox(15);
        block.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox titles = new VBox(5);
        
        HBox nameAndBadge = new HBox(10);
        nameAndBadge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblName = new Label(p.getNom());
        lblName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2A2C41;");
        
        if (p.getNomCategorie() != null && !p.getNomCategorie().isEmpty()) {
            Label badge = new Label(p.getNomCategorie());
            badge.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4F46E5; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            nameAndBadge.getChildren().add(badge);
        }
        nameAndBadge.getChildren().add(0, lblName);

        Label lblDesc = new Label(p.getDescription());
        lblDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        titles.getChildren().addAll(nameAndBadge, lblDesc);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().add("secondary-btn");
        btnEdit.setStyle("-fx-background-color: #FDBF50; -fx-text-fill: #2A2C41;");
        btnEdit.setOnAction(e -> {
            showProductEditDialog(p);
        });
        
        Button btnDelete = new Button(p.getDeletedAt() == null ? "Supprimer" : "Restaurer");
        btnDelete.setStyle(p.getDeletedAt() == null ? "-fx-background-color: #FF724C; -fx-text-fill: white;" : "-fx-background-color: #2ECC71; -fx-text-fill: white;");
        btnDelete.setOnAction(e -> {
            sendDeleteRequest(RequestType.ADMIN_DELETE_PRODUCT, p.getIdProduit(), true);
        });
        
        header.getChildren().addAll(titles, spacer, btnEdit, btnDelete);
        
        // Table des SKUs
        TableView<SKU> skuTable = new TableView<>();
        skuTable.setPrefHeight(200);
        skuTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<SKU, String> colCode = new TableColumn<>("Code SKU");
        colCode.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("sku"));
        
        TableColumn<SKU, String> colPrix = new TableColumn<>("Prix");
        colPrix.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.format("%.2f €", c.getValue().getPrix())));
        
        TableColumn<SKU, String> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
        
        TableColumn<SKU, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button specEdit = new Button("Edit");
            private final Button specDel = new Button("Del");
            private final HBox specBox = new HBox(8, specEdit, specDel);
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setOpacity(1.0);
                } else {
                    SKU s = (SKU) getTableRow().getItem();
                    boolean isDeleted = s.getDeletedAt() != null;
                    
                    specEdit.setOnAction(e -> showSKUEditDialog(s));
                    
                    if (isDeleted) {
                        specDel.setText("Restaurer");
                        specDel.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-size: 11px;");
                        setOpacity(0.5);
                    } else {
                        specDel.setText("Supprimer");
                        specDel.setStyle("-fx-background-color: #FF724C; -fx-text-fill: white; -fx-font-size: 11px;");
                        setOpacity(1.0);
                    }
                    
                    specDel.setOnAction(e -> sendDeleteRequest(RequestType.DELETE_SKU, s.getSku(), false));
                    setGraphic(specBox);
                }
            }
        });
        
        skuTable.getColumns().addAll(colCode, colPrix, colStock, colActions);
        
        block.getChildren().addAll(header, skuTable);
        
        // Load data for this table
        loadSkusForTable(p, skuTable);
        
        return block;
    }

    private void loadSkusForTable(Produit p, TableView<SKU> table) {
        String token = SessionManager.getInstance().getSession().getAccessToken();
        javafx.concurrent.Task<Reponse> task = new javafx.concurrent.Task<>() {
            @Override
            protected Reponse call() {
                Map<String, Object> params = new HashMap<>();
                params.put("idProduit", p.getIdProduit());
                return ClientSocket.getInstance().envoyer(new Requete(RequestType.ADMIN_GET_SKU_BY_PRODUIT, params, token));
            }
        };
        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                @SuppressWarnings("unchecked")
                List<SKU> skus = (List<SKU>) rep.getDonnees().get("skus");
                javafx.application.Platform.runLater(() -> table.setItems(FXCollections.observableArrayList(skus)));
            }
        });
        new Thread(task).start();
    }

    @FXML private void handleNewProduit() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/product_add_wizard.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Ajouter un nouveau Produit (Wizard)");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(productContainer.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(page));

            // Le controller se rafraîchira tout seul si on ajoute un callback ou si on recharge ici
            dialogStage.showAndWait();
            
            // Rafraîchir la liste après la fermeture du wizard
            loadProduits(searchProduitField.getText());
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setContentText("Impossible d'ouvrir le wizard : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML private void handleNewSKU() {
        // TODO: Implémenter modale ajout SKU
        System.out.println("Ajouter nouveau SKU");
    }


    private void sendDeleteRequest(RequestType type, Object id, boolean isProduct) {
        System.out.println("[AdminController] Envoi requête suppression - Type: " + type + ", ID: " + id);
        String token = SessionManager.getInstance().getSession().getAccessToken();
        Map<String, Object> params = new HashMap<>();
        if (isProduct) params.put("idProduit", id);
        else params.put("sku", id);
        
        new Thread(() -> {
            try {
                Reponse rep = ClientSocket.getInstance().envoyer(new Requete(type, params, token));
                javafx.application.Platform.runLater(() -> {
                    if (rep != null && rep.isSucces()) {
                        loadProduits(searchProduitField.getText());
                    } else {
                        String msg = (rep != null) ? rep.getMessage() : "Le serveur n'a pas répondu.";
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText("L'opération a échoué");
                        alert.setContentText(msg);
                        alert.showAndWait();
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erreur Fatale");
                    alert.setContentText("Connexion perdue ou erreur client : " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void showProductEditDialog(Produit p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/product_edit.fxml"));
            VBox page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier le Produit");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(productContainer.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(page));

            ProductEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProduit(p);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadProduits(searchProduitField.getText());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void showSKUEditDialog(SKU sku) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/sku_edit.fxml"));
            VBox page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier SKU");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(productContainer.getScene().getWindow());
            javafx.scene.Scene scene = new javafx.scene.Scene(page);
            dialogStage.setScene(scene);

            SKUEditController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setSKU(sku);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadProduits(searchProduitField.getText());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
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
                String adminToken = SessionManager.getInstance().getSession().getAccessToken();
                
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
                    
                    Object commandesObj = reponse.getDonnees().get("commandes");
                    List<Map<String, Object>> commandesData = new ArrayList<>();

                    if (commandesObj instanceof String) {
                        // La donnée est chiffrée par le serveur pour le transport réseau
                        try {
                            byte[] combined = java.util.Base64.getDecoder().decode((String) commandesObj);
                            byte[] iv = new byte[12];
                            byte[] encryptedBytes = new byte[combined.length - 12];
                            System.arraycopy(combined, 0, iv, 0, 12);
                            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.length);

                            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
                            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
                            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, ClientSocket.getInstance().getSessionSecretKey(), spec);
                            
                            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                            
                            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decryptedBytes);
                                 java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
                                commandesData = (List<Map<String, Object>>) ois.readObject();
                            }
                        } catch (Exception e) {
                            System.err.println("[AdminController] Erreur déchiffrement commandes: " + e.getMessage());
                        }
                    } else if (commandesObj instanceof List) {
                        commandesData = (List<Map<String, Object>>) commandesObj;
                    }
 
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
        
        String token = SessionManager.getInstance().getSession().getAccessToken();
        
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
                Object clientsObj = rep.getDonnees().get("clients");
                List<Client> clientsData = new ArrayList<>();

                if (clientsObj instanceof String) {
                    try {
                        byte[] combined = java.util.Base64.getDecoder().decode((String) clientsObj);
                        byte[] iv = new byte[12];
                        byte[] encryptedBytes = new byte[combined.length - 12];
                        System.arraycopy(combined, 0, iv, 0, 12);
                        System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.length);

                        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
                        javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
                        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, ClientSocket.getInstance().getSessionSecretKey(), spec);
                        
                        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                        
                        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(decryptedBytes);
                             java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais)) {
                            clientsData = (List<Client>) ois.readObject();
                        }
                    } catch (Exception ex) {
                        System.err.println("[AdminController] Erreur déchiffrement clients: " + ex.getMessage());
                    }
                } else if (clientsObj instanceof List) {
                    clientsData = (List<Client>) clientsObj;
                }

                final List<Client> finalClients = clientsData;
                javafx.application.Platform.runLater(() -> {
                    tableClients.setItems(FXCollections.observableArrayList(finalClients));
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
                                        
                                        String adminToken = SessionManager.getInstance().getSession().getAccessToken();
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
                                params.put("targetUserId", item.getId());
                                
                                String adminToken = SessionManager.getInstance().getSession().getAccessToken();
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