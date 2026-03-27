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
    private CheckBox cbPromoFilter;
    @FXML
    private Label cartBadge;
    @FXML
    private StackPane userAvatarContainer;
    @FXML
    private Circle userAvatar;
    @FXML
    private Label userInitial;


    // Main Layout Components
    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private VBox mainContent;

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
    @FXML
    private Button voirToutButton;

    // Palette de couleurs
    private static final String CORAIL = "#FF724C";
    private static final String SAFRAN = "#FDBF50";
    private static final String BLANC_CASSE = "#F4F4F8";
    private static final String BLEU_NUIT = "#2A2C41";

    // Server Config
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5555;

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
    private boolean showOnlyPromo = false;
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
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(15, 30, 15, 30));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20px; -fx-border-color: #F4F4F8; -fx-border-width: 1.5;");

        // Add premium shadow
        DropShadow ds = new DropShadow(20, Color.rgb(0, 0, 0, 0.08));
        ds.setOffsetY(8);
        bar.setEffect(ds);

        Label l = new Label("Filtrage Avancé :");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + "; -fx-font-size: 15px;");
        l.setGraphic(IconLibrary.getIcon(IconLibrary.SEARCH, 18, CORAIL));
        l.setGraphicTextGap(10);

        catsFilter = new MenuButton(selectedCategory);
        catsFilter.setStyle("-fx-background-color: " + BLANC_CASSE + "; -fx-background-radius: 50px; -fx-text-fill: "
                + BLEU_NUIT + "; -fx-font-size: 12px; -fx-padding: 6 18; -fx-cursor: hand;");

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
        CheckBox cbPromo = new CheckBox("En solde 🏷️");
        cbPromoFilter = cbPromo;
        cbPromo.setSelected(showOnlyPromo);
        cbPromo.setOnAction(e -> {
            showOnlyPromo = cbPromo.isSelected();
            applyFilters();
        });

        CheckBox cbStock = new CheckBox("En stock 📦");
        cbStock.setSelected(showOnlyInStock);
        cbStock.setOnAction(e -> {
            showOnlyInStock = cbStock.isSelected();
            applyFilters();
        });

        String cbStyle = "-fx-background-color: " + BLANC_CASSE
                + "; -fx-background-radius: 50px; -fx-padding: 8 15; -fx-font-size: 11px; -fx-cursor: hand; -fx-text-fill: "
                + BLEU_NUIT + ";";
        cbPromo.setStyle(cbStyle);
        cbStock.setStyle(cbStyle);
        toggles.getChildren().addAll(cbPromo, cbStock);

        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPrefHeight(25);

        VBox pCont = new VBox(2);
        Label pLabel = new Label("Tranche de Prix (MAD)");
        pLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-font-weight: bold; -fx-letter-spacing: 0.5;");
        HBox pRow = new HBox(12);
        pRow.setAlignment(Pos.CENTER_LEFT);
        Slider pSlider = new Slider(0, 50000, maxPrice);
        pSlider.setPrefWidth(140);
        Label v = new Label("Max " + (maxPrice / 1000) + "k");
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: " + CORAIL + "; -fx-font-size: 13px;");

        // Debounce price slider
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxPrice = newVal.intValue();
            v.setText("Max " + (maxPrice / 1000) + "k");
            pause.playFromStart();
        });
        pause.setOnFinished(e -> applyFilters());

        pRow.getChildren().addAll(pSlider, v);
        pCont.getChildren().addAll(pLabel, pRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("Nouveautés ✨", "Prix croissant 📈",
                "Prix décroissant 📉", "Mieux notés ⭐"));
        sort.setValue(currentSort);
        sort.setStyle("-fx-background-color: white; -fx-border-color: " + CORAIL
                + "; -fx-border-radius: 50px; -fx-background-radius: 50px; -fx-font-size: 11px; -fx-padding: 3 10; -fx-font-weight: bold;");
        sort.setOnAction(e -> {
            currentSort = sort.getValue();
            applyFilters();
        });

        bar.getChildren().addAll(l, catsFilter, toggles, sep, pCont, spacer, sort);

        if (mainContent != null) {
            // Remove previous filter if any to avoid duplicates
            mainContent.getChildren().removeIf(n -> n.getId() != null && n.getId().equals("filter-bar-wrapper"));

            VBox wrapper = new VBox(bar);
            wrapper.setId("filter-bar-wrapper");
            wrapper.setPadding(new Insets(10, 0, 30, 0));

            // Insert after Hero Banner if possible, otherwise at top
            int bannerIdx = mainContent.getChildren().indexOf(heroBanner);
            if (bannerIdx >= 0) {
                mainContent.getChildren().add(bannerIdx + 1, wrapper);
            } else {
                mainContent.getChildren().add(0, wrapper);
            }
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setupNavbar();
        setupHeroSlider();
        loadCategories();
        loadProducts();

        // Final UI tweaks
        if (mainScrollPane != null) {
            mainScrollPane.setStyle("-fx-background-color: " + BLANC_CASSE + "; -fx-border-width: 0;");
            mainScrollPane.setFitToHeight(true);
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

        // User Avatar and Name from Session
        if (SessionManager.getInstance().isAuthenticated()) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            if (email != null && !email.isEmpty()) {
                String name = email.split("@")[0].toUpperCase();
                if (userInitial != null) userInitial.setText(name.substring(0, 1));
            }
        } else {
            if (userInitial != null) userInitial.setText("G"); // G for Guest
        }

        if (userAvatar != null) {
            userAvatar.setFill(Color.web(SessionManager.getInstance().isAuthenticated() ? CORAIL : "#94a3b8"));
        }
    }

    private void updateBadges() {
        // GET_CART
        Task<Reponse> cartTask = new Task<>() {
            @Override protected Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated()) return null;
                int idClient = (SessionManager.getInstance().getCurrentUser() != null) ? SessionManager.getInstance().getCurrentUser().getIdUtilisateur() : -1;
                if (idClient == -1) return null;
                return sendToServer(new Requete(RequestType.GET_CART, Map.of("idClient", idClient), SessionManager.getInstance().getSession().getAccessToken()));
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
        Task<Reponse> notifTask = new Task<>() {
            @Override protected Reponse call() {
                if (!SessionManager.getInstance().isAuthenticated()) return null;
                return sendToServer(new Requete(RequestType.GET_NOTIFICATIONS, Map.of("idUtilisateur", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()), SessionManager.getInstance().getSession().getAccessToken()));
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
        Task<Reponse> pTask = new Task<>() {
            @Override protected Reponse call() {
                String token = SessionManager.getInstance().isAuthenticated() ? SessionManager.getInstance().getSession().getAccessToken() : "";
                return sendToServer(new Requete(RequestType.GET_ALL_PRODUITS_AFFICHABLES, new HashMap<>(), token));
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
            allProducts.add(m);
        }

        applyFilters();
    }

    private void applyFilters() {
        List<Map<String, Object>> filtered = new ArrayList<>(allProducts);

        // 1. Category Filter
        if (selectedCategory != null && !selectedCategory.equals("Toutes les catégories")) {
            filtered.removeIf(p -> !selectedCategory.equals(p.get("categorie")));
        }

        // 2. Promo Filter
        if (showOnlyPromo) {
            filtered.removeIf(p -> {
                int prix = (int) p.get("prix");
                int prixOrig = (int) p.get("prixOriginal");
                return prix >= prixOrig;
            });
        }

        // 3. Stock Filter
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

        HBox hBox = new HBox(15);
        hBox.setPadding(new Insets(10, 0, 10, 0));
        for (int i = 0; i < Math.min(6, productsList.size()); i++) {
            hBox.getChildren().add(createProductCard(productsList.get(i)));
        }

        ScrollPane sp = new ScrollPane(hBox);
        sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        productGrid.getChildren().add(sp);
    }

    private void setupMeilleuresVentes() {
        mainContent.getChildren().removeIf(n -> n.getId() != null && n.getId().contains("best-sellers-section"));
        VBox sec = new VBox(15);
        sec.setId("best-sellers-section");
        Label h = new Label("Meilleures Ventes");
        h.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + ";");
        FlowPane grid = new FlowPane(15, 15);
        for (int i = 0; i < Math.min(4, productsList.size()); i++) {
            grid.getChildren().add(createProductCard(productsList.get(i)));
        }
        sec.getChildren().addAll(h, grid);
        mainContent.getChildren().add(sec);
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

        HBox rBox = new HBox(2);
        for (int i = 0; i < 4; i++)
            rBox.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 10, SAFRAN));
        rBox.getChildren().add(IconLibrary.getIcon(IconLibrary.STAR, 10, SAFRAN));

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

            Task<Reponse> task = new Task<>() {
                @Override
                protected Reponse call() {
                    Map<String, Object> reqP = new HashMap<>();
                    reqP.put("idProduit", idObj);
                    String token = SessionManager.getInstance().getSession().getAccessToken();
                    Reponse skuRep = client.ClientSocket.getInstance()
                            .envoyer(new Requete(RequestType.GET_SKU_BY_PRODUIT, reqP, token));

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
                                    .envoyer(new Requete(RequestType.ADD_TO_CART, addParams, token));
                        }
                    }
                    return new Reponse(false, "Produit sans SKU disponible.", null);
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
                // Force cache clear for detail page to ensure fresh data
                SceneManager.clearCache("product-detail.fxml");
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

        if (mainContent != null) {
            // Remove previous footer and spacer if any
            mainContent.getChildren().removeIf(
                    n -> n.getId() != null && (n.getId().equals("footer-box") || n.getId().equals("footer-spacer")));

            // Push footer to bottom
            Region spacer = new Region();
            spacer.setId("footer-spacer");
            VBox.setVgrow(spacer, Priority.ALWAYS);

            mainContent.getChildren().addAll(spacer, f);
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

    private Reponse sendToServer(Requete req) {
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
            mainContent.getChildren().remove(b);
            retry.run();
        });
        b.getChildren().addAll(l, r);
        mainContent.getChildren().add(b);
    }

    private void showToast(String msg) {
        Label t = new Label(msg);
        t.setStyle("-fx-background-color: " + BLEU_NUIT
                + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 50px;");
        StackPane p = new StackPane(t);
        p.setAlignment(Pos.BOTTOM_RIGHT);
        StackPane.setMargin(t, new Insets(20));
        p.setMouseTransparent(true); // Make the toast not block clicks
        if (mainContent.getScene() != null) {
            StackPane root = (StackPane) mainContent.getScene().getRoot();
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
        if (SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("panier.fxml", "Mon Panier - ChriOnline");
        } else {
            // Mémoriser la redirection vers le panier après login
            SessionManager.getInstance().setPendingRedirect("panier.fxml", "Mon Panier - ChriOnline");
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
        }
    }

    @FXML
    private void handleCreateAccountClick() {
        SceneManager.switchTo("login.fxml", "Créer un compte - ChriOnline");
    }
    
    @FXML private void handleUserClick() { 
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
                SessionManager.getInstance().clearPendingRedirect(); // Nettoyer en cas de déco
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
            
            MenuItem miInscription = new MenuItem("Créer un compte");
            miInscription.setOnAction(e -> {
                SessionManager.getInstance().clearPendingRedirect();
                // Redirection vers inscription si elle existe, sinon login par défaut
                SceneManager.switchTo("login.fxml", "Inscription - ChriOnline");
            });
            
            userMenu.getItems().addAll(miConnexion, miInscription);
        }
        
        userMenu.show(userAvatar, javafx.geometry.Side.BOTTOM, 0, 0);
    }


    @FXML
    private void handleLoginClick() {
        // Optionnel: On peut aussi mettre le home en redirect par défaut si on veut
        SessionManager.getInstance().clearPendingRedirect(); 
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
        Task<Reponse> task = new Task<>() {
            @Override
            protected Reponse call() {
                return sendToServer(new Requete(RequestType.GET_ALL_CATEGORIES, null, null));
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

                        // 3. Promotions Card
                        VBox promoCard = createCategoryCard("Promotions", IconLibrary.TAG);
                        promoCard.setUserData("PROMO");
                        categoriesContainer.getChildren().add(promoCard);
                        
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
        if (n.contains("phone") || n.contains("smart")) return IconLibrary.PHONE;
        if (n.contains("access")) return IconLibrary.HEADPHONE;
        if (n.contains("ordi") || n.contains("laptop") || n.contains("élec") || n.contains("info")) return IconLibrary.LAPTOP;
        if (n.contains("montre") || n.contains("watch")) return IconLibrary.WATCH;
        if (n.contains("vête") || n.contains("mode") || n.contains("chauss") || n.contains("maison")) return IconLibrary.TAG;
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
            if ("PROMO".equals(card.getUserData())) {
                selectedCategory = "Toutes les catégories";
                showOnlyPromo = true;
            } else {
                selectedCategory = name;
                showOnlyPromo = false;
            }
            
            // Sync with top filter bar
            if (catsFilter != null) catsFilter.setText(selectedCategory);
            if (cbPromoFilter != null) cbPromoFilter.setSelected(showOnlyPromo);

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
            
            boolean isSelected = false;
            if ("PROMO".equals(card.getUserData())) {
                isSelected = showOnlyPromo;
            } else {
                isSelected = selectedCategory.equals(cardName) && !showOnlyPromo;
            }

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