package ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.layout.Priority;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import client.utils.SessionManager;
import client.utils.SceneManager;
import service.ProduitDetailService;
import model.Avis;
import model.ProduitVarValeur;
import model.Variante;
import model.SKU;
import ui.utils.IconLibrary;

import java.net.URL;
import java.util.*;
import java.io.*;
import java.net.Socket;

public class ProductDetailController implements Initializable {

    private static int selectedProductId;
    private static final ObjectProperty<Integer> productIdProperty = new SimpleObjectProperty<>(0);

    public static void setSelectedProductId(int id) {
        selectedProductId = id;
        productIdProperty.set(id);
        System.out.println("[ProductDetailController] setSelectedProductId called with ID: " + id);
    }

    @FXML
    private StackPane rootStackPane;
    @FXML
    private VBox rootPane;
    @FXML
    private Label logoLabel;
    @FXML
    private Button cartButton;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label originalPriceLabel;
    @FXML
    private Label promoBadge;
    @FXML
    private HBox ratingBox;
    @FXML
    private Label reviewsLabel;
    @FXML
    private VBox variantsContainer;
    @FXML
    private TextField quantityField;
    @FXML
    private Label stockLabel;
    @FXML
    private Button addToCartButton;
    @FXML
    private ScrollPane productScrollPane;
    @FXML
    private VBox mainContent;
    @FXML
    private HBox thumbnailBar;
    @FXML
    private StackPane mainImageContainer;

    // Avis
    @FXML
    private VBox reviewsContainer;
    @FXML
    private ComboBox<String> evaluationComboBox;
    @FXML
    private TextArea avisContentField;
    @FXML
    private Label avisStatusLabel;
    @FXML
    private VBox avisFormContainer;
    @FXML
    private VBox loginRequiredContainer;

    private int quantity = 1;
    private Map<String, Object> produitData;
    private Map<String, Object> uiData;
    private Map<String, String> currentSelections = new HashMap<>();
    private Map<String, Object> currentSku;
    private List<Map<String, Object>> allSkus;
    private int currentImageIndex = 0;

    // [USER REQUEST] New fields for variants
    private List<model.ProduitVarValeur> productPVVs;
    private Map<Integer, String> variantIdToNameMap = new HashMap<>();
    private Map<String, Integer> valueToIdPvvMap = new HashMap<>(); // "Name_Value" -> idPVV
    private Map<String, ToggleButton> pvvButtonsMap = new HashMap<>(); // "idPVV" -> button

    private static final String CORAIL = "#FF724C";
    private static final String BLEU_NUIT = "#2A2C41";
    private static final String BLANC_CASSE = "#F4F4F8";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (logoLabel != null)
            logoLabel.setStyle("-fx-text-fill: #FDBF50; -fx-font-size: 20px; -fx-font-weight: bold;");

        if (evaluationComboBox != null) {
            evaluationComboBox.setItems(FXCollections.observableArrayList("5", "4", "3", "2", "1"));
            evaluationComboBox.getSelectionModel().selectFirst();
        }

        if (quantityField != null) {
            quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    quantityField.setText(newVal.replaceAll("[^\\d]", ""));
                } else if (!newVal.isEmpty()) {
                    try {
                        int q = Integer.parseInt(newVal);
                        int max = 0;
                        if (currentSku != null && currentSku.get("quantite") != null) {
                            max = ((Number) currentSku.get("quantite")).intValue();
                        }
                        if (q > max)
                            q = max;
                        if (q < 1)
                            q = 1;
                        quantity = q;
                        if (!String.valueOf(q).equals(newVal)) {
                            quantityField.setText(String.valueOf(q));
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        System.out.println("[ProductDetailController] initialize called with selectedProductId: " + selectedProductId);

        // Écouter les changements de productIdProperty
        productIdProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal != 0) {
                System.out.println("[ProductDetailController] Product ID changed to: " + newVal);
                selectedProductId = newVal;
                loadProductComplet();
            }
        });

        // Si l'ID est déjà disponible au démarrage, charger immédiatement
        if (selectedProductId != 0) {
            System.out.println("[ProductDetailController] Loading product immediately - ID: " + selectedProductId);
            loadProductComplet();
        }

        if (productScrollPane != null) {
            productScrollPane.setFitToHeight(false);
            productScrollPane.setFitToWidth(true);
        }

