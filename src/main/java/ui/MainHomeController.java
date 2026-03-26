package ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

public class MainHomeController implements Initializable {

    // Navbar Components
    @FXML private HBox navbar;
    @FXML private Label logoLabel;
    @FXML private TextField searchField;
    @FXML private Button cartButton;
    @FXML private Label cartBadge;
    @FXML private StackPane userAvatarContainer;
    @FXML private Circle userAvatar;
    @FXML private Label userInitial;

    // Sidebar Components
    @FXML private VBox sidebar;
    @FXML private Button btnTous;
    @FXML private Button btnSmartphones;
    @FXML private Button btnAccessoires;
    @FXML private Button btnOrdinateurs;
    @FXML private Button btnMontres;
    @FXML private Button btnPromotions;

    // Main Layout Components
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox mainContent;

    // Hero Banner Components
    @FXML private StackPane heroBanner;
    @FXML private Label heroBadge;
    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;
    @FXML private Button heroButton;

    // Section Components (Dynamic)
    @FXML private FlowPane productGrid;
    @FXML private Label sectionTitle;
    @FXML private Button voirToutButton;

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
    private ObservableList<Map<String, Object>> productsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupNavbar();
        setupFilterBar();
        setupSidebar();
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
            logoLabel.setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-size: 18px; -fx-font-weight: bold; -fx-letter-spacing: -0.5px;");
        }

        // Search bar custom style
        if (searchField != null) {
            searchField.setPromptText("Rechercher des produits...");
            searchField.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 50px; " +
                    "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.6); -fx-padding: 8 16;");
        }

        // Load Cart and Notif counts
        updateBadges();

        // User Avatar and Name from Session
        String email = SessionManager.getInstance().getEmail();
        if (email != null && !email.isEmpty()) {
            String name = email.split("@")[0].toUpperCase();
            if (userInitial != null) userInitial.setText(name.substring(0, 1));
            // Show full name prefix as tooltip if possible (skip for now to stay in pure FX)
        }

        // Avatar ContextMenu
        if (userAvatar != null) {
            userAvatar.setFill(Color.web(CORAIL));
            ContextMenu userMenu = new ContextMenu();
            MenuItem miCompte = new MenuItem("Mon compte");
            MenuItem miCommandes = new MenuItem("Mes commandes");
            MenuItem miDeconnexion = new MenuItem("Déconnexion");
            
            miDeconnexion.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold;");
            miDeconnexion.setOnAction(e -> System.out.println("Logout..."));
            
            userMenu.getItems().addAll(miCompte, miCommandes, new SeparatorMenuItem(), miDeconnexion);
            
            // Interaction with the avatar parent container if possible, or circle directly
            userAvatar.getParent().setOnMouseClicked(e -> {
                userMenu.show(userAvatar, e.getScreenX(), e.getScreenY());
            });
        }
    }

    private void setupSidebar() {
        // Style ALL sidebar icons and buttons
        Button[] btns = {btnTous, btnSmartphones, btnAccessoires, btnOrdinateurs, btnMontres, btnPromotions};
        String[] icons = {IconLibrary.CATEGORY, IconLibrary.PHONE, IconLibrary.HEADPHONE, IconLibrary.LAPTOP, IconLibrary.WATCH, IconLibrary.TAG};

        for (int i = 0; i < btns.length; i++) {
            if (btns[i] != null) {
                btns[i].setGraphic(IconLibrary.getIcon(icons[i], 16, "rgba(244,244,248,0.4)"));
                btns[i].setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(244,244,248,0.4); " +
                        "-fx-padding: 10 15; -fx-background-radius: 10px; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
                
                final int idx = i;
                btns[i].setOnMouseEntered(e -> {
                    if (!btns[idx].getStyle().contains(CORAIL)) {
                        btns[idx].setStyle("-fx-background-color: rgba(255,114,76,0.1); -fx-text-fill: " + SAFRAN + "; " +
                                "-fx-padding: 10 15; -fx-background-radius: 10px; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
                    }
                });
                btns[i].setOnMouseExited(e -> {
                    if (!btns[idx].getStyle().contains(CORAIL)) {
                        btns[idx].setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(244,244,248,0.4); " +
                                "-fx-padding: 10 15; -fx-background-radius: 10px; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");
                    }
                });
            }
        }
        
        if (btnTous != null) {
            btnTous.setStyle("-fx-background-color: " + CORAIL + "; -fx-text-fill: white; " +
                    "-fx-padding: 10 15; -fx-background-radius: 10px; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT;");
        }
    }

    private void updateBadges() {
        // GET_CART
        Task<Reponse> cartTask = new Task<>() {
            @Override protected Reponse call() {
                return sendToServer(new Requete(RequestType.GET_CART, Map.of("idClient", SessionManager.getInstance().getUserId()), SessionManager.getInstance().getToken()));
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
                        cartBadge.setStyle("-fx-background-color: " + CORAIL + "; -fx-background-radius: 50px; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 1 5;");
                    }
                });
            }
        });
        new Thread(cartTask).start();

        // GET_NOTIFICATIONS (Badge on icons if relevant)
        Task<Reponse> notifTask = new Task<>() {
            @Override protected Reponse call() {
                return sendToServer(new Requete(RequestType.GET_NOTIFICATIONS, Map.of("idUtilisateur", SessionManager.getInstance().getUserId()), SessionManager.getInstance().getToken()));
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
        bannersData.add(Map.of("badge", "NOUVELLE COLLECTION", "title", "Smartphones\nHaut de Gamme", "subtitle", "Découvrez notre sélection des derniers modèles", "btn", "Découvrir →"));
        bannersData.add(Map.of("badge", "SOLDES FLASH", "title", "Accessoires\nIndispensables", "subtitle", "Jusqu'à -50% sur l'audio et le gaming", "btn", "Voir les offres"));
        bannersData.add(Map.of("badge", "OFFRE LIMITÉE", "title", "Flash Deals\nSamsung S24", "subtitle", "Fin de l'offre dans : 03:42:15", "btn", "Profiter vite"));

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
            heroBanner.setStyle("-fx-background-radius: 20px;");
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
        
        if (heroBadge != null) heroBadge.setText(data.get("badge"));
        if (heroTitle != null) heroTitle.setText(data.get("title"));
        if (heroSubtitle != null) heroSubtitle.setText(data.get("subtitle"));
        if (heroButton != null) heroButton.setText(data.get("btn"));

        // Update dots
        if (sliderDots != null) {
            for (int i = 0; i < sliderDots.getChildren().size(); i++) {
                Circle d = (Circle) sliderDots.getChildren().get(i);
                d.setFill(Color.web("white", i == index ? 1.0 : 0.3));
                d.setRadius(i == index ? 5 : 4);
            }
        }

        // 3rd Banner Countdown logic
        if (index == 2) startCountdown(); else stopCountdown();
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
                long h = sec / 3600; long m = (sec % 3600) / 60; long s = sec % 60;
                heroSubtitle.setText(String.format("Fin de l'offre dans : %02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void stopCountdown() { if (countdownTimeline != null) countdownTimeline.stop(); }

    // ==========================================
    // 3. CATEGORIES SECTION
    // ==========================================
    private void loadCategories() {
        Task<Reponse> catTask = new Task<>() {
            @Override protected Reponse call() {
                return sendToServer(new Requete(RequestType.GET_ALL_CATEGORIES, new HashMap<>(), SessionManager.getInstance().getToken()));
            }
        };
        catTask.setOnSucceeded(e -> {
            Reponse rep = catTask.getValue();
            if (rep != null && rep.isSucces()) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) rep.getDonnees().get("categories");
                Platform.runLater(() -> setupCategories(categories));
            } else {
                Platform.runLater(() -> System.err.println("Failed to load categories"));
            }
        });
        new Thread(catTask).start();
    }

    private void setupCategories(List<Map<String, Object>> categories) {
        if (mainContent == null) return;
        
        Label h = new Label("Explorer les Catégories");
        h.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + ";");
        h.setPadding(new Insets(20, 0, 10, 0));

        FlowPane grid = new FlowPane(20, 20);
        String[] gradients = {"#FF724C", "#FDBF50", "#2A2C41", "#3b82f6", "#10b981"};

        for (int i = 0; i < categories.size(); i++) {
            Map<String, Object> c = categories.get(i);
            String name = (String) c.get("nom");
            String color = gradients[i % gradients.length];
            grid.getChildren().add(createCategoryCard(name, (Integer) c.get("nombreProduits"), color));
        }

        // Insert after Hero Banner
        int bannerIdx = mainContent.getChildren().indexOf(heroBanner);
        mainContent.getChildren().add(bannerIdx + 1, new VBox(10, h, grid));
    }

    private VBox createCategoryCard(String name, Integer count, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setPrefSize(140, 140);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20px; -fx-cursor: hand;");
        
        String emoji = "📦";
        if (name.toLowerCase().contains("phone")) emoji = "📱";
        else if (name.toLowerCase().contains("audio")) emoji = "🎧";
        else if (name.toLowerCase().contains("ordinateur")) emoji = "💻";
        else if (name.toLowerCase().contains("montre")) emoji = "⌚";

        Label em = new Label(emoji); em.setStyle("-fx-font-size: 35px;");
        Label nm = new Label(name); nm.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label ct = new Label(count + " Produits"); ct.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 10px;");
        
        card.getChildren().addAll(em, nm, ct);

        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        card.setOnMouseEntered(e -> { st.setToX(1.05); st.setToY(1.05); st.play(); });
        card.setOnMouseExited(e -> { st.setToX(1.0); st.setToY(1.0); st.play(); });
        
        return card;
    }

    // ==========================================
    // 4. PRODUCTS & NEW SECTIONS
    // ==========================================
    private void loadProducts() {
        Task<Reponse> pTask = new Task<>() {
            @Override protected Reponse call() {
                return sendToServer(new Requete(RequestType.GET_ALL_PRODUITS_AFFICHABLES, new HashMap<>(), SessionManager.getInstance().getToken()));
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
        productsList.clear();
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
            productsList.add(m);
        }
        setupTopProductsGrid();
        setupMeilleuresVentes();
        setupPromotionsSection();
        setupFooter(); // Always at the end
    }

    private void setupTopProductsGrid() {
        if (productGrid == null) return;
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
        VBox sec = new VBox(15);
        Label h = new Label("Meilleures Ventes");
        h.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + BLEU_NUIT + ";");
        FlowPane grid = new FlowPane(15, 15);
        for (int i = 0; i < Math.min(4, productsList.size()); i++) {
            grid.getChildren().add(createProductCard(productsList.get(i)));
        }
        sec.getChildren().addAll(h, grid);
        mainContent.getChildren().add(sec);
    }

    private void setupPromotionsSection() {
        VBox promo = new VBox(20);
        promo.setPadding(new Insets(25));
        promo.setStyle("-fx-background-color: #FFF0EB; -fx-background-radius: 20px;");
        
        Label h = new Label("Promotions du Jour 🔥");
        h.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + CORAIL + ";");
        
        FlowPane grid = new FlowPane(15, 15);
        for (Map<String, Object> p : productsList) {
            if ((Integer) p.get("prix") < (Integer) p.get("prixOriginal")) {
                grid.getChildren().add(createProductCard(p));
            }
        }
        promo.getChildren().addAll(h, grid);
        mainContent.getChildren().add(promo);
    }

    private VBox createProductCard(Map<String, Object> p) {
        VBox card = new VBox(10);
        card.setPrefSize(180, 240);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; -fx-border-color: #f0f0f0; -fx-border-width: 1px; -fx-border-radius: 16px;");
        
        // Placeholder image/icon area
        StackPane img = new StackPane();
        img.setPrefHeight(100);
        img.setStyle("-fx-background-color: " + BLANC_CASSE + "; -fx-background-radius: 12px;");
        img.getChildren().add(IconLibrary.getIcon(IconLibrary.PACKAGE, 30, BLEU_NUIT));
        
        int prix = (int) p.get("prix");
        int prixOrig = (int) p.get("prixOriginal");
        if (prix < prixOrig) {
            Label badge = new Label("- " + (int)(100 - (prix*100.0/prixOrig)) + "%");
            badge.setStyle("-fx-background-color: " + CORAIL + "; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 5px;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            img.getChildren().add(badge);
        }

        Label nm = new Label((String) p.get("nom")); nm.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + BLEU_NUIT + ";");
        nm.setWrapText(true);
        
        HBox pBox = new HBox(8);
        Label sp = new Label(prix + " MAD"); sp.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label op = new Label(prixOrig + ""); op.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px; -fx-strikethrough: true;");
        pBox.getChildren().addAll(sp, op);
        
        HBox rBox = new HBox(2);
        for (int i=0; i<4; i++) rBox.getChildren().add(IconLibrary.getFilledIcon(IconLibrary.STAR, 10, SAFRAN));
        rBox.getChildren().add(IconLibrary.getIcon(IconLibrary.STAR, 10, SAFRAN));

        Button btnAdd = new Button("Ajouter");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 50px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> showToast("Article ajouté !"));

        card.getChildren().addAll(img, nm, rBox, pBox, btnAdd);
        return card;
    }

    // ==========================================
    // 5. FILTERS & FOOTER & HELPERS
    // ==========================================
    private void setupFilterBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: white; -fx-border-color: #EEE; -fx-border-width: 0 0 1 0;");
        
        Label l = new Label("Filtrer par :"); l.setStyle("-fx-font-weight: bold;");
        MenuButton cats = new MenuButton("Catégories");
        
        // Advanced Filters
        CheckBox cbPromo = new CheckBox("En Promotion");
        CheckBox cbStock = new CheckBox("En Stock");
        cbPromo.setStyle("-fx-font-size: 11px;");
        cbStock.setStyle("-fx-font-size: 11px;");
        
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        
        Slider pSlider = new Slider(0, 50000, 25000);
        pSlider.setPrefWidth(100);
        Label v = new Label("Max 25k");
        pSlider.valueProperty().addListener((obs, oldVal, newVal) -> v.setText("Max " + newVal.intValue() + "k"));
        
        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
        
        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("Le plus récent", "Prix croissant", "Prix décroissant", "Mieux notés"));
        sort.setValue("Trier par");
        sort.setStyle("-fx-background-radius: 50px; -fx-font-size: 11px;");

        bar.getChildren().addAll(l, cats, cbPromo, cbStock, sep, new Label("Budget :"), pSlider, v, r, sort);
        if (mainContent != null) mainContent.getChildren().add(0, bar);
    }

    private void setupFooter() {
        VBox f = new VBox(30);
        f.setPadding(new Insets(50, 40, 30, 40));
        f.setStyle("-fx-background-color: " + BLEU_NUIT + ";");
        f.setAlignment(Pos.CENTER);

        HBox cols = new HBox(60);
        cols.setAlignment(Pos.CENTER);
        
        VBox c1 = new VBox(10); c1.getChildren().addAll(new Label("Liens utiles"), new Label("À propos"), new Label("FAQ"), new Label("Retours"));
        VBox c2 = new VBox(10); c2.getChildren().addAll(new Label("Social"), new Label("📸 Instagram"), new Label("🐦 Twitter"), new Label("📘 Facebook"));
        VBox c3 = new VBox(10); c3.getChildren().addAll(new Label("Contact"), new Label("📧 help@chrionline.ma"), new Label("📞 +212 5 22 00 00"));
        
        for (VBox c : List.of(c1, c2, c3)) {
            ((Label)c.getChildren().get(0)).setStyle("-fx-text-fill: " + SAFRAN + "; -fx-font-weight: bold;");
            for (int i=1; i<c.getChildren().size(); i++) ((Label)c.getChildren().get(i)).setStyle("-fx-text-fill: #AAA; -fx-font-size: 12px;");
        }

        Label copy = new Label("© 2026 ChriOnline. Tous droits réservés.");
        copy.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 10px;");
        
        cols.getChildren().addAll(c1, c2, c3);
        f.getChildren().addAll(cols, new Separator(), copy);
        
        if (mainContent != null) {
            // Remove previous footer if any
            mainContent.getChildren().removeIf(n -> n instanceof VBox && "footer-box".equals(n.getId()));
            mainContent.getChildren().removeIf(n -> n instanceof Region && "footer-spacer".equals(n.getId()));

            // Push footer to bottom
            Region spacer = new Region();
            spacer.setId("footer-spacer");
            VBox.setVgrow(spacer, Priority.ALWAYS);
            
            f.setId("footer-box");
            mainContent.getChildren().addAll(spacer, f);
        }
    }

    private Reponse sendToServer(Requete req) {
        try (Socket s = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(req); out.flush();
            return (Reponse) in.readObject();
        } catch (Exception e) { return null; }
    }

    private void showError(String msg, Runnable retry) {
        VBox b = new VBox(10);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(40));
        Label l = new Label("⚠️ " + msg); l.setStyle("-fx-text-fill: " + CORAIL + "; -fx-font-weight: bold;");
        Button r = new Button("Réessayer");
        r.setOnAction(e -> { mainContent.getChildren().remove(b); retry.run(); });
        b.getChildren().addAll(l, r);
        mainContent.getChildren().add(b);
    }

    private void showToast(String msg) {
        Label t = new Label(msg);
        t.setStyle("-fx-background-color: " + BLEU_NUIT + "; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 50px;");
        StackPane p = new StackPane(t); p.setAlignment(Pos.BOTTOM_RIGHT); StackPane.setMargin(t, new Insets(20));
        if (mainContent.getScene() != null) {
            StackPane root = (StackPane) mainContent.getScene().getRoot();
            root.getChildren().add(p);
            new Timeline(new KeyFrame(Duration.seconds(3), ev -> root.getChildren().remove(p))).play();
        }
    }

    @FXML private void handleHeroAction() { System.out.println("Hero clicked"); }
    @FXML private void handleCartClick() { 
        System.out.println("Navigate to Cart...");
        SceneManager.switchTo("panier.fxml", "Mon Panier - ChriOnline");
    }
    @FXML private void handleUserClick() { System.out.println("User clicked"); }
    @FXML private void handleCategoryClick() { System.out.println("Category clicked"); }
    @FXML private void handleSearch() { System.out.println("Search: " + searchField.getText()); }
}