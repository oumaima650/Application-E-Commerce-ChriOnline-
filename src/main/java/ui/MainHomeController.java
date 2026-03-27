package ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.effect.DropShadow;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import shared.Requete;
import shared.Reponse;
import shared.RequestType;
import model.Categorie;
import client.utils.SessionManager;
import client.utils.SceneManager;
import ui.utils.IconLibrary;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javafx.event.ActionEvent;
import javafx.animation.PauseTransition;
import java.util.Set;
import java.util.TreeSet;

public class MainHomeController implements Initializable {

    // Navbar Components
    @FXML
    private HBox navbar;
    @FXML
    private Label logoLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button cartButton;
    @FXML
    private Button loginButton;
    @FXML
    private HBox categoriesContainer;
    private List<VBox> categoryCards = new ArrayList<>();

    // Filter Bar Components (promoted to fields for sync)
    private MenuButton catsFilter;
    private Slider pSlider;
    private Label priceMaxLabel;
    @FXML
    private Label cartBadge;
    @FXML
    private StackPane userAvatarContainer;
    @FXML
    private Circle userAvatar;
    @FXML
    private Label userInitial;
    @FXML
    private StackPane rootStackPane;


    // Main Layout Components
    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private VBox productContent; // Scrollable container for ALL content
    @FXML
    private VBox filterBarContainer; // Placeholder for the filter bar

    // Hero Banner Components
    @FXML
    private StackPane heroBanner;
    @FXML
    private Label heroBadge;
    @FXML
    private Label heroTitle;
    @FXML
    private Label heroSubtitle;
    @FXML
    private Button heroButton;
    @FXML
    private ImageView heroBannerImage;

    // Section Components (Dynamic)
    @FXML
    private FlowPane productGrid;
    @FXML
    private Label sectionTitle;

    // Palette de couleurs
    private static final String CORAIL = "#FF724C";
    private static final String SAFRAN = "#FDBF50";
    private static final String BLANC_CASSE = "#F4F4F8";
    private static final String BLEU_NUIT = "#2A2C41";


    // Slider State
    private int currentBannerIndex = 0;
    private Timeline bannerTimeline;
    private List<Map<String, String>> bannersData;
    private HBox sliderDots;
    private Timeline countdownTimeline;

    // Data State
    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private ObservableList<Map<String, Object>> productsList = FXCollections.observableArrayList();
    // Banner image URLs (populated after products load)
    private final List<String> bannerImageUrls = new ArrayList<>();

    // Filter State
    private String selectedCategory = "Toutes les catégories";
    private boolean showOnlyInStock = false;
    private int maxPrice = 50000;
    private String currentSort = "Nouveautés ✨";

    /**
     * Enhanced Filter Bar: Respects the Produit/SKU model provided by the user,
     * allowing for real-time filtering of sellable SKUs by price, stock, and promo.
     */
    /**
     * Enhanced Filter Bar: Respects the Produit/SKU model provided by the user,
     * allowing for real-time filtering of sellable SKUs by price, stock, and promo.
     */
    private void setupFilterBar() {
        HBox bar = new HBox(25);
        bar.setPadding(new Insets(12, 30, 12, 30));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: white; -fx-background-radius: 18px; -fx-border-color: #EEE; -fx-border-width: 1;");

        // Add premium shadow
        DropShadow ds = new DropShadow(15, Color.rgb(0, 0, 0, 0.05));
        ds.setOffsetY(5);
        bar.setEffect(ds);

        Label l = new Label("Filtrage Avancé :");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 14px;");
        l.setGraphic(IconLibrary.getIcon(IconLibrary.SEARCH, 16, CORAIL));
        l.setGraphicTextGap(10);

        catsFilter = new MenuButton(selectedCategory);
        catsFilter.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 50px; -fx-text-fill: "
                + BLEU_NUIT + "; -fx-font-size: 12px; -fx-padding: 6 20; -fx-border-color: #EEE; -fx-border-radius: 50px; -fx-cursor: hand;");

        // [USER REQUEST] REMOVED categories dropdown from bar for better UX
        // bar.getChildren().add(catsFilter); 

        // Populate Categories dynamically
        Set<String> categories = new TreeSet<>();
        for (Map<String, Object> p : allProducts) {
            if (p.get("categorie") != null)
                categories.add((String) p.get("categorie"));
        }

        MenuItem allItem = new MenuItem("Toutes les catégories");
        allItem.setOnAction(e -> {
            selectedCategory = "Toutes les catégories";
            catsFilter.setText("Toutes les catégories");
            applyFilters();
        });
        catsFilter.getItems().add(allItem);

        for (String c : categories) {
            MenuItem mi = new MenuItem(c);
            mi.setOnAction(e -> {
                selectedCategory = c;
                catsFilter.setText(c);
                applyFilters();
            });
            catsFilter.getItems().add(mi);
        }

