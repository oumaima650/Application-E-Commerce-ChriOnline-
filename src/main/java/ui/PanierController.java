package ui;

import client.utils.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import client.ClientSocket;
import shared.RequestType;
import shared.Requete;
import shared.Reponse;
import client.utils.SessionManager;
import ui.utils.IconLibrary;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PanierController implements Initializable {

    @FXML
    private Label badgeArticles;
    @FXML
    private VBox panierItemsBox;
    @FXML
    private Label lblSousTotal;
    @FXML
    private Label lblTotalTTC;
    @FXML
    private Button btnValider;
    @FXML
    private ScrollPane panierScrollPane;
    @FXML
    private StackPane rootStackPane;
    
    // Navbar components
    @FXML
    private Button loginButton;
    @FXML
    private StackPane userAvatarContainer;
    @FXML
    private Circle userAvatar;
    @FXML
    private Label userInitial;

    private static final String CORAIL = "#FF724C";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Double> unitPrices = new HashMap<>();
    private final Map<String, Integer> currentQuantities = new HashMap<>();
    private final Map<String, HBox> itemRows = new HashMap<>();
    private final Map<String, Boolean> selectedItems = new HashMap<>();
    private final Map<String, CheckBox> checkBoxes = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }

        if (panierScrollPane != null) {
            panierScrollPane.setFitToHeight(false);
            panierScrollPane.setFitToWidth(true);
        }

        setupNavbar();
        chargerPanier();
    }

    private void setupNavbar() {
        boolean loggedIn = SessionManager.getInstance().isAuthenticated();
        if (loginButton != null) {
            loginButton.setVisible(!loggedIn);
            loginButton.setManaged(!loggedIn);
        }
        if (userAvatarContainer != null) {
            userAvatarContainer.setVisible(loggedIn);
            userAvatarContainer.setManaged(loggedIn);
        }

        if (loggedIn && SessionManager.getInstance().getCurrentUser() != null) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            String name = email.split("@")[0].toUpperCase();
            if (userInitial != null) {
                userInitial.setText(name.substring(0, 1));
            }
        }

        if (userAvatar != null) {
            userAvatar.setFill(Color.web(CORAIL));
        }
    }

    @FXML
    private void handleLoginClick() {
        SessionManager.getInstance().clearPendingRedirect();
        SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
    }

    @FXML
    private void handleUserClick() {
        if (userAvatar == null) return;

        ContextMenu userMenu = new ContextMenu();
        
        if (SessionManager.getInstance().isAuthenticated()) {
            MenuItem miCompte = new MenuItem("Mon compte");
            MenuItem miCommandes = new MenuItem("Mes commandes");
            MenuItem miDeconnexion = new MenuItem("Déconnexion");
            
            miCompte.setOnAction(e -> SceneManager.switchTo("profile.fxml", "Mon Profil - ChriOnline"));
            miCommandes.setOnAction(e -> SceneManager.switchTo("commandes.fxml", "Mes Commandes - ChriOnline"));
            
            miDeconnexion.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold;");
            miDeconnexion.setOnAction(e -> {
                SessionManager.getInstance().fermer();
                SessionManager.getInstance().clearPendingRedirect();
                SceneManager.switchTo("main-home.fxml", "Boutique - ChriOnline");
            });
            
            userMenu.getItems().addAll(miCompte, miCommandes, new SeparatorMenuItem(), miDeconnexion);
        } else {
            MenuItem miConnexion = new MenuItem("Se connecter");
            miConnexion.setStyle("-fx-font-weight: bold; -fx-text-fill: " + CORAIL + ";");
            miConnexion.setOnAction(e -> {
                SessionManager.getInstance().clearPendingRedirect();
                SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            });
            userMenu.getItems().add(miConnexion);
        }
        
        userMenu.show(userAvatar, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleValiderCommande() {
        if (selectedItems == null) return;
        List<String> skus = selectedItems.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();

        if (skus.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Panier Vide");
            alert.setHeaderText("Action impossible");
            alert.setContentText(
                    "Votre panier est vide ou aucun article n'est sélectionné. Veuillez ajouter des produits pour passer une commande.");
            alert.showAndWait();
            return;
        }

        System.out.println("Navigation vers le checkout avec " + skus.size() + " articles...");
        CheckoutController.setSelectedSkus(skus);
        SceneManager.switchTo("checkout.fxml", "ChriOnline - Paiement Sécurisé");
    }

    private void chargerPanier() {
        Task<Reponse> fetchTask = new Task<>() {
            @Override
            protected Reponse call() throws Exception {
                Requete req = new Requete(RequestType.GET_CART, Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()), SessionManager.getInstance().getSession().getAccessToken());
                return ClientSocket.getInstance().envoyer(req);
            }

        };

        fetchTask.setOnSucceeded(e -> updateUIWithCartData(fetchTask.getValue()));
        dbExecutor.submit(fetchTask);
    }

    private void updateUIWithCartData(Reponse rep) {
        if (panierItemsBox != null) {
            panierItemsBox.getChildren().clear();
        }
        unitPrices.clear();
        currentQuantities.clear();
        itemRows.clear();
        selectedItems.clear();
        checkBoxes.clear();

        int totalArticles = 0;
        double totalGlobal = 0.0;

        if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
            Map<String, Object> donnees = rep.getDonnees();
            List<Map<String, Object>> lignes = (List<Map<String, Object>>) donnees.get("lignes");

            if (lignes != null) {
                for (int i = 0; i < lignes.size(); i++) {
                    Map<String, Object> ligne = lignes.get(i);
                    String sku = (String) ligne.get("sku");
                    int qty = (Integer) ligne.get("quantite");
                    double sousTotal = ((Number) ligne.get("sousTotal")).doubleValue();
                    String icon = (String) ligne.get("image");

                    double unitPrice = (qty > 0) ? sousTotal / qty : 0;
                    unitPrices.put(sku, unitPrice);
                    currentQuantities.put(sku, qty);
                    selectedItems.put(sku, true); // Par défaut tout est coché

                    totalArticles += qty;
                    totalGlobal += sousTotal;

                    boolean isHighlighted = (i == 0);
                    HBox row = createCartItemRow(sku, sku, "Ref: " + sku, qty, sousTotal, "cat-phone", icon,
                            isHighlighted);
                    itemRows.put(sku, row);
                    panierItemsBox.getChildren().add(row);
                }
            }
            updateSummaryLabels(totalArticles, totalGlobal);
        }
    }

    private void updateSummaryLabels(int nbArticles, double total) {
        if (badgeArticles != null)
            badgeArticles.setText(nbArticles + " article" + (nbArticles > 1 ? "s" : ""));
        String fmt = String.format("%.2f MAD", total).replace(",", " ");
        if (lblSousTotal != null)
            lblSousTotal.setText(fmt);
        if (lblTotalTTC != null)
            lblTotalTTC.setText(fmt);
    }

    private void updateQuantite(String sku, int nextQty, Label lblQty, Label lblPrice) {
        if (nextQty < 0)
            return;
        if (nextQty == 0) {
            removeProduit(sku);
            return;
        }

        // --- OPTIMISTIC UI UPDATE (Instant) ---
        currentQuantities.put(sku, nextQty);
        double uPrice = unitPrices.getOrDefault(sku, 0.0);
        lblQty.setText(String.valueOf(nextQty));
        lblPrice.setText(String.format("%.2f MAD", nextQty * uPrice).replace(",", " "));

        recalculateTotalsFromLocalData();

        // --- BACKGROUND SYNC (to Server) ---
        dbExecutor.submit(() -> {
            Map<String, Object> p = new HashMap<>();
            p.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
            p.put("sku", sku);
            p.put("quantite", nextQty);
            
            Requete req = new Requete(RequestType.UPDATE_QUANTITY_CART, p, SessionManager.getInstance().getSession().getAccessToken());

            Reponse res = ClientSocket.getInstance().envoyer(req);
            if (!res.isSucces()) {
                Platform.runLater(this::chargerPanier); // Revert on failure
            }
        });
    }

    private void recalculateTotalsFromLocalData() {
        int totalItems = 0;
        double totalSum = 0;

        for (String sku : currentQuantities.keySet()) {
            if (selectedItems.getOrDefault(sku, false)) {
                int qty = currentQuantities.get(sku);
                totalItems += qty;
                totalSum += qty * unitPrices.getOrDefault(sku, 0.0);
            }
        }
        updateSummaryLabels(totalItems, totalSum);

        if (btnValider != null) {
            btnValider.setDisable(totalItems == 0);
        }
    }

    private void removeProduit(String sku) {
        // --- OPTIMISTIC UI REMOVE (Instant) ---
        HBox row = itemRows.get(sku);
        if (row != null && panierItemsBox != null) {
            panierItemsBox.getChildren().remove(row);
        }
        currentQuantities.remove(sku);
        selectedItems.remove(sku);
        checkBoxes.remove(sku);
        itemRows.remove(sku);

        recalculateTotalsFromLocalData();

        // --- BACKGROUND SYNC (to Server) ---
        dbExecutor.submit(() -> {
            Map<String, Object> p = new HashMap<>();
            p.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
            p.put("sku", sku);
            Requete req = new Requete(RequestType.REMOVE_FROM_CART, p, SessionManager.getInstance().getSession().getAccessToken());

            Reponse res = ClientSocket.getInstance().envoyer(req);
            if (!res.isSucces()) {
                Platform.runLater(this::chargerPanier); // Revert on failure
            }
        });
    }

    private HBox createCartItemRow(String sku, String name, String subText, int qty, double prix, String catClass,
            String svgPathStr, boolean highlighted) {
        HBox row = new HBox(14);
        row.getStyleClass().add(highlighted ? "cart-item-highlighted" : "cart-item");
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("item-checkbox");
        cb.setSelected(selectedItems.getOrDefault(sku, true));
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            selectedItems.put(sku, newVal);
            recalculateTotalsFromLocalData();
        });
        checkBoxes.put(sku, cb);

        
        StackPane thumb = new StackPane();
        thumb.getStyleClass().addAll("item-thumbnail", catClass);
        thumb.setPrefSize(80, 80);
        
        if (svgPathStr != null && (svgPathStr.startsWith("http") || svgPathStr.startsWith("https"))) {
            ImageView imgView = new ImageView();
            try {
                Image img = new Image(svgPathStr, true); // true for background loading
                imgView.setImage(img);
                imgView.setFitWidth(70);
                imgView.setFitHeight(70);
                imgView.setPreserveRatio(true);
                
                // Rounded corners for image
                Rectangle clip = new Rectangle(70, 70);
                clip.setArcWidth(12);
                clip.setArcHeight(12);
                imgView.setClip(clip);
                
                thumb.getChildren().add(imgView);
            } catch (Exception e) {
                SVGPath icon = IconLibrary.getIcon(IconLibrary.PHONE, 24, "#FFFFFF");
                thumb.getChildren().add(icon);
            }
        } else {
            // Fallback to SVG if it's not a URL
            String fallbackIcon = (svgPathStr == null || svgPathStr.isEmpty()) ? IconLibrary.PHONE : svgPathStr;
            SVGPath icon = IconLibrary.getIcon(fallbackIcon, 24, "#FFFFFF");
            icon.getStyleClass().add("item-icon");
            thumb.getChildren().add(icon);
        }
        
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        Label n = new Label(name);
        n.getStyleClass().add("item-name");
        Label s = new Label(subText);
        s.getStyleClass().add("item-sub");
        info.getChildren().addAll(n, s);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lp = new Label(String.format("%.2f MAD", prix).replace(",", " "));
        lp.getStyleClass().add("item-price");
        lp.setMinWidth(90);
        lp.setAlignment(Pos.CENTER_RIGHT);

        Label lq = new Label(String.valueOf(qty));
        lq.getStyleClass().add("qty-label");

        HBox qBox = new HBox(10);
        qBox.getStyleClass().add("qty-box");
        qBox.setAlignment(Pos.CENTER);
        Button bm = new Button("−");
        bm.getStyleClass().add("qty-btn");
        bm.setOnAction(e -> updateQuantite(sku, currentQuantities.get(sku) - 1, lq, lp));
        Button bp = new Button("+");
        bp.getStyleClass().add("qty-btn");
        bp.setOnAction(e -> updateQuantite(sku, currentQuantities.get(sku) + 1, lq, lp));
        qBox.getChildren().addAll(bm, lq, bp);

        Button bd = new Button();
        bd.getStyleClass().add("delete-btn");
        SVGPath tr = IconLibrary.getIcon(IconLibrary.TRASH, 16, "#E74C3C");
        tr.getStyleClass().add("icon-trash");
        bd.setGraphic(tr);
        bd.setOnAction(e -> removeProduit(sku));

        row.getChildren().addAll(cb, thumb, info, sp, qBox, lp, bd);

        return row;
    }

    @FXML
    private void handleViderPanier() {
        if (panierItemsBox != null) {
            panierItemsBox.getChildren().clear();
        }
        unitPrices.clear();
        currentQuantities.clear();
        itemRows.clear();
        selectedItems.clear();
        checkBoxes.clear();

        recalculateTotalsFromLocalData();

        dbExecutor.submit(() -> {
            Map<String, Object> p = new HashMap<>();
            p.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
            Requete req = new Requete(RequestType.CLEAR_CART, p, SessionManager.getInstance().getSession().getAccessToken());

            Reponse res = ClientSocket.getInstance().envoyer(req);
            if (!res.isSucces()) {
                Platform.runLater(this::chargerPanier); // Revert on failure
            }
        });
    }

    @FXML
    private void goToHome() {
        SceneManager.switchTo("main-home.fxml", "ChriOnline - Accueil");
    }
}
