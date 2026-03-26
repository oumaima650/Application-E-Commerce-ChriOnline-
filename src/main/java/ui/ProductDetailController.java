package ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import client.utils.SessionManager;
import client.utils.SceneManager;
import ui.utils.IconLibrary;
import model.ProduitAffichable;

import java.net.URL;
import java.util.*;
import java.io.*;
import java.net.Socket;
import java.math.BigDecimal;

public class ProductDetailController implements Initializable {

    private static int selectedProductId;
    public static void setSelectedProductId(int id) { selectedProductId = id; }

    @FXML private Label logoLabel;
    @FXML private Button cartButton;
    @FXML private ImageView productImageView;
    @FXML private Label categoryLabel;
    @FXML private Label productNameLabel;
    @FXML private Label priceLabel;
    @FXML private Label originalPriceLabel;
    @FXML private Label promoBadge;
    @FXML private HBox ratingBox;
    @FXML private Label reviewsLabel;
    @FXML private VBox variantsContainer;
    @FXML private Label quantityLabel;
    @FXML private Label stockLabel;
    @FXML private Button addToCartButton;

    private int quantity = 1;
    private ProduitAffichable currentProduit;
    private Map<String, List<Map<String, Object>>> variantsMap = new HashMap<>();
    private Map<String, Integer> selectedPVVs = new HashMap<>(); // VariantName -> PVVId
    private String currentSelectedSku = null;

    private static final String CORAIL = "#FF724C";
    private static final String BLEU_NUIT = "#2A2C41";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (logoLabel != null) logoLabel.setStyle("-fx-text-fill: #FDBF50; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        loadProductDetails();
        loadVariants();
        updateCartBadge();
    }

