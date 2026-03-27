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

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import client.utils.SessionManager;
import client.utils.SceneManager;
import service.ProduitDetailService;
import model.Avis;
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
    private Map<String, String> currentSelections;
    private Map<String, Object> currentSku;
    private List<Map<String, Object>> allSkus;
    private int currentImageIndex = 0;

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
                    setupVariantSelectors();
                    loadProductImage();
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

        // Load Avis
        loadReviews();

        // Stars (simulation)
        ratingBox.getChildren().clear();
        for (int i = 0; i < 5; i++) {
            ratingBox.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 14, "#FDBF50"));
        }
        reviewsLabel.setText("(0 avis)");
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
        // Ceci nécessiterait une requête supplémentaire ou une structure de données
        // différente
    }

    private void setupVariantSelectors() {
        if (variantsContainer == null || uiData == null)
            return;

        variantsContainer.getChildren().clear();

        @SuppressWarnings("unchecked")
        Map<String, List<String>> variantes = (Map<String, List<String>>) uiData.get("variantes");

        if (variantes == null || variantes.isEmpty()) {
            Label noVariants = new Label("Ce produit n'a pas de variantes");
            noVariants.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            variantsContainer.getChildren().add(noVariants);
            return;
        }

        for (Map.Entry<String, List<String>> entry : variantes.entrySet()) {
            String nomVariante = entry.getKey();
            List<String> valeurs = entry.getValue();

            VBox varianteBox = new VBox(8);
            varianteBox.setPadding(new Insets(0, 0, 15, 0));

            Label titre = new Label(nomVariante + ":");
            titre.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 14px;");

            FlowPane optionsPane = new FlowPane(10, 10);
            ToggleGroup group = new ToggleGroup();

            boolean hasVisibleOption = false;

            for (String val : valeurs) {
                // Vérifier si cette option est compatible avec les autres sélections
                if (!isVariantOptionCompatible(nomVariante, val)) {
                    continue; // Cacher si non compatible
                }

                hasVisibleOption = true;
                ToggleButton btn = new ToggleButton();
                btn.setToggleGroup(group);
                
                String imgUrl = nomVariante.equalsIgnoreCase("Couleur") ? findImageForColor(val) : null;
                
                if (imgUrl != null) {
                    try {
                        Image img = new Image(imgUrl, 40, 40, true, true, true);
                        ImageView iv = new ImageView(img);
                        iv.setClip(new javafx.scene.shape.Circle(20, 20, 20));
                        btn.setGraphic(iv);
                        btn.setTooltip(new Tooltip(val));
                        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #DDD; -fx-border-radius: 50%; -fx-cursor: hand; -fx-padding: 2;");
                    } catch (Exception e) {
                        btn.setText(val);
                        applyButtonStyle(btn, false);
                    }
                } else {
                    btn.setText(val);
                    applyButtonStyle(btn, false);
                }

                if (currentSelections != null && val.equals(currentSelections.get(nomVariante))) {
                    btn.setSelected(true);
                    applyButtonStyle(btn, true);
                }

                btn.setOnAction(e -> {
                    if (btn.isSelected()) {
                        mettreAJourVariante(nomVariante, val);
                    } else {
                        btn.setSelected(true);
                    }
                });

                optionsPane.getChildren().add(btn);
            }

            if (hasVisibleOption) {
                varianteBox.getChildren().addAll(titre, optionsPane);
                variantsContainer.getChildren().add(varianteBox);
            }
        }
    }

    private void applyButtonStyle(ToggleButton btn, boolean selected) {
        if (btn.getGraphic() != null) {
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + (selected ? CORAIL : "#DDD")
                    + "; -fx-border-width: " + (selected ? "2px" : "1px") + "; -fx-border-radius: 50%; -fx-cursor: hand; -fx-padding: 2;");
        } else {
            btn.setStyle("-fx-background-color: " + (selected ? CORAIL : "white")
                    + "; -fx-text-fill: " + (selected ? "white" : "black")
                    + "; -fx-border-color: " + (selected ? CORAIL : "#DDD")
                    + "; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 5 15; -fx-cursor: hand;");
        }
    }

    private boolean isVariantOptionCompatible(String targetType, String targetValue) {
        if (allSkus == null || allSkus.isEmpty()) return true;

        for (Map<String, Object> sku : allSkus) {
            @SuppressWarnings("unchecked")
            Map<String, String> skuVariants = (Map<String, String>) sku.get("variantes");
            if (skuVariants == null) continue;

            if (!targetValue.equals(skuVariants.get(targetType))) continue;

            boolean matchesOthers = true;
            for (Map.Entry<String, String> selection : currentSelections.entrySet()) {
                String type = selection.getKey();
                if (type.equals(targetType)) continue;
                if (!selection.getValue().equals(skuVariants.get(type))) {
                    matchesOthers = false;
                    break;
                }
            }
            if (matchesOthers) return true;
        }
        return false;
    }

    private void mettreAJourVariante(String nomVariante, String nouvelleValeur) {
        if (currentSelections == null)
            currentSelections = new HashMap<>();
        currentSelections.put(nomVariante, nouvelleValeur);

        // Ajuster les autres sélections si elles deviennent invalides
        ajusterSelectionsInvalides(nomVariante);

        // Refresh UI to hide incompatible options
        setupVariantSelectors();

        // Trouver le SKU correspondant aux sélections finales
        Map<String, Object> nouveauSku = trouverSkuParVariantes(currentSelections);

        if (nouveauSku != null) {
            currentSku = nouveauSku;
            updateSkuDisplay();

            String imageUrl = (String) nouveauSku.get("image");
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
                        System.err.println("[ProductDetail] Erreur chargement image: " + e.getMessage());
                    }
                }).start();
            }
            updateGallerySelection(nouveauSku);
        }
    }

    private void ajusterSelectionsInvalides(String skipType) {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> variantes = (Map<String, List<String>>) uiData.get("variantes");
        if (variantes == null) return;

        for (String type : variantes.keySet()) {
            if (type.equals(skipType)) continue;

            String currentVal = currentSelections.get(type);
            if (currentVal != null && !isVariantOptionCompatible(type, currentVal)) {
                // Trouver la première option compatible pour ce type
                for (String val : variantes.get(type)) {
                    if (isVariantOptionCompatible(type, val)) {
                        currentSelections.put(type, val);
                        break;
                    }
                }
            }
        }
    }

    private Map<String, Object> trouverSkuParVariantes(Map<String, String> selections) {
        if (uiData == null || selections == null)
            return null;
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> skusMap = (Map<String, Map<String, Object>>) uiData.get("skus");
        if (skusMap == null) {
            skusMap = (Map<String, Map<String, Object>>) uiData.get("skusOrganises");
        }
        if (skusMap != null) {
            return util.ProduitVariantUtils.trouverSkuPourVariantes(skusMap, selections);
        }
        return null;
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