        updateCartBadge();
        loadReviews();
        updateAvisVisibility();
    }

    private void loadProductComplet() {
        System.out.println(
                "[ProductDetailController] loadProductComplet called with selectedProductId: " + selectedProductId);

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() {
                return ProduitDetailService.preparerDonneesPourUI(selectedProductId);
            }
        };
        task.setOnSucceeded(e -> {
            uiData = task.getValue();
            if (uiData != null) {
                Platform.runLater(() -> {
                    // Fix: Data mapping was incorrect. These keys are inside "produit" Map
                    produitData = (Map<String, Object>) uiData.get("produit");
                    currentSelections = (Map<String, String>) uiData.get("selections");
                    currentSku = (Map<String, Object>) uiData.get("skuActuel");
                    
                    // Extraire les Listes brutes depuis produitData si non présentes à la racine
                    if (uiData.get("skusBruts") == null && produitData != null) {
                        uiData.put("skusBruts", produitData.get("skusBruts"));
                    }
                    if (uiData.get("variantesOrganisees") == null && produitData != null) {
                        uiData.put("variantes", produitData.get("variantesOrganisees"));
                    }
                    
                    displayProductInfo();
                    loadExtraVariantData(); // Fetch PVVs and Variant Names
                    loadReviews();
                });
            }
        });
        task.setOnFailed(e -> {
            System.err.println("Erreur chargement produit complet: " + e.getSource().getException().getMessage());
        });
        new Thread(task).start();
    }

    private void displayProductInfo() {
        if (produitData == null)
            return;

        productNameLabel.setText((String) produitData.get("nomProduit"));

        // Fix: Use "skusBruts" from uiData (which we just patched) or produitData
        Object skusObj = uiData.get("skusBruts");
        if (skusObj == null && produitData != null) skusObj = produitData.get("skusBruts");
        
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<java.lang.String, java.lang.Object>> skus = 
            (java.util.List<java.util.Map<java.lang.String, java.lang.Object>>) skusObj;
        
        allSkus = skus != null ? skus : new java.util.ArrayList<>();

        // Récupérer les variantes organisées pour les sélecteurs
        Object varOrg = uiData.get("variantes");
        if (varOrg == null && produitData != null) varOrg = produitData.get("variantesOrganisees");
        
        @SuppressWarnings("unchecked")
        Map<String, List<String>> variantesOrganisees = (Map<String, List<String>>) varOrg;
        if (variantesOrganisees != null) {
            uiData.put("variantes", variantesOrganisees);
        }

        // Afficher la plage de prix si plusieurs SKU
        if (produitData.containsKey("prixMinimum") && produitData.containsKey("prixMaximum")) {
            double min = (java.lang.Double) produitData.get("prixMinimum");
            double max = (java.lang.Double) produitData.get("prixMaximum");
            if (min == max) {
                priceLabel.setText((int) min + " MAD");
                originalPriceLabel.setVisible(false);
            } else {
                priceLabel.setText((int) min + " - " + (int) max + " MAD");
                originalPriceLabel.setVisible(false);
            }
        }

        updateSkuDisplay();
        setupImageGallery();

        // Afficher le stock total
        if (produitData.containsKey("stockTotal")) {
            int stockTotal = (java.lang.Integer) produitData.get("stockTotal");
            if (stockTotal <= 0) {
                stockLabel.setText("Rupture de stock");
                stockLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            } else {
                stockLabel.setText("Stock total: " + stockTotal + " unités");
                stockLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
            }
        }

        // Initialiser la quantité par défaut
        quantity = 1;
        if (quantityField != null)
            quantityField.setText(String.valueOf(quantity));

        updateAvisVisibility();
    }

    private void setupImageGallery() {
        if (thumbnailBar == null || allSkus.isEmpty())
            return;

        thumbnailBar.getChildren().clear();

        // Créer des thumbnails pour chaque SKU qui a une image
        for (int i = 0; i < allSkus.size(); i++) {
            Map<String, Object> sku = allSkus.get(i);
            String imageUrl = (String) sku.get("image");

            if (imageUrl != null && !imageUrl.isBlank()) {
                StackPane thumbContainer = new StackPane();
                thumbContainer.setPrefSize(60, 60);
                thumbContainer.setStyle(
                        "-fx-background-color: white; -fx-border-color: #DDD; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");

                ImageView thumb = new ImageView();
                thumb.setFitWidth(50);
                thumb.setFitHeight(50);
                thumb.setPreserveRatio(true);

                // Charger l'image en arrière-plan
                final int index = i;
                new Thread(() -> {
                    try {
                        Image img = new Image(imageUrl, 50, 50, true, true, true);
                        Platform.runLater(() -> {
                            if (!img.isError()) {
                                thumb.setImage(img);
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[Gallery] Erreur chargement thumbnail: " + e.getMessage());
                    }
                }).start();

                // Gérer la sélection
                thumbContainer.setOnMouseClicked(e -> selectImage(index));

                // Style pour l'image sélectionnée
                if (i == currentImageIndex) {
                    thumbContainer.setStyle("-fx-background-color: " + CORAIL + "; -fx-border-color: " + CORAIL
                            + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");
                }

                thumbContainer.getChildren().add(thumb);
                thumbnailBar.getChildren().add(thumbContainer);
            }
        }

        // Afficher la première image par défaut
        if (!allSkus.isEmpty()) {
            selectImage(0);
        }
    }

    private void selectImage(int index) {
        if (index < 0 || index >= allSkus.size())
            return;

        currentImageIndex = index;
        Map<String, Object> selectedSkuFromGallery = allSkus.get(index);

        // Mettre à jour l'image principale
        String imageUrl = (String) selectedSkuFromGallery.get("image");
        if (imageUrl != null && !imageUrl.isBlank() && productImageView != null) {
            new Thread(() -> {
                try {
                    Image img = new Image(imageUrl, 400, 300, true, true, true);
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            productImageView.setImage(img);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[ProductDetail] Erreur chargement image principale: " + e.getMessage());
                }
            }).start();
        }

        // Mettre à jour les thumbnails pour montrer la sélection
        updateThumbnailSelection();

        // Si ce SKU est différent du SKU actuel des variantes, mettre à jour
        if (currentSku == null || !selectedSkuFromGallery.equals(currentSku)) {
            // Trouver les variantes correspondantes à ce SKU
            updateVariantesFromSku(selectedSkuFromGallery);
        }
    }

    private void updateThumbnailSelection() {
        if (thumbnailBar == null)
            return;

        for (int i = 0; i < thumbnailBar.getChildren().size(); i++) {
            StackPane container = (StackPane) thumbnailBar.getChildren().get(i);
            if (i == currentImageIndex) {
                container.setStyle("-fx-background-color: " + CORAIL + "; -fx-border-color: " + CORAIL
                        + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");
            } else {
                container.setStyle(
                        "-fx-background-color: white; -fx-border-color: #DDD; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;");
            }
        }
    }

    private void updateVariantesFromSku(Map<String, Object> sku) {
        // Cette méthode met à jour les sélecteurs de variantes pour correspondre au SKU
        // sélectionné via la galerie
        // Pour l'instant, nous allons simplement mettre à jour le SKU actuel et
        // l'affichage
        currentSku = sku;
        updateSkuDisplay();

        // TODO: Implémenter la logique pour extraire les valeurs de variantes du SKU et
        // mettre à jour les ComboBox
    }

    // [USER REQUEST] IMPLEMENTATION OF INTELLIGENT VARIANT SELECTION
    private void loadExtraVariantData() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getAccessToken() : "";
                
                // 1. Fetch PVVs
                Map<String, Object> pvvParams = new HashMap<>();
                pvvParams.put("idProduit", selectedProductId);
                Reponse pvvRep = client.ClientSocket.getInstance().envoyer(new Requete(RequestType.GET_PRODUCT_VARIANTS, pvvParams, token));
                
                // 2. Fetch Variant Names
                Reponse varRep = client.ClientSocket.getInstance().envoyer(new Requete(RequestType.GET_VARIANTES_BY_PRODUIT, pvvParams, token));
                
                if (pvvRep != null && pvvRep.isSucces() && varRep != null && varRep.isSucces()) {
                    @SuppressWarnings("unchecked")
                    List<ProduitVarValeur> pvvs = (List<ProduitVarValeur>) pvvRep.getDonnees().get("pvvs");
                    @SuppressWarnings("unchecked")
                    List<Variante> vars = (List<Variante>) varRep.getDonnees().get("variantes");
                    
                    productPVVs = pvvs;
                    variantIdToNameMap.clear();
                    for (Variante v : vars) variantIdToNameMap.put(v.getIdVariante(), v.getNom());
                    
                    valueToIdPvvMap.clear();
                    for (ProduitVarValeur pvv : pvvs) {
                        String name = variantIdToNameMap.get(pvv.getIdVariante());
                        if (name != null) valueToIdPvvMap.put(name + "_" + pvv.getValeur(), pvv.getIdPVV());
                    }
                }
                return null;
            }
        };
        
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setupVariantSelectors();
            refreshVariantButtonStates(); // Update gray-out initial state
            loadProductImage();
        }));
        new Thread(task).start();
    }

    private void setupVariantSelectors() {
        if (variantsContainer == null || productPVVs == null) return;

        variantsContainer.getChildren().clear();
        pvvButtonsMap.clear();

        // Group PVVs by variant name
        Map<String, List<model.ProduitVarValeur>> grouped = new LinkedHashMap<>();
        for (model.ProduitVarValeur pvv : productPVVs) {
            String name = variantIdToNameMap.get(pvv.getIdVariante());
            if (name != null) {
                grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(pvv);
            }
        }

        for (Map.Entry<String, List<model.ProduitVarValeur>> entry : grouped.entrySet()) {
            String nomVariante = entry.getKey();
            List<model.ProduitVarValeur> valores = entry.getValue();

            VBox box = new VBox(8);
            box.setPadding(new Insets(0, 0, 15, 0));
            Label l = new Label(nomVariante + " :");
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 14px;");

            FlowPane flow = new FlowPane(10, 10);
            ToggleGroup group = new ToggleGroup();

            for (model.ProduitVarValeur pvv : valores) {
                ToggleButton btn = new ToggleButton(pvv.getValeur());
                btn.setToggleGroup(group);
                btn.setUserData(pvv);
                pvvButtonsMap.put(String.valueOf(pvv.getIdPVV()), btn);

                // Initial Selection
                if (currentSelections.containsKey(nomVariante) && currentSelections.get(nomVariante).equals(pvv.getValeur())) {
                    btn.setSelected(true);
                }

                btn.setOnAction(e -> {
                    if (btn.isSelected()) {
                        currentSelections.put(nomVariante, pvv.getValeur());
                        
                        // Auto-correct invalid combinations
                        boolean isValid = false;
                        for (Map<String, Object> sku : allSkus) {
                            @SuppressWarnings("unchecked")
                            Map<String, String> skuVars = (Map<String, String>) sku.get("variantes");
                            if (skuVars == null) continue;
                            
                            boolean allMatch = true;
                            for (Map.Entry<String, String> sel : currentSelections.entrySet()) {
                                if (!sel.getValue().equals(skuVars.get(sel.getKey()))) {
                                    allMatch = false;
                                    break;
                                }
                            }
                            if (allMatch) {
                                isValid = true;
                                break;
                            }
                        }
                        
                        if (!isValid) {
                            // Find any valid SKU that matches the NEW selection and update other selections
                            for (Map<String, Object> sku : allSkus) {
                                @SuppressWarnings("unchecked")
                                Map<String, String> skuVars = (Map<String, String>) sku.get("variantes");
                                if (skuVars == null) continue;
                                
                                if (pvv.getValeur().equals(skuVars.get(nomVariante))) {
                                    for (Map.Entry<String, String> skuVar : skuVars.entrySet()) {
                                        currentSelections.put(skuVar.getKey(), skuVar.getValue());
                                        // Update the toggle buttons visually
                                        for (ToggleButton otherBtn : pvvButtonsMap.values()) {
                                            model.ProduitVarValeur otherPvv = (model.ProduitVarValeur) otherBtn.getUserData();
                                            String otherType = variantIdToNameMap.get(otherPvv.getIdVariante());
                                            if (otherType != null && otherType.equals(skuVar.getKey()) && otherPvv.getValeur().equals(skuVar.getValue())) {
                                                otherBtn.setSelected(true);
                                            }
                                        }
                                    }
                                    break; // Take the first matching SKU
                                }
                            }
                        }

                        refreshVariantButtonStates();
                        fetchSkuByCurrentSelections();
                    } else {
                        btn.setSelected(true); // Don't allow deselect
                    }
                });

                applyButtonStyles(btn, true);
                flow.getChildren().add(btn);
            }
            box.getChildren().addAll(l, flow);
            variantsContainer.getChildren().add(box);
        }
    }

    private void refreshVariantButtonStates() {
        if (allSkus == null || pvvButtonsMap.isEmpty()) return;

        for (ToggleButton btn : pvvButtonsMap.values()) {
            model.ProduitVarValeur pvv = (model.ProduitVarValeur) btn.getUserData();
            String type = variantIdToNameMap.get(pvv.getIdVariante());
            String val = pvv.getValeur();

            // Check if this option is possible with selections of OTHER variant types
            boolean possible = false;
            for (Map<String, Object> sku : allSkus) {
                @SuppressWarnings("unchecked")
                Map<String, String> skuVars = (Map<String, String>) sku.get("variantes");
                if (skuVars == null) continue;

                // Does this SKU have our target value?
                if (!val.equals(skuVars.get(type))) continue;

                // Does it match our OTHER selections?
                boolean matchesOthers = true;
                for (Map.Entry<String, String> sel : currentSelections.entrySet()) {
                    if (sel.getKey().equals(type)) continue;
                    if (!sel.getValue().equals(skuVars.get(sel.getKey()))) {
                        matchesOthers = false;
                        break;
                    }
                }
                if (matchesOthers) {
                    possible = true;
                    break;
                }
            }

            // Remove setDisable to allow clicking and switching
            applyButtonStyles(btn, possible);
        }
    }

    private void applyButtonStyles(ToggleButton btn, boolean possible) {
        boolean selected = btn.isSelected();

        if (!possible) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCC; -fx-border-color: #EEE; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 5 15; -fx-cursor: hand; -fx-strikethrough: true;");
        } else if (selected) {
            btn.setStyle("-fx-background-color: " + CORAIL + "; -fx-text-fill: white; -fx-border-color: " + CORAIL + "; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 5 15; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            btn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #DDD; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 5 15; -fx-cursor: hand;");
        }
    }

    private void fetchSkuByCurrentSelections() {
        if (currentSelections.size() < variantIdToNameMap.size()) return; // Wait for all selected

        List<Integer> pvvIds = new ArrayList<>();
        for (Map.Entry<String, String> sel : currentSelections.entrySet()) {
            Integer id = valueToIdPvvMap.get(sel.getKey() + "_" + sel.getValue());
            if (id != null) pvvIds.add(id);
        }

        Task<Map<String, Object>> task = new Task<>() {
            @Override protected Map<String, Object> call() throws Exception {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getAccessToken() : "";
                Map<String, Object> params = new HashMap<>();
                params.put("idProduit", selectedProductId);
                params.put("pvvIds", pvvIds);
                
                Reponse rep = client.ClientSocket.getInstance().envoyer(new Requete(RequestType.GET_SKU_BY_VARIANTS, params, token));
                if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                    Object skuObj = rep.getDonnees().get("sku");
                    if (skuObj != null) {
                        // The server returns a model.SKU object. Let's convert it to a Map for compatibility with existing code
                        SKU s = (SKU) skuObj;
                        Map<String, Object> m = new HashMap<>();
                        m.put("SKU", s.getSku());
                        m.put("prix", s.getPrix());
                        m.put("quantite", s.getQuantite());
                        m.put("image", s.getImage());
                        return m;
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Map<String, Object> sku = task.getValue();
            if (sku != null) {
                currentSku = sku;
                updateSkuDisplay();
                // Update Image if present
                String img = (String) sku.get("image");
                if (img != null && !img.isBlank()) {
                    new Thread(() -> {
                        try {
                            Image im = new Image(img, 400, 300, true, true, true);
                            Platform.runLater(() -> { if(!im.isError()) productImageView.setImage(im); });
                        } catch(Exception ignored) {}
                    }).start();
                }
                addToCartButton.setDisable(false);
                addToCartButton.setText("Ajouter au panier");
            } else {
                priceLabel.setText("Non disponible");
                stockLabel.setText("Combinaison non disponible");
                stockLabel.setStyle("-fx-text-fill: #EF4444;");
                addToCartButton.setDisable(true);
            }
        }));
        new Thread(task).start();
    }

    private String findImageForColor(String color) {
        if (allSkus == null)
            return null;
        for (Map<String, Object> sku : allSkus) {
            @SuppressWarnings("unchecked")
            Map<String, String> vars = (Map<String, String>) sku.get("variantes");
            if (vars != null && color.equals(vars.get("Couleur"))) {
                String img = (String) sku.get("image");
                if (img != null && !img.trim().isEmpty()) {
                    return img;
                }
            }
        }
        return null;
    }

    private void updateGallerySelection(Map<String, Object> selectedSku) {
        if (thumbnailBar == null || allSkus.isEmpty())
            return;

        // Trouver l'index du SKU sélectionné dans la galerie
        for (int i = 0; i < allSkus.size(); i++) {
            Map<String, Object> sku = allSkus.get(i);
            if (sku.equals(selectedSku)) {
                currentImageIndex = i;
                updateThumbnailSelection();
                break;
            }
        }
    }

    private void updateSkuDisplay() {
        if (currentSku == null)
            return;

        // Mettre à jour le prix
        if (currentSku.containsKey("prix")) {
            double prix = ((Number) currentSku.get("prix")).doubleValue();
            priceLabel.setText((int) prix + " MAD");
            originalPriceLabel.setVisible(false);
        }

        // Mettre à jour le stock
        if (currentSku.containsKey("quantite")) {
            int stock = (Integer) currentSku.get("quantite");

            // Si pas de stock
            if (stock <= 0) {
                stockLabel.setText("Rupture de stock");
                stockLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                addToCartButton.setText("Rupture de stock");
                addToCartButton.setStyle("-fx-background-color: #999; -fx-text-fill: white;");
                addToCartButton.setDisable(true);
                quantity = 0;
            } else {
                stockLabel.setText("En stock: " + stock + " unités");
                stockLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                addToCartButton.setText("Ajouter au panier");
                addToCartButton
                        .setStyle("-fx-background-color: " + CORAIL + "; -fx-text-fill: white; -fx-cursor: hand;");
                addToCartButton.setDisable(false);
                if (quantity == 0)
                    quantity = 1;
                // Si la quantite choisie dépasse le stock, la réduire
                if (quantity > stock)
                    quantity = stock;
            }
            if (quantityField != null && !quantityField.isFocused())
                quantityField.setText(String.valueOf(quantity));
        }
    }

    private void loadProductImage() {
        if (productImageView == null || currentSku == null)
            return;

        String imageUrl = (String) currentSku.get("image");
        if (imageUrl != null && !imageUrl.isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(imageUrl, 400, 300, true, true, true);
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            productImageView.setImage(img);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[ProductDetail] Erreur chargement image: " + e.getMessage());
                }
            }).start();
        }
    }

    @FXML private void handleBack() { SceneManager.back(); }
  @FXML private void handleCart() { 
        if (SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("panier.fxml", "Mon Panier - ChriOnline");
        } else {
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
        }
    }    
    @FXML
    private void incrementQty() {
        quantity++;
        if (quantityField != null)
            quantityField.setText(String.valueOf(quantity));
    }

    @FXML
    private void decrementQty() {
        if (quantity > 1) {
            quantity--;
            if (quantityField != null)
                quantityField.setText(String.valueOf(quantity));
        }
    }

    @FXML
    private void handleAddToCart() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            SessionManager.getInstance().setPendingRedirect("product-detail.fxml", "Détail Produit - ChriOnline");
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }

        if (currentSku == null) {
            showToast("Veuillez sélectionner toutes les options");
            return;
        }

        String skuCode = (String) currentSku.get("SKU");
        if (skuCode == null) {
            showToast("SKU non disponible");
            return;
        }

        final int currentUserId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        Task<shared.Reponse> task = new Task<>() {
            @Override
            protected shared.Reponse call() {
                Map<String, Object> params = new HashMap<>();
                params.put("idClient", currentUserId);
                params.put("sku", skuCode);
                params.put("quantite", quantity);
                
                return sendRequest(new shared.Requete(shared.RequestType.ADD_TO_CART, params, SessionManager.getInstance().getSession().getAccessToken()));
            }
        };
        task.setOnSucceeded(e -> {
            shared.Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                showToast("Produit ajouté au panier !");
                updateCartBadge();
            } else {
                showToast("Erreur lors de l'ajout au panier");
            }
        });
        task.setOnFailed(e -> {
            showToast("Erreur de connexion");
        });
        new Thread(task).start();
    }

    private void updateCartBadge() {
        Task<shared.Reponse> task = new Task<>() {
            @Override protected shared.Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated()) return null;
                int idClient = (SessionManager.getInstance().getCurrentUser() != null) ? SessionManager.getInstance().getCurrentUser().getIdUtilisateur() : -1;
                if (idClient == -1) return null;
                Map<String, Object> params = new HashMap<>();
                params.put("idClient", idClient);
                
                return sendRequest(new shared.Requete(shared.RequestType.GET_CART, params, SessionManager.getInstance().getSession().getAccessToken()));
            }
        };
        task.setOnSucceeded(e -> {
            shared.Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> items = (List<?>) rep.getDonnees().get("lignes");
                int count = (items != null) ? items.size() : 0;
                Platform.runLater(() -> cartButton.setText("Panier (" + count + ")"));
            }
        });
        new Thread(task).start();
    }

    private void loadReviews() {
        if (reviewsContainer == null)
            return;

        javafx.concurrent.Task<shared.Reponse> task = new javafx.concurrent.Task<>() {
            @Override
            protected shared.Reponse call() {
                java.util.Map<java.lang.String, java.lang.Object> params = new java.util.HashMap<>();
                params.put("idProduit", selectedProductId);
                return sendRequest(new shared.Requete(shared.RequestType.GET_AVIS_BY_PRODUIT, params, null));
            }
        };

        task.setOnSucceeded(e -> {
            shared.Reponse rep = task.getValue();
            javafx.application.Platform.runLater(() -> {
                reviewsContainer.getChildren().clear();
                if (rep != null && rep.isSucces() && rep.getDonnees() != null) {
                    List<?> rawList = (List<?>) rep.getDonnees().get("avis");
                    int count = (rawList != null) ? rawList.size() : 0;
                    reviewsLabel.setText("(" + count + " avis)");

                    if (rawList == null || rawList.isEmpty()) {
                        Label noAvis = new Label("Aucun avis pour ce produit pour le moment. Soyez le premier !");
                        noAvis.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                        reviewsContainer.getChildren().add(noAvis);
                    } else {
                        for (Object o : rawList) {
                            if (!(o instanceof Avis)) continue;
                            Avis avis = (Avis) o;
                            VBox avisBox = new VBox(8);
                            avisBox.setStyle("-fx-background-color: white; -fx-padding: 18; -fx-background-radius: 12px; -fx-border-color: #EEE; -fx-border-width: 1px;");

                            HBox header = new HBox(12);
                            header.setAlignment(Pos.CENTER_LEFT);
                            
                            Label nameLbl = new Label(avis.getNomClient() != null ? avis.getNomClient() : "Client ChriOnline");
                            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2A2C41; -fx-font-size: 14px;");

                            HBox stars = new HBox(2);
                            int eval = avis.getEvaluation() != null ? avis.getEvaluation() : 5;
                            for (int i = 0; i < 5; i++) {
                                if (i < eval)
                                    stars.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 14, "#FDBF50"));
                                else
                                    stars.getChildren().add(IconLibrary.getIcon(IconLibrary.STAR, 14, "#DDD"));
                            }
                            header.getChildren().addAll(nameLbl, stars);

                            Label contentLbl = new Label(avis.getContenu());
                            contentLbl.setWrapText(true);
                            contentLbl.setStyle("-fx-text-fill: #555; -fx-line-spacing: 2;");

                            avisBox.getChildren().addAll(header, contentLbl);
                            reviewsContainer.getChildren().add(avisBox);
                        }
                    }
                }
            });
        });

        task.setOnFailed(e -> {
            System.err.println("[ProductDetail] Erreur chargement avis: " + task.getException());
            Platform.runLater(() -> reviewsLabel.setText("(Erreur avis)"));
        });

        new Thread(task).start();
    }

    @FXML
    private void handleSubmitAvis() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            SessionManager.getInstance().setPendingRedirect("product-detail.fxml", "Détail Produit - ChriOnline");
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }

        String contenu = avisContentField.getText();
        if (contenu == null || contenu.trim().isEmpty()) {
            avisStatusLabel.setText("Veuillez écrire un commentaire.");
            avisStatusLabel.setStyle("-fx-text-fill: red;");
            avisStatusLabel.setVisible(true);
            return;
        }

        final int currentUserId = SessionManager.getInstance().getCurrentUser().getIdUtilisateur();
        final int finalEval = Integer.parseInt(evaluationComboBox.getValue());
        final boolean hasExistingReview = false; // TODO: Implémenter la vérification

        // Soumettre l'avis (nouveau ou modifié)
        javafx.concurrent.Task<shared.Reponse> submitTask = new javafx.concurrent.Task<>() {
            @Override
            protected shared.Reponse call() {
                java.util.Map<java.lang.String, java.lang.Object> params = new java.util.HashMap<>();
                params.put("idProduit", selectedProductId);
                params.put("idClient", currentUserId);
                params.put("contenu", contenu.trim());
                params.put("evaluation", finalEval);

                shared.RequestType requestType = hasExistingReview ? shared.RequestType.UPDATE_AVIS : shared.RequestType.ADD_AVIS;
                return sendRequest(new shared.Requete(requestType, params, SessionManager.getInstance().getSession().getAccessToken()));
            }
        };

        submitTask.setOnSucceeded(e2 -> {
            shared.Reponse rep = submitTask.getValue();
            if (rep != null && rep.isSucces()) {
                String successMsg = hasExistingReview ? "Votre avis a été mis à jour avec succès !" : "Merci pour votre avis !";
                avisContentField.clear();
                evaluationComboBox.setValue("5");
                avisStatusLabel.setText(successMsg);
                avisStatusLabel.setStyle("-fx-text-fill: green;");
                avisStatusLabel.setVisible(true);
                loadReviews(); // Refresh the reviews list
            } else {
                String errorMsg = hasExistingReview ? "Erreur lors de la mise à jour de votre avis" : "Erreur lors de l'ajout de votre avis";
                avisStatusLabel.setText(errorMsg + ": " + (rep != null ? rep.getMessage() : "Inconnue"));
                avisStatusLabel.setStyle("-fx-text-fill: red;");
                avisStatusLabel.setVisible(true);
            }
        });

        submitTask.setOnFailed(e2 -> {
            String errorMsg = hasExistingReview ? "Erreur de connexion lors de la mise à jour" : "Erreur de connexion lors de l'ajout";
            avisStatusLabel.setText(errorMsg);
            avisStatusLabel.setStyle("-fx-text-fill: red;");
            avisStatusLabel.setVisible(true);
        });

        new Thread(submitTask).start();
    }


    private void updateAvisVisibility() {
        boolean loggedIn = SessionManager.getInstance().isAuthenticated();
        if (avisFormContainer != null) {
            avisFormContainer.setVisible(loggedIn);
            avisFormContainer.setManaged(loggedIn);
        }
        if (loginRequiredContainer != null) {
            loginRequiredContainer.setVisible(!loggedIn);
            loginRequiredContainer.setManaged(!loggedIn);
        }
    }

    @FXML
    private void handleLoginRedirect() {
        SessionManager.getInstance().setPendingRedirect("product-detail.fxml", "Détail Produit - ChriOnline");
        SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
    }

    private shared.Reponse sendRequest(shared.Requete req) {
        return client.ClientSocket.getInstance().envoyer(req);
    }

    private void showToast(String msg) {
        System.out.println("TOAST: " + msg);

        javafx.application.Platform.runLater(() -> {
            if (rootStackPane == null && mainContent != null && mainContent.getScene() != null) {
                javafx.scene.Parent root = mainContent.getScene().getRoot();
                if (root instanceof javafx.scene.layout.StackPane) {
                    rootStackPane = (javafx.scene.layout.StackPane) root;
                }
            }

            if (rootStackPane != null) {
                javafx.scene.control.Label toast = new javafx.scene.control.Label(msg);
                toast.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white; -fx-padding: 10 20; " +
                               "-fx-background-radius: 50px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

                javafx.scene.layout.StackPane toastWrapper = new javafx.scene.layout.StackPane(toast);
                toastWrapper.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
                javafx.scene.layout.StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 50, 0));
                toastWrapper.setMouseTransparent(true);

                rootStackPane.getChildren().add(toastWrapper);

                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toastWrapper);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), toastWrapper);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setDelay(javafx.util.Duration.seconds(2));
                fadeOut.setOnFinished(e -> rootStackPane.getChildren().remove(toastWrapper));

                fadeIn.play();
                fadeOut.play();
            }
        });
    }
}