        HBox toggles = new HBox(12);
        toggles.setAlignment(Pos.CENTER_LEFT);

        CheckBox cbStock = new CheckBox("En stock");
        cbStock.setSelected(showOnlyInStock);
        cbStock.setGraphic(IconLibrary.getIcon(IconLibrary.PACKAGE, 14, BLEU_NUIT));
        cbStock.setGraphicTextGap(8);
        cbStock.setOnAction(e -> {
            showOnlyInStock = cbStock.isSelected();
            applyFilters();
        });

        String cbStyle = "-fx-background-color: #F8F9FA; -fx-background-radius: 50px; -fx-padding: 8 16; -fx-font-size: 11px; -fx-cursor: hand; -fx-text-fill: "
                + BLEU_NUIT + "; -fx-border-color: #EEE; -fx-border-radius: 50px;";
        cbStock.setStyle(cbStyle);
        toggles.getChildren().addAll(cbStock);

        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPrefHeight(20);
        sep.setMaxHeight(20);

        VBox pCont = new VBox(4);
        pCont.setAlignment(Pos.CENTER_LEFT);
        Label pLabel = new Label("Tranche de Prix (MAD)");
        pLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #AAA; -fx-font-weight: bold; -fx-text-transform: uppercase;");
        
        HBox pRow = new HBox(15);
        pRow.setAlignment(Pos.CENTER_LEFT);
        
        pSlider = new Slider(0, 50000, maxPrice);
        pSlider.setPrefWidth(160);
        pSlider.setStyle(
            ".slider .track { -fx-background-color: #EEE; -fx-pref-height: 4px; } " +
            ".slider .thumb { -fx-background-color: white; -fx-border-color: #DDD; -fx-pref-width: 16px; -fx-pref-height: 16px; }"
        );
        
