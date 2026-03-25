package ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import client.ClientSocket;
import shared.RequestType;
import shared.Requete;
import shared.Reponse;
import ui.utils.IconLibrary;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PanierController implements Initializable {

    @FXML private Label badgeArticles;
    @FXML private VBox panierItemsBox;
    @FXML private Label lblSousTotal;
    @FXML private Label lblTotalTTC;
    @FXML private Button btnValider;
    
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Double> unitPrices = new HashMap<>();
    private final Map<String, Integer> currentQuantities = new HashMap<>();
    private final Map<String, HBox> itemRows = new HashMap<>();

    // Pour l'exercice, on va dire que le client connecté est l'ID 4
    private final int ID_CLIENT = 7;
    private final String DEBUG_TOKEN = "DEBUG";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerPanier();
        
        if(btnValider != null) {
            btnValider.setOnAction(e -> System.out.println("Validation de la commande..."));
        }
    }

    private void chargerPanier() {
        Task<Reponse> fetchTask = new Task<>() {
            @Override
            protected Reponse call() throws Exception {
                Requete req = new Requete(RequestType.GET_CART, null, DEBUG_TOKEN);
                return ClientSocket.getInstance().envoyer(req);
            }
        };

        fetchTask.setOnSucceeded(e -> updateUIWithCartData(fetchTask.getValue()));
        dbExecutor.submit(fetchTask);
    }

    private void updateUIWithCartData(Reponse rep) {
        if(panierItemsBox != null) {
            panierItemsBox.getChildren().clear();
        }
        unitPrices.clear();
        currentQuantities.clear();
        itemRows.clear();
        
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
                    totalArticles += qty;
                    totalGlobal += sousTotal;
                    
                    boolean isHighlighted = (i == 0);
                    HBox row = createCartItemRow(sku, sku, "Ref: " + sku, qty, sousTotal, "cat-phone", icon, isHighlighted);
                    itemRows.put(sku, row);
                    panierItemsBox.getChildren().add(row);
                }
            }
            updateSummaryLabels(totalArticles, totalGlobal);
        }
    }
    
    private void updateSummaryLabels(int nbArticles, double total) {
        if(badgeArticles != null) badgeArticles.setText(nbArticles + " article" + (nbArticles > 1 ? "s" : ""));
        String fmt = String.format("%.2f MAD", total).replace(",", " ");
        if(lblSousTotal != null) lblSousTotal.setText(fmt);
        if(lblTotalTTC != null) lblTotalTTC.setText(fmt);
    }

    private void updateQuantite(String sku, int nextQty, Label lblQty, Label lblPrice) {
        if (nextQty < 0) return;
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
            p.put("sku", sku);
            p.put("quantite", nextQty);
            
            Requete req = new Requete(RequestType.UPDATE_QUANTITY_CART, p, DEBUG_TOKEN);
            Reponse res = ClientSocket.getInstance().envoyer(req);
            if (!res.isSucces()) {
                Platform.runLater(this::chargerPanier); // Revert on failure
            }
        });
    }

    private void recalculateTotalsFromLocalData() {
        int totalItems = currentQuantities.values().stream().mapToInt(Integer::intValue).sum();
        double totalSum = currentQuantities.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * unitPrices.getOrDefault(entry.getKey(), 0.0))
                .sum();
        updateSummaryLabels(totalItems, totalSum);
    }
    
    private void removeProduit(String sku) {
        // --- OPTIMISTIC UI REMOVE (Instant) ---
        HBox row = itemRows.get(sku);
        if (row != null && panierItemsBox != null) {
            panierItemsBox.getChildren().remove(row);
        }
        currentQuantities.remove(sku);
        itemRows.remove(sku);
        
        recalculateTotalsFromLocalData();

        // --- BACKGROUND SYNC (to Server) ---
        dbExecutor.submit(() -> {
            Map<String, Object> p = new HashMap<>();
            p.put("sku", sku);
            Requete req = new Requete(RequestType.REMOVE_FROM_CART, p, DEBUG_TOKEN);
            Reponse res = ClientSocket.getInstance().envoyer(req);
            if (!res.isSucces()) {
                Platform.runLater(this::chargerPanier); // Revert on failure
            }
        });
    }

    private HBox createCartItemRow(String sku, String name, String subText, int qty, double prix, String catClass, String svgPathStr, boolean highlighted) {
        HBox row = new HBox(14);
        row.getStyleClass().add(highlighted ? "cart-item-highlighted" : "cart-item");
        row.setAlignment(Pos.CENTER_LEFT);
        
        if (svgPathStr == null || svgPathStr.isEmpty()) 
            svgPathStr = IconLibrary.PHONE;

        StackPane thumb = new StackPane();
        thumb.getStyleClass().addAll("item-thumbnail", catClass);
        SVGPath icon = IconLibrary.getIcon(svgPathStr, 24, "#FFFFFF");
        icon.getStyleClass().add("item-icon");
        thumb.getChildren().add(icon);
        
        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        Label n = new Label(name); n.getStyleClass().add("item-name");
        Label s = new Label(subText); s.getStyleClass().add("item-sub");
        info.getChildren().addAll(n, s);
        
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        
        Label lp = new Label(String.format("%.2f MAD", prix).replace(",", " "));
        lp.getStyleClass().add("item-price"); lp.setMinWidth(90); lp.setAlignment(Pos.CENTER_RIGHT);

        Label lq = new Label(String.valueOf(qty));
        lq.getStyleClass().add("qty-label");

        HBox qBox = new HBox(10);
        qBox.getStyleClass().add("qty-box"); qBox.setAlignment(Pos.CENTER);
        Button bm = new Button("−"); bm.getStyleClass().add("qty-btn");
        bm.setOnAction(e -> updateQuantite(sku, currentQuantities.get(sku) - 1, lq, lp));
        Button bp = new Button("+"); bp.getStyleClass().add("qty-btn");
        bp.setOnAction(e -> updateQuantite(sku, currentQuantities.get(sku) + 1, lq, lp));
        qBox.getChildren().addAll(bm, lq, bp);
        
        Button bd = new Button(); bd.getStyleClass().add("delete-btn");
        SVGPath tr = IconLibrary.getIcon(IconLibrary.TRASH, 16, "#E74C3C");
        tr.getStyleClass().add("icon-trash"); bd.setGraphic(tr);
        bd.setOnAction(e -> removeProduit(sku));
        
        row.getChildren().addAll(thumb, info, sp, qBox, lp, bd);
        return row;
    }
}