    private void loadProductDetails() {
        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getToken() : "";
                return sendRequest(new Requete(RequestType.GET_PRODUIT_BY_ID, Map.of("id", selectedProductId), token));
            }
        };
        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                Map<String, Object> data = (Map<String, Object>) rep.getDonnees().get("produit");
                Platform.runLater(() -> displayProduct(data));
            }
        });
        new Thread(task).start();
    }

    private void displayProduct(Map<String, Object> data) {
        productNameLabel.setText((String) data.get("nom"));
        categoryLabel.setText(((String) data.get("categorie")).toUpperCase());
        
        BigDecimal price = (BigDecimal) data.get("prix");
        priceLabel.setText(price.intValue() + " MAD");
        
        currentSelectedSku = (String) data.get("sku");
        int stock = (Integer) data.get("stock");
        stockLabel.setText("En stock (" + stock + " unités)");
        stockLabel.setStyle("-fx-text-fill: " + (stock > 0 ? "#10B981" : "#EF4444") + "; -fx-font-weight: bold;");
        
        // Stars
        ratingBox.getChildren().clear();
        for (int i=0; i<5; i++) {
            ratingBox.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 14, "#FDBF50"));
        }
    }

    private void loadVariants() {
        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getToken() : "";
                return sendRequest(new Requete(RequestType.GET_PRODUCT_VARIANTS, Map.of("idProduit", selectedProductId), token));
            }
        };
        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                List<Map<String, Object>> pvvList = (List<Map<String, Object>>) rep.getDonnees().get("pvv");
                Platform.runLater(() -> setupVariantSelectors(pvvList));
            }
        });
        new Thread(task).start();
    }

    private void setupVariantSelectors(List<Map<String, Object>> pvvList) {
        variantsContainer.getChildren().clear();
        variantsMap.clear();

        // Group by variant name (e.g., "Couleur", "Taille")
        for (Map<String, Object> pvv : pvvList) {
            String varName = (String) pvv.get("nomVariante");
            variantsMap.computeIfAbsent(varName, k -> new ArrayList<>()).add(pvv);
        }

        for (String varName : variantsMap.keySet()) {
            VBox box = new VBox(10);
            Label lbl = new Label(varName);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 14px;");
            
            FlowPane options = new FlowPane(10, 10);
            ToggleGroup group = new ToggleGroup();
            
            List<Map<String, Object>> values = variantsMap.get(varName);
            for (Map<String, Object> val : values) {
                ToggleButton btn = new ToggleButton((String) val.get("valeur"));
                btn.setToggleGroup(group);
                btn.setStyle("-fx-background-color: white; -fx-border-color: #DDD; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8 20; -fx-cursor: hand;");
                
                btn.selectedProperty().addListener((obs, oldV, newV) -> {
                    if (newV) {
                        btn.setStyle("-fx-background-color: " + CORAIL + "; -fx-text-fill: white; -fx-background-radius: 10px; -fx-padding: 8 20; -fx-font-weight: bold;");
                        selectedPVVs.put(varName, (Integer) val.get("idPVV"));
                        resolveSKU();
                    } else {
                        btn.setStyle("-fx-background-color: white; -fx-border-color: #DDD; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8 20; -fx-cursor: hand;");
                    }
                });
                options.getChildren().add(btn);
            }
            
            box.getChildren().addAll(lbl, options);
            variantsContainer.getChildren().add(box);
        }
    }

    private void resolveSKU() {
        if (selectedPVVs.size() < variantsMap.size()) return; // Wait for all variants to be selected

        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getToken() : "";
                return sendRequest(new Requete(RequestType.GET_SKU_BY_VARIANTS, 
                    Map.of("idProduit", selectedProductId, "pvvIds", new ArrayList<>(selectedPVVs.values())), 
                    token));
            }
        };
        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                Map<String, Object> skuData = (Map<String, Object>) rep.getDonnees().get("sku");
                Platform.runLater(() -> updateSkuInfo(skuData));
            }
        });
        new Thread(task).start();
    }

    private void updateSkuInfo(Map<String, Object> skuData) {
        BigDecimal price = (BigDecimal) skuData.get("prix");
        priceLabel.setText(price.intValue() + " MAD");
        
        currentSelectedSku = (String) skuData.get("codeSku");
        int stock = (Integer) skuData.get("stock");
        stockLabel.setText("En stock (" + stock + " unités)");
        stockLabel.setStyle("-fx-text-fill: " + (stock > 0 ? "#10B981" : "#EF4444") + "; -fx-font-weight: bold;");
        
        // If image exists in SKU, update it
        String img = (String) skuData.get("image");
        if (img != null && !img.isEmpty()) {
            // Logic to load new image...
        }
    }

    @FXML private void handleBack() { SceneManager.back(); }
    @FXML private void handleCart() { SceneManager.switchTo("panier.fxml", "Mon Panier - ChriOnline"); }
    
    @FXML private void incrementQty() { quantity++; quantityLabel.setText(String.valueOf(quantity)); }
    @FXML private void decrementQty() { if (quantity > 1) { quantity--; quantityLabel.setText(String.valueOf(quantity)); } }

    @FXML private void handleAddToCart() {
        if (currentSelectedSku == null) {
            showToast("Veuillez sélectionner toutes les options");
            return;
        }
        
        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated()) return null;
                return sendRequest(new Requete(RequestType.ADD_TO_CART, 
                    Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur(), "sku", currentSelectedSku, "quantite", quantity), 
                    SessionManager.getInstance().getSession().getToken()));
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue().isSucces()) {
                showToast("Produit ajouté !");
                updateCartBadge();
            }
        });
        new Thread(task).start();
    }

    private void updateCartBadge() {
        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated()) return null;
                return sendRequest(new Requete(RequestType.GET_CART, Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()), SessionManager.getInstance().getSession().getToken()));
            }
        };
        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> items = (List<?>) rep.getDonnees().get("lignes");
                int count = (items != null) ? items.size() : 0;
                Platform.runLater(() -> cartButton.setText("Panier (" + count + ")"));
            }
        });
        new Thread(task).start();
    }

    private Reponse sendRequest(Requete req) {
        try (Socket s = new Socket("127.0.0.1", 5555);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(req); out.flush();
            return (Reponse) in.readObject();
        } catch (Exception e) { return null; }
    }

    private void showToast(String msg) {
        // Simple implementation for demo
        System.out.println("TOAST: " + msg);
    }
}