        priceMaxLabel = new Label("Max " + (maxPrice / 1000) + "k");
        priceMaxLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + CORAIL + "; -fx-font-size: 13px;");

        // Debounce price slider
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxPrice = newVal.intValue();
            priceMaxLabel.setText("Max " + (maxPrice >= 1000 ? (maxPrice / 1000) + "k" : maxPrice));
            pause.playFromStart();
        });
        pause.setOnFinished(e -> applyFilters());

        pRow.getChildren().addAll(pSlider, priceMaxLabel);
        pCont.getChildren().addAll(pLabel, pRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("Nouveautés ✨", "Prix croissant 📈",
                "Prix décroissant 📉", "Mieux notés ⭐"));
        sort.setValue(currentSort);
        sort.setStyle("-fx-background-color: white; -fx-border-color: " + CORAIL
                + "; -fx-border-radius: 50px; -fx-background-radius: 50px; -fx-font-size: 11px; -fx-padding: 3 12; -fx-font-weight: bold; -fx-cursor: hand;");
        sort.setOnAction(e -> {
            currentSort = sort.getValue();
            applyFilters();
        });

        bar.getChildren().addAll(l, toggles, sep, pCont, spacer, sort);

        if (filterBarContainer != null) {
            filterBarContainer.getChildren().clear();
            VBox wrapper = new VBox(bar);
            wrapper.setId("filter-bar-wrapper");
            wrapper.setPadding(new Insets(10, 40, 10, 40));
            filterBarContainer.getChildren().add(wrapper);
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setupNavbar();
        setupFilterBar();
        setupHeroSlider();
        loadCategories();
        loadProducts();

        // Ensure vertical scrolling is natural
        if (mainScrollPane != null) {
            mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
            mainScrollPane.setFitToHeight(false);
            mainScrollPane.setFitToWidth(true);
        }
    }

    // ==========================================
    // 1. NAVBAR IMPLEMENTATION
    // ==========================================
    private void setupNavbar() {
        if (logoLabel != null) {
            logoLabel.setText("ChriOnline");
            logoLabel.setStyle("-fx-text-fill: " + SAFRAN
                    + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-letter-spacing: -0.5px;");
        }

        // Search bar custom style
        if (searchField != null) {
            searchField.setPromptText("Rechercher des produits...");
            searchField.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.6); -fx-padding: 8 16;");

            // Style the container (HBox) for better visibility
            if (searchField.getParent() instanceof HBox) {
                searchField.getParent().setStyle(
                        "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 50px; -fx-alignment: CENTER_LEFT; -fx-padding: 0 16;");
            }
        }

        // Load Cart and Notif counts
        updateBadges();

        // User Authentication State
        boolean loggedIn = SessionManager.getInstance().isAuthenticated();
        if (loginButton != null) {
            loginButton.setVisible(!loggedIn);
            loginButton.setManaged(!loggedIn);
        }
        if (userAvatarContainer != null) {
            userAvatarContainer.setVisible(loggedIn);
            userAvatarContainer.setManaged(loggedIn);
        }

        if (loggedIn) {
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
    private void updateBadges() {
        // GET_CART
        Task<Reponse> cartTask = new Task<>() {
            @Override
            protected shared.Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated())
                    return null;
                return sendToServer(new shared.Requete(shared.RequestType.GET_CART,
                        Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()),
                        SessionManager.getInstance().getSession().getAccessToken()));
            }
        };
        cartTask.setOnSucceeded(e -> {
            Reponse rep = cartTask.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> items = (List<?>) rep.getDonnees().get("lignes");
                int count = (items != null) ? items.size() : 0;
                Platform.runLater(() -> {
                    if (cartBadge != null) {
                        cartBadge.setText(String.valueOf(count));
                        cartBadge.setVisible(count > 0);
                        cartBadge.setStyle("-fx-background-color: " + CORAIL
                                + "; -fx-background-radius: 50px; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 1 5;");
                    }
                });
            }
        });
        new Thread(cartTask).start();

        // GET_NOTIFICATIONS (Badge on icons if relevant)
        Task<shared.Reponse> notifTask = new Task<>() {
            @Override
            protected shared.Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated())
                    return null;
                return sendToServer(new shared.Requete(shared.RequestType.GET_NOTIFICATIONS,
                        Map.of("idUtilisateur", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()),
                        SessionManager.getInstance().getSession().getAccessToken()));
            }
        };
        notifTask.setOnSucceeded(e -> {
            Reponse rep = notifTask.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> notifs = (List<?>) rep.getDonnees().get("notifications");
                // For now just console log as badge might not exist in FXML yet
                System.out.println("Non-read notifications: " + (notifs != null ? notifs.size() : 0));
            }
        });
        new Thread(notifTask).start();
    }

    // ==========================================
    // 2. HERO SLIDER IMPLEMENTATION
    // ==========================================
    private void setupHeroSlider() {
        bannersData = new ArrayList<>();
        bannersData.add(Map.of("badge", "NOUVELLE COLLECTION", "title", "Smartphones\nHaut de Gamme", "subtitle",
                "Découvrez notre sélection des derniers modèles", "btn", "Découvrir →"));
        bannersData.add(Map.of("badge", "SOLDES FLASH", "title", "Accessoires\nIndispensables", "subtitle",
                "Jusqu'à -50% sur l'audio et le gaming", "btn", "Voir les offres"));
        bannersData.add(Map.of("badge", "OFFRE LIMITÉE", "title", "Flash Deals\nSamsung S24", "subtitle",
                "Fin de l'offre dans : 03:42:15", "btn", "Profiter vite"));

        // Add Navigation Dots below banner
        sliderDots = new HBox(8);
        sliderDots.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(sliderDots, Pos.BOTTOM_CENTER);
        StackPane.setMargin(sliderDots, new Insets(0, 0, 15, 0));

        for (int i = 0; i < bannersData.size(); i++) {
            Circle dot = new Circle(4, Color.web("white", 0.3));
            dot.setCursor(javafx.scene.Cursor.HAND);
            final int index = i;
            dot.setOnMouseClicked(e -> showBanner(index));
            sliderDots.getChildren().add(dot);
        }

        if (heroBanner != null) {
            heroBanner.getChildren().add(sliderDots);
            // Removed the inline style override so FXML's background color stays intact
        }

        // Automatic Sliding
        bannerTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            currentBannerIndex = (currentBannerIndex + 1) % bannersData.size();
            showBanner(currentBannerIndex);
        }));
        bannerTimeline.setCycleCount(Timeline.INDEFINITE);
        bannerTimeline.play();

        showBanner(0);
    }

    private void showBanner(int index) {
        currentBannerIndex = index;
        Map<String, String> data = bannersData.get(index);

        // Load Cloudinary image asynchronously if available
        if (heroBannerImage != null) {
            String url = bannerImageUrls.size() > index ? bannerImageUrls.get(index) : null;
            if (url != null && !url.isBlank()) {
                // Load in background thread to avoid blocking UI
                new Thread(() -> {
                    try {
                        Image img = new Image(url, 200, 200, true, true, true);
                        Platform.runLater(() -> {
                            if (!img.isError())
                                heroBannerImage.setImage(img);
                        });
                    } catch (Exception e) {
                        System.err.println("[Hero] Image load error: " + e.getMessage());
                    }
                }).start();
            } else {
                heroBannerImage.setImage(null);
            }
        }

        if (heroBadge != null)
            heroBadge.setText(data.get("badge"));
        if (heroTitle != null)
            heroTitle.setText(data.get("title"));
        if (heroSubtitle != null)
            heroSubtitle.setText(data.get("subtitle"));
        if (heroButton != null)
            heroButton.setText(data.get("btn"));

        // Update dots
        if (sliderDots != null) {
            for (int i = 0; i < sliderDots.getChildren().size(); i++) {
                Circle d = (Circle) sliderDots.getChildren().get(i);
                d.setFill(Color.web("white", i == index ? 1.0 : 0.3));
                d.setRadius(i == index ? 5 : 4);
            }
        }

        // 3rd Banner Countdown logic
        if (index == 2)
            startCountdown();
        else
            stopCountdown();
    }

    private void startCountdown() {
        stopCountdown();
        LocalDateTime end = LocalDateTime.now().plusHours(4).plusMinutes(32);
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long sec = LocalDateTime.now().until(end, ChronoUnit.SECONDS);
            if (sec <= 0) {
                heroSubtitle.setText("Offre terminée !");
                stopCountdown();
            } else {
                long h = sec / 3600;
                long m = (sec % 3600) / 60;
                long s = sec % 60;
                heroSubtitle.setText(String.format("Fin de l'offre dans : %02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null)
            countdownTimeline.stop();
    }

    // ==========================================
    // 3. CATEGORIES SECTION
    // ==========================================
    // 4. PRODUCTS & NEW SECTIONS
    // ==========================================
    private void loadProducts() {
        Task<shared.Reponse> pTask = new Task<>() {
            @Override protected shared.Reponse call() {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getAccessToken() : "";
                return sendToServer(new shared.Requete(shared.RequestType.GET_ALL_PRODUITS_AFFICHABLES, new HashMap<>(), token));
            }
        };
        pTask.setOnSucceeded(e -> {
            Reponse rep = pTask.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> prods = (List<?>) rep.getDonnees().get("produits");
                Platform.runLater(() -> processProducts(prods));
            } else {
                showError("Impossible de charger les produits", this::loadProducts);
            }
        });
        pTask.setOnFailed(e -> {
            Throwable ex = pTask.getException();
            System.err.println("[MainHome] Erreur critique chargement produits : " + (ex != null ? ex.getMessage() : "Inconnue"));
            if (ex != null) ex.printStackTrace();
            showError("Erreur réseau ou désérialisation", this::loadProducts);
        });
        new Thread(pTask).start();
    }

    private void processProducts(List<?> data) {
        allProducts.clear();
        productsList.clear();
        bannerImageUrls.clear();
        bannersData.clear(); // Clear existing hardcoded banners
        for (Object o : data) {
            model.ProduitAffichable p = (model.ProduitAffichable) o;
            Map<String, Object> m = new HashMap<>();
            m.put("nom", p.getNom());
            m.put("prix", p.getPrix().intValue());
            m.put("prixOriginal", p.getPrixOriginal().intValue());
            m.put("categorie", p.getCategorie());
            m.put("image", p.getImage());
            m.put("stock", p.getStock());
            m.put("id", p.getIdProduit());
            m.put("note", p.getNoteMoyenne());
            m.put("avisCount", p.getNombreAvis());
            allProducts.add(m);
        }
        
        // [USER REQUEST] Dynamic price limit
        int absMax = 0;
        for (Map<String, Object> p : allProducts) {
            int price = (int) p.get("prix");
            if (price > absMax) absMax = price;
        }
        
        // Buffer of 10% or at least a minimum
        final int finalMax = Math.max(absMax + 1, 100); 
        
        Platform.runLater(() -> {
            if (pSlider != null) {
                pSlider.setMax(finalMax);
                // If it's first load or if current maxPrice is unrealisticly high, sync it
                if (maxPrice > finalMax || maxPrice == 50000) {
                    maxPrice = finalMax;
                    pSlider.setValue(finalMax);
                }
            }
            if (priceMaxLabel != null) {
                priceMaxLabel.setText("Max " + (maxPrice >= 1000 ? (maxPrice / 1000) + "k" : maxPrice));
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        List<Map<String, Object>> filtered = new ArrayList<>(allProducts);

        // 1. Category Filter
        if (selectedCategory != null && !selectedCategory.equals("Toutes les catégories")) {
            filtered.removeIf(p -> !selectedCategory.equals(p.get("categorie")));
        }

        // 2. Stock Filter
        if (showOnlyInStock) {
            filtered.removeIf(p -> (int) p.get("stock") <= 0);
        }

        // 4. Price Filter
        filtered.removeIf(p -> (int) p.get("prix") > maxPrice);

        // 5. Sorting
        if (currentSort != null) {
            switch (currentSort) {
                case "Prix croissant 📈":
                    filtered.sort((a, b) -> Integer.compare((int) a.get("prix"), (int) b.get("prix")));
                    break;
                case "Prix décroissant 📉":
                    filtered.sort((a, b) -> Integer.compare((int) b.get("prix"), (int) a.get("prix")));
                    break;
                case "Mieux notés ⭐":
                    // Fallback to ID for now if rating not in model
                    filtered.sort((a, b) -> Integer.compare((int) b.get("id"), (int) a.get("id")));
                    break;
                default: // Nouveautés - sorted by ID desc
                    filtered.sort((a, b) -> Integer.compare((int) b.get("id"), (int) a.get("id")));
                    break;
            }
        }

        refreshProductGrid(filtered);

        // Populate banner images and dynamic text from the first products with images
        bannerImageUrls.clear();
        bannersData.clear();
        for (Map<String, Object> p : allProducts) {
            String img = (String) p.get("image");
            if (img != null && !img.isBlank()) {
                bannerImageUrls.add(img);

                Map<String, String> banner = new HashMap<>();
                banner.put("badge", "OFFRE SPÉCIALE");
                banner.put("title", (String) p.get("nom"));
                banner.put("subtitle",
                        "Découvrez notre nouvelle collection " + (p.get("categorie") != null ? p.get("categorie") : ""));
                banner.put("btn", "Acheter maintenant →");
                bannersData.add(banner);

                if (bannerImageUrls.size() >= 3)
                    break; // Limit to 3 banners
            }
        }

        // Pad fallbacks if needed
        if (bannersData.isEmpty()) {
            bannersData.add(Map.of("badge", "NOUVELLE COLLECTION", "title", "Produits\nTendance", "subtitle",
                    "Découvrez notre sélection des derniers modèles", "btn", "Découvrir →"));
        }
        while (bannerImageUrls.size() < bannersData.size())
            bannerImageUrls.add(null);

        showBanner(currentBannerIndex);
        setupFooter();
    }

    private void refreshProductGrid(List<Map<String, Object>> filtered) {
        productsList.setAll(filtered);
        setupTopProductsGrid();
        setupMeilleuresVentes();
    }

    private void setupTopProductsGrid() {
        if (productGrid == null)
            return;
        productGrid.getChildren().clear();

        if (productsList.isEmpty()) {
            Label placeholder = new Label("Aucun produit ne correspond à votre recherche ou catégorie.");
            placeholder.setStyle("-fx-text-fill: #aaa; -fx-padding: 40; -fx-font-style: italic;");
            productGrid.getChildren().add(placeholder);
            return;
        }

        // Ajout direct au FlowPane au lieu d'imbriquer un ScrollPane inutile
        // (car tout le produitContent est déjà dans un ScrollPane au niveau FXML)
        for (int i = 0; i < Math.min(8, productsList.size()); i++) {
            productGrid.getChildren().add(createProductCard(productsList.get(i)));
        }
    }

    private void setupMeilleuresVentes() {
        if (productContent == null) return;
        
        // Supprimer l'ancienne section si elle existe
        productContent.getChildren().removeIf(n -> n.getId() != null && n.getId().equals("best-sellers-section"));
        
        if (productsList.size() < 4) return; // Pas assez de produits pour cette section

        VBox sec = new VBox(15);
        sec.setId("best-sellers-section");
        sec.setPadding(new Insets(20, 0, 0, 0));
        
        Label h = new Label("Meilleures Ventes");
        h.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + ";");
        
        FlowPane grid = new FlowPane(15, 15);
        grid.setPrefWrapLength(750); // Ajuster selon le besoin
        
        for (int i = 4; i < Math.min(12, productsList.size()); i++) {
            grid.getChildren().add(createProductCard(productsList.get(i)));
        }
        
        sec.getChildren().addAll(h, grid);
        productContent.getChildren().add(sec);
    }


    private VBox createProductCard(Map<String, Object> p) {
        VBox card = new VBox(10);
        card.setPrefSize(180, 240);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #f0f0f0; -fx-border-width: 1px; -fx-border-radius: 16px;");

        // Image du produit depuis Cloudinary/URL SKU
        StackPane img = new StackPane();
        img.setPrefHeight(100);
        img.setStyle("-fx-background-color: " + BLANC_CASSE + "; -fx-background-radius: 12px;");

        // Charger l'image du SKU
        String imageUrl = (String) p.get("image");
        if (imageUrl != null && !imageUrl.isBlank()) {
            ImageView productImage = new ImageView();
            productImage.setFitWidth(160);
            productImage.setFitHeight(100);
            productImage.setPreserveRatio(true);
            productImage.setSmooth(true);

            // Charger l'image en arrière-plan
            new Thread(() -> {
                try {
                    Image imgObj = new Image(imageUrl, 160, 100, true, true, true);
                    Platform.runLater(() -> {
                        if (!imgObj.isError()) {
                            productImage.setImage(imgObj);
                            img.getChildren().clear();
                            img.getChildren().add(productImage);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[ProductCard] Erreur chargement image: " + e.getMessage());
                }
            }).start();
        } else {
            // Image par défaut si pas d'URL
            img.getChildren().add(IconLibrary.getIcon(IconLibrary.PACKAGE, 30, BLEU_NUIT));
        }

        int prix = (int) p.get("prix");
        int prixOrig = (int) p.get("prixOriginal");
        if (prix < prixOrig) {
            Label badge = new Label("- " + (int) (100 - (prix * 100.0 / prixOrig)) + "%");
            badge.setStyle("-fx-background-color: " + CORAIL
                    + "; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 5px;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            img.getChildren().add(badge);
        }

        Label nm = new Label((String) p.get("nom"));
        nm.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + BLEU_NUIT + ";");
        nm.setWrapText(true);

        HBox pBox = new HBox(8);
        Label sp = new Label(prix + " MAD");
        sp.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label op = new Label(prixOrig + "");
        op.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px; -fx-strikethrough: true;");
        pBox.getChildren().addAll(sp, op);

        HBox rBox = new HBox(4);
        rBox.setAlignment(Pos.CENTER_LEFT);
        
        double note = (p.get("note") != null) ? (double) p.get("note") : 0.0;
        int avis = (p.get("avisCount") != null) ? (int) p.get("avisCount") : 0;
        
        // Render 5 stars based on the note
        for (int i = 1; i <= 5; i++) {
            if (i <= Math.round(note)) {
                rBox.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 11, SAFRAN));
            } else {
                rBox.getChildren().add(IconLibrary.getIcon(IconLibrary.STAR, 11, "#DDD"));
            }
        }
        
        Label avisLabel = new Label("(" + avis + ")");
        avisLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");
        rBox.getChildren().add(avisLabel);

        Button btnAdd = new Button("Ajouter");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle("-fx-background-color: " + BLEU_NUIT
                + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 50px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            e.consume(); // Prevent card click
            if (!SessionManager.getInstance().isAuthenticated()) {
                SessionManager.getInstance().setPendingRedirect("main-home.fxml", "ChriOnline - Accueil");
                SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
                return;
            }

            Integer idObj = (Integer) p.get("id");
            if (idObj == null) {
                showToast("Produit indisponible");
                return;
            }

            Task<shared.Reponse> task = new Task<>() {
                @Override
                protected shared.Reponse call() {
                    Map<String, Object> reqP = new HashMap<>();
                    reqP.put("idProduit", idObj);
                    String token = SessionManager.getInstance().getSession().getAccessToken();
                    shared.Reponse skuRep = client.ClientSocket.getInstance()
                            .envoyer(new shared.Requete(shared.RequestType.GET_SKU_BY_PRODUIT, reqP, token));

                    if (skuRep != null && skuRep.isSucces() && skuRep.getDonnees() != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> skus = (List<Map<String, Object>>) skuRep.getDonnees().get("skus");
                        if (skus != null && !skus.isEmpty()) {
                            String firstSku = (String) skus.get(0).get("SKU");
                            Map<String, Object> addParams = new HashMap<>();
                            addParams.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
                            addParams.put("sku", firstSku);
                            addParams.put("quantite", 1);
                            return client.ClientSocket.getInstance()
                                    .envoyer(new shared.Requete(shared.RequestType.ADD_TO_CART, addParams, token));
                        }
                    }
                    return new shared.Reponse(false, "Produit sans SKU disponible.", null);
                }
            };

            task.setOnSucceeded(ev -> {
                Reponse rep = task.getValue();
                if (rep != null && rep.isSucces()) {
                    showToast("Article ajouté au panier !");
                    updateBadges();
                } else {
                    showToast(rep != null ? rep.getMessage() : "Serveur injoignable");
                }
            });
            new Thread(task).start();
        });

        card.getChildren().addAll(img, nm, pBox, rBox, btnAdd);

        card.setOnMouseClicked(e -> {
            Object objId = p.get("id");
            System.out.println("[MainHome] Card Clicked: " + p.get("nom") + " (ID: " + objId + ")");

            if (objId instanceof Number) {
                int id = ((Number) objId).intValue();
                // Navigate to product detail
                ProductDetailController.setSelectedProductId(id);
                SceneManager.switchTo("product-detail.fxml", (String) p.get("nom"));
            } else {
                System.err.println("[MainHome] ERROR: Product ID is not a Number for " + p.get("nom"));
            }
        });

        // Hover Scale Effect
        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        card.setOnMouseEntered(ev -> {
            card.setCursor(javafx.scene.Cursor.HAND);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
            card.setStyle(card.getStyle() + " -fx-border-color: " + CORAIL
                    + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        });
        card.setOnMouseExited(ev -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #f0f0f0; -fx-border-width: 1px; -fx-border-radius: 16px;");
        });

        return card;
    }

    private void setupFooter() {
        VBox f = new VBox(20);
        f.setPadding(new Insets(50, 40, 30, 40));
        f.setStyle("-fx-background-color: " + BLEU_NUIT + ";");
        f.setAlignment(Pos.CENTER);
        f.setId("footer-box");

        HBox cols = new HBox(60);
        cols.setAlignment(Pos.CENTER);

        VBox c1 = new VBox(10);
        Label h1 = new Label("Liens utiles");
        h1.setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-weight: bold;");
        c1.getChildren().addAll(h1, createFooterLabel("À propos"), createFooterLabel("FAQ"),
                createFooterLabel("Retours"));

        VBox c2 = new VBox(10);
        Label h2 = new Label("Social");
        h2.setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-weight: bold;");
        c2.getChildren().addAll(h2, createFooterLabel("📸 Instagram"), createFooterLabel("🐦 Twitter"),
                createFooterLabel("📘 Facebook"));

        VBox c3 = new VBox(10);
        Label h3 = new Label("Contact");
        h3.setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-weight: bold;");
        c3.getChildren().addAll(h3, createFooterLabel("📧 help@chrionline.ma"),
                createFooterLabel("📞 +212 5 22 00 00"));

        cols.getChildren().addAll(c1, c2, c3);

        Separator s = new Separator();
        s.setOpacity(0.1);
        s.setMaxWidth(800);

        Label copy = new Label("© 2026 ChriOnline. Tous droits réservés.");
        copy.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 10px;");

        f.getChildren().addAll(cols, s, copy);

        if (productContent != null) {
            // Remove previous footer and spacer if any
            productContent.getChildren().removeIf(
                    n -> n.getId() != null && (n.getId().equals("footer-box") || n.getId().equals("footer-spacer")));

            // Push footer to bottom
            Region spacer = new Region();
            spacer.setId("footer-spacer");
            VBox.setVgrow(spacer, Priority.ALWAYS);

            productContent.getChildren().addAll(spacer, f);
        }
    }

    private Label createFooterLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 12px; -fx-cursor: hand;");
        l.setOnMouseEntered(e -> l.setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-size: 12px; -fx-cursor: hand;"));
        l.setOnMouseExited(
                e -> l.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 12px; -fx-cursor: hand;"));
        return l;
    }

    private shared.Reponse sendToServer(shared.Requete req) {
        try {
            return client.ClientSocket.getInstance().envoyer(req);
        } catch (Exception e) {
            System.err.println("[MainHomeController] sendToServer error: " + e.getMessage());
            return null;
        }

    }

    private void showError(String msg, Runnable retry) {
        VBox b = new VBox(10);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(40));
        Label l = new Label("⚠️ " + msg);
        l.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold;");
        Button r = new Button("Réessayer");
        r.setOnAction(e -> {
            productContent.getChildren().remove(b);
            retry.run();
        });
        b.getChildren().addAll(l, r);
        productContent.getChildren().add(0, b); // Add at top of scrollable area
    }

    private void showToast(String msg) {
        Label t = new Label(msg);
        t.setStyle("-fx-background-color: " + BLEU_NUIT
                + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 50px;");
        StackPane p = new StackPane(t);
        p.setAlignment(Pos.BOTTOM_RIGHT);
        StackPane.setMargin(t, new Insets(20));
        p.setMouseTransparent(true); // Make the toast not block clicks
        if (productContent.getScene() != null) {
            StackPane root;
            if (productContent.getScene().getRoot() instanceof StackPane sp) {
                root = sp;
            } else {
                // Fallback: If root is not StackPane, we can't easily overlay but we won't crash
                System.out.println("[Toast] No StackPane root found: " + msg);
                return;
            }
            root.getChildren().add(p);
            new Timeline(new KeyFrame(Duration.seconds(3), ev -> root.getChildren().remove(p))).play();
        }
    }

    @FXML
    private void handleHeroAction() {
        System.out.println("Hero Action: Click");
    }

    @FXML
    private void handleCartClick() {
        SceneManager.switchTo("panier.fxml", "Mon Panier - ChriOnline");
    }

    @FXML
    private void handleCreateAccountClick() {
        SceneManager.switchTo("login.fxml", "Créer un compte - ChriOnline");
    }
    
    @FXML
    private void handleUserClick() {
        if (userAvatar == null)
            return;

        ContextMenu userMenu = new ContextMenu();
        MenuItem miCompte = new MenuItem("Mon compte");
        MenuItem miCommandes = new MenuItem("Mes commandes");
        MenuItem miDeconnexion = new MenuItem("Déconnexion");

        miCompte.setOnAction(e -> SceneManager.switchTo("profile.fxml", "Mon Profil - ChriOnline"));
        miCommandes.setOnAction(e -> SceneManager.switchTo("commandes.fxml", "Mes Commandes - ChriOnline"));

        miDeconnexion.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold;");
        miDeconnexion.setOnAction(e -> {
            SessionManager.getInstance().fermer();
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
        });

        userMenu.getItems().addAll(miCompte, miCommandes, new SeparatorMenuItem(), miDeconnexion);
        userMenu.show(userAvatar, javafx.geometry.Side.BOTTOM, 0, 0);
    }


    @FXML
    private void handleLoginClick() {
        SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
    }

    @FXML
    public void handleSearchAction(ActionEvent event) {
        if (searchField != null) {
            System.out.println("Search Action: " + searchField.getText());
        }
    }

    private void handleCategoryCardClick(javafx.scene.input.MouseEvent event) {
        // Actually we don't need this method anymore as we use setOnMouseClicked on dynamic cards
    }

    private void loadCategories() {
        Task<shared.Reponse> task = new Task<>() {
            @Override
            protected shared.Reponse call() {
                return sendToServer(new shared.Requete(shared.RequestType.GET_ALL_CATEGORIES, null, null));
            }
        };

        task.setOnSucceeded(e -> {
            Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                List<?> categories = (List<?>) rep.getDonnees().get("categories");
                Platform.runLater(() -> {
                    if (categoriesContainer != null) {
                        categoriesContainer.getChildren().clear();
                        categoryCards.clear();

                        // 1. All Products Card
                        VBox allCard = createCategoryCard("Toutes les catégories", IconLibrary.CATEGORY);
                        categoriesContainer.getChildren().add(allCard);

                        // 2. DB Categories
                        if (categories != null) {
                            for (Object obj : categories) {
                                String name = null;
                                if (obj instanceof Map) {
                                    Map<String, Object> catMap = (Map<String, Object>) obj;
                                    name = (String) catMap.get("nom");
                                } else if (obj instanceof Categorie) {
                                    name = ((Categorie) obj).getNom();
                                }

                                if (name != null) {
                                    String icon = getIconForCategory(name);
                                    VBox card = createCategoryCard(name, icon);
                                    categoriesContainer.getChildren().add(card);
                                }
                            }
                        }

                        // No Promotions card
                        updateCategorySelectionVisuals();
                    }
                });
            }
        });
        new Thread(task).start();
    }

    private String getIconForCategory(String name) {
        if (name == null) return IconLibrary.CATEGORY;
        String n = name.toLowerCase();
        if (n.contains("vête") || n.contains("habit") || n.contains("mode") || n.contains("shirt")) return IconLibrary.SHIRT;
        if (n.contains("chauss") || n.contains("sneaker") || n.contains("boot") || n.contains("shoe")) return IconLibrary.SHOE;
        if (n.contains("maison") || n.contains("déco") || n.contains("mobilier") || n.contains("home")) return IconLibrary.SOFA;
        if (n.contains("phone") || n.contains("smart")) return IconLibrary.PHONE;
        if (n.contains("access")) return IconLibrary.HEADPHONE;
        if (n.contains("ordi") || n.contains("laptop") || n.contains("élec") || n.contains("info")) return IconLibrary.LAPTOP;
        if (n.contains("montre") || n.contains("watch")) return IconLibrary.WATCH;
        return IconLibrary.CATEGORY;
    }

    private VBox createCategoryCard(String name, String iconPath) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 80);
        card.setPadding(new Insets(12));
        card.setCursor(javafx.scene.Cursor.HAND);
        
        // Base Style
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #f0f0f0; -fx-border-width: 1.5px;");

        SVGPath icon = IconLibrary.getIcon(iconPath, 24, CORAIL);
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        card.getChildren().addAll(icon, label);

        card.setOnMouseClicked(ev -> {
            selectedCategory = name;
            
            // Sync with top filter bar
            if (catsFilter != null) catsFilter.setText(selectedCategory);

            updateCategorySelectionVisuals();
            applyFilters();
        });

        categoryCards.add(card);
        return card;
    }

    private void updateCategorySelectionVisuals() {
        for (VBox card : categoryCards) {
            Label label = (Label) card.getChildren().get(1);
            String cardName = label.getText();

            boolean isSelected = selectedCategory.equals(cardName);

            if (isSelected) {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: " + CORAIL + "; -fx-border-width: 2px;");
                card.setEffect(new DropShadow(15, Color.rgb(255, 114, 76, 0.25)));
            } else {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #f0f0f0; -fx-border-width: 1.5px;");
                card.setEffect(null);
            }
        }
    }
}