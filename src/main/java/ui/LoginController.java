package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Utilisateur;
import client.utils.SceneManager;
import client.utils.SessionManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;


public class LoginController implements Initializable {

    private static final int     PW_MIN       = 8;
    private static final int     PW_MAX       = 32;
    private static final Pattern HAS_UPPER    = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT    = Pattern.compile(".*[0-9].*");
    private static final Pattern HAS_SPECIAL  = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*");
    private static final Pattern EMAIL_REGEX  = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private StackPane   rootPane;
    @FXML private VBox        loginCard;
    @FXML private TabPane     mainTabPane;
    @FXML private Tab         loginTab;
    @FXML private Tab         registerTab;

    @FXML private VBox          loginFormBox;
    @FXML private HBox          loginEmailWrapper;
    @FXML private HBox          loginPasswordWrapper;
    @FXML private TextField     loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button        loginButton;
    @FXML private Label         loginErrorLabel;

    @FXML private VBox          registerFormBox;
    @FXML private HBox          registerNameWrapper;
    @FXML private HBox          registerFirstNameWrapper;
    @FXML private HBox          registerPhoneWrapper;
    @FXML private HBox          registerEmailWrapper;
    @FXML private HBox          registerPasswordWrapper;
    @FXML private HBox          registerConfirmWrapper;
    @FXML private TextField     registerNomField;        // Last name
    @FXML private TextField     registerPrenomField;     // First name
    @FXML private TextField     registerPhoneField;
    @FXML private TextField     registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Button        registerButton;
    @FXML private Label         registerErrorLabel;

    @FXML private HBox  strengthBarBox;
    @FXML private Label strengthLabel;
    @FXML private Hyperlink forgotPasswordLink;

    // --- reCAPTCHA ---
    @FXML private StackPane loginRecaptchaContainer;
    @FXML private StackPane registerRecaptchaContainer;
    private String loginRecaptchaToken = "";
    private String registerRecaptchaToken = "";
    private WebView loginWebView;
    private WebView registerWebView;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Assurer que le CSS spécifique à Login est toujours chargé, même en cache
        if (rootPane != null) {
            try {
                rootPane.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
                rootPane.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());
            } catch (Exception e) {
                System.err.println("[LoginController] Erreur de chargement du CSS : " + e.getMessage());
            }
        }

        setupTabSwitching();
        setupFocusEffects();
        setupLiveValidation();
        setupEnterNavigation();
        setupPasswordStrengthMeter();
        setupRecaptcha(loginRecaptchaContainer, true);
        setupRecaptcha(registerRecaptchaContainer, false);
        animateCardEntrance();
    }

    @FXML
    private void handleBackToHome() {
        SceneManager.switchTo("main-home.fxml", "Boutique - ChriOnline");
    }

    @FXML
    private void handleForgotPassword() {
        System.out.println("[LoginController] Mot de passe oublié cliqué.");
        // Pour l'instant, on se contente d'un log. Une future implémentation pourrait ouvrir une popup.
    }

    @FXML
    private void handleLogin() {
        String email    = trim(loginEmailField);
        String password = loginPasswordField.getText();

        if (email.isEmpty()) {
            showError(loginErrorLabel, "⚠ L'adresse e-mail est obligatoire.");
            shake(loginEmailWrapper);
            loginEmailField.requestFocus();
            return;
        }

        // --- reCAPTCHA Validation ---
        if (loginRecaptchaToken == null || loginRecaptchaToken.isEmpty()) {
            showError(loginErrorLabel, "⚠ Veuillez valider le reCAPTCHA.");
            return;
        }
        if (!EMAIL_REGEX.matcher(email).matches()) {
            showError(loginErrorLabel, "⚠ Adresse e-mail invalide.");
            shake(loginEmailWrapper);
            loginEmailField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError(loginErrorLabel, "⚠ Le mot de passe est obligatoire.");
            shake(loginPasswordWrapper);
            loginPasswordField.requestFocus();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("email",      email);
        params.put("motDePasse", password);
        params.put("recaptchaToken", loginRecaptchaToken);

        shared.Requete requete = new shared.Requete(shared.RequestType.LOGIN, params, null);

        setLoginLoading(true);
        runAsync(requete, reponse -> {
            setLoginLoading(false);
            try {
                if (reponse == null) {
                    showError(loginErrorLabel, "⚠ Impossible de joindre le serveur.");
                    return;
                }
                if (reponse.isSucces()) {
                    Map<String, Object> donnees = reponse.getDonnees();
                    if (donnees == null) {
                        System.err.println("[Login] Erreur : donnees est null dans la réponse.");
                        showError(loginErrorLabel, "⚠ Erreur de données serveur.");
                        return;
                    }

                    String accessToken  = (String) donnees.get("accessToken");
                    String refreshToken = (String) donnees.get("refreshToken");
                    Object userObj      = donnees.get("utilisateur");
                    String type         = (String) donnees.get("typeUtilisateur");

                    System.out.println("[Login] Données reçues : userObj type=" + (userObj != null ? userObj.getClass().getName() : "null"));

                    if (!(userObj instanceof Utilisateur user)) {
                        System.err.println("[Login] Erreur de cast : l'objet utilisateur n'est pas une instance de model.Utilisateur");
                        showError(loginErrorLabel, "⚠ Erreur de session client.");
                        return;
                    }

                    client.utils.SessionManager.getInstance().ouvrir(accessToken, refreshToken, user);
                    
                    // Enregistrer le port UDP pour les notifications
                    client.ClientApp.getInstance().registerUdpPort(accessToken);

                    System.out.println("[Login] Connecté — email=" + user.getEmail() + " role=" + type);
                    navigateToMain(type);
                } else {
                    showError(loginErrorLabel, "⚠ " + reponse.getMessage());
                    shake(loginEmailWrapper);
                    shake(loginPasswordWrapper);
                    loginPasswordField.clear();
                }
            } catch (Exception e) {
                System.err.println("[Login] Exception critique lors du traitement du login : " + e.getMessage());
                e.printStackTrace();
                showError(loginErrorLabel, "⚠ Erreur interne : " + e.getClass().getSimpleName());
            }
        });
    }

    @FXML
    private void handleRegister() {
        String nom      = trim(registerNomField);
        String prenom   = trim(registerPrenomField);
        String phone    = trim(registerPhoneField);
        String email    = trim(registerEmailField);
        String password = registerPasswordField.getText();
        String confirm  = registerConfirmPasswordField.getText();

        if (nom.isEmpty() || nom.length() < 2) {
            showError(registerErrorLabel, "⚠ Le nom doit contenir au moins 2 caractères.");
            shake(registerNameWrapper);
            return;
        }
        if (prenom.isEmpty() || prenom.length() < 2) {
            showError(registerErrorLabel, "⚠ Le prénom doit contenir au moins 2 caractères.");
            shake(registerFirstNameWrapper);
            return;
        }
        if (email.isEmpty() || !EMAIL_REGEX.matcher(email).matches()) {
            showError(registerErrorLabel, "⚠ Adresse e-mail invalide.");
            shake(registerEmailWrapper);
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^[+0-9\\s\\-]{7,15}$")) {
            showError(registerErrorLabel, "⚠ Numéro de téléphone invalide.");
            shake(registerPhoneWrapper);
            return;
        }

        // --- reCAPTCHA Validation ---
        if (registerRecaptchaToken == null || registerRecaptchaToken.isEmpty()) {
            showError(registerErrorLabel, "⚠ Veuillez valider le reCAPTCHA.");
            return;
        }

        String pwError = validatePassword(password);
        if (pwError != null) {
            showError(registerErrorLabel, pwError);
            shake(registerPasswordWrapper);
            registerPasswordField.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            showError(registerErrorLabel, "⚠ Les mots de passe ne correspondent pas.");
            shake(registerConfirmWrapper);
            registerConfirmPasswordField.clear();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("email",      email);
        params.put("motDePasse", password);
        params.put("nom",        nom);
        params.put("prenom",     prenom);
        params.put("telephone",  phone);
        params.put("recaptchaToken", registerRecaptchaToken);

        shared.Requete requete = new shared.Requete(shared.RequestType.REGISTER, params, null);

        setRegisterLoading(true);
        runAsync(requete, reponse -> {
            setRegisterLoading(false);
            if (reponse == null) {
                showError(registerErrorLabel, "⚠ Impossible de joindre le serveur.");
                return;
            }
            if (reponse.isSucces()) {
                showSuccess(registerErrorLabel,
                        "✓ Compte créé ! Connectez-vous.");
                loginEmailField.setText(email);
                delay(1600, () -> mainTabPane.getSelectionModel().select(loginTab));
            } else {
                showError(registerErrorLabel, "⚠ " + reponse.getMessage());
            }
        });
    }

    @FXML
    private void handleSwitchToRegister() {
        mainTabPane.getSelectionModel().select(registerTab);
    }


    private String validatePassword(String pw) {
        if (pw == null || pw.isEmpty())
            return "⚠ Le mot de passe est obligatoire.";
        if (pw.length() < PW_MIN)
            return "⚠ Mot de passe trop court (min " + PW_MIN + " caractères).";
        if (pw.length() > PW_MAX)
            return "⚠ Mot de passe trop long (max " + PW_MAX + " caractères).";
        if (!HAS_UPPER.matcher(pw).matches())
            return "⚠ Ajoutez au moins une lettre majuscule (A-Z).";
        if (!HAS_DIGIT.matcher(pw).matches())
            return "⚠ Ajoutez au moins un chiffre (0-9).";
        if (!HAS_SPECIAL.matcher(pw).matches())
            return "⚠ Ajoutez au moins un caractère spécial (!@#$%...).";
        return null; // ← valid
    }

    private int scorePassword(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= PW_MIN) score++;
        if (HAS_UPPER.matcher(pw).matches()) score++;
        if (HAS_DIGIT.matcher(pw).matches()) score++;
        if (HAS_SPECIAL.matcher(pw).matches()) score++;
        return score;
    }

    private void setupPasswordStrengthMeter() {
        if (registerPasswordField == null || strengthBarBox == null) return;

        registerPasswordField.textProperty().addListener((obs, old, val) -> {
            int score = scorePassword(val);
            String[] barColors = {"#e8e8ef", "#E74C3C", "#E67E22", "#FDBF50", "#27AE60"};
            
            javafx.collections.ObservableList<javafx.scene.Node> segs = strengthBarBox.getChildren();
            for (int i = 0; i < segs.size(); i++) {
                Node node = segs.get(i);
                if (node instanceof Region) {
                    Region seg = (Region) node;
                    String color = (i < score && score > 0) ? barColors[score] : "#e8e8ef";
                    seg.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
                }
            }

            if (strengthLabel != null) {
                String tresForLabel = "Tr" + "\u00e8" + "s fort";
                String[] labels = {"", "Faible", "Moyen", "Fort", tresForLabel};
                String[] txtColors = {"transparent", "#E74C3C", "#E67E22", "#c89600", "#27AE60"};
                strengthLabel.setText(val.isEmpty() ? "" : labels[score]);
                strengthLabel.setStyle("-fx-text-fill:" + txtColors[score] + ";-fx-font-weight:bold;");
            }
        });
    }


    /**
     * Runs the network call on a daemon thread, then calls the callback
     * on the JavaFX Application Thread when done.
     */
    private void runAsync(shared.Requete requete, java.util.function.Consumer<shared.Reponse> onDone) {
        Task<shared.Reponse> task = new Task<>() {
            @Override
            protected shared.Reponse call() {
                return client.ClientSocket.getInstance().envoyer(requete);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> onDone.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            System.err.println("[LoginController] Task failed: " + task.getException());
            onDone.accept(null);
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


        private void navigateToMain(String type) {
        // Vider l'historique pour ne pas revenir au Login avec le bouton "Retour"
        SceneManager.clearHistory();
        
        // --- IMPORTANT : Enregistrer le port UDP pour les notifications ---
        registerUdpPort(SessionManager.getInstance().getSession().getAccessToken());

        if ("ADMIN".equals(type)) {
            SceneManager.switchTo("admin.fxml", "ChriOnline - Administration");
        } else {
            // Vérifier si une redirection était prévue (ex: vers Checkout)
            String redirect = SessionManager.getInstance().getPendingRedirect();
            if (redirect != null && !redirect.isEmpty()) {
                String title = SessionManager.getInstance().getPendingRedirectTitle();
                SessionManager.getInstance().clearPendingRedirect();
                SceneManager.switchTo(redirect, title != null ? title : "ChriOnline");
            } else {
                SceneManager.clearCache("main-home.fxml"); // Forcer le rafraîchissement
                SceneManager.switchTo("panier.fxml", "ChriOnline - Panier");
            }
        }
    }

    /**
     * Enregistre le port UDP du client auprès du serveur pour activer les notifications
     */
    private void registerUdpPort(String token) {
        if (token == null) return;
        
        Task<Void> udpTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("udpPort", client.ClientApp.UDP_PORT);
                    
                    shared.Requete req = new shared.Requete(shared.RequestType.REGISTER_UDP_PORT, params, token);
                    shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                    
                    if (rep != null && rep.isSucces()) {
                        System.out.println("[LoginController] Port UDP " + client.ClientApp.UDP_PORT + " enregistré.");
                    }
                } catch (Exception e) {
                    System.err.println("[LoginController] Erreur UDP: " + e.getMessage());
                }
                return null;
            }
        };
        Thread t = new Thread(udpTask);
        t.setDaemon(true);
        t.start();
    }


    private void setupTabSwitching() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    clearErrors();
                    VBox form = (selected == loginTab) ? loginFormBox : registerFormBox;
                    if (form != null) slideIn(form);
                }
        );
    }

    private void setupFocusEffects() {
        bindFocus(loginEmailField,              loginEmailWrapper);
        bindFocus(loginPasswordField,           loginPasswordWrapper);
        bindFocus(registerNomField,             registerNameWrapper);
        bindFocus(registerPrenomField,          registerFirstNameWrapper);
        bindFocus(registerPhoneField,           registerPhoneWrapper);
        bindFocus(registerEmailField,           registerEmailWrapper);
        bindFocus(registerPasswordField,        registerPasswordWrapper);
        bindFocus(registerConfirmPasswordField, registerConfirmWrapper);
    }

    private void bindFocus(TextField field, HBox wrapper) {
        if (field == null || wrapper == null) return;
        field.focusedProperty().addListener((obs, was, is) -> {
            if (is) wrapper.getStyleClass().add("input-wrapper-focused");
            else    wrapper.getStyleClass().remove("input-wrapper-focused");
        });
    }

    private void setupLiveValidation() {
        for (TextField f : new TextField[]{loginEmailField, loginPasswordField}) {
            if (f != null) f.textProperty().addListener((o, old, n) -> loginErrorLabel.setText(""));
        }
        for (TextField f : new TextField[]{
                registerNomField, registerPrenomField, registerPhoneField,
                registerEmailField, registerPasswordField, registerConfirmPasswordField
        }) {
            if (f != null) f.textProperty().addListener((o, old, n) -> {
                registerErrorLabel.getStyleClass().removeAll("error-label-success");
                registerErrorLabel.getStyleClass().add("error-label");
                registerErrorLabel.setText("");
            });
        }
    }

    private void setupEnterNavigation() {
        // Login tab
        onEnter(loginEmailField,    () -> loginPasswordField.requestFocus());
        onEnter(loginPasswordField, this::handleLogin);

        // Register tab
        onEnter(registerNomField,             () -> registerPrenomField.requestFocus());
        onEnter(registerPrenomField,          () -> registerPhoneField.requestFocus());
        onEnter(registerPhoneField,           () -> registerEmailField.requestFocus());
        onEnter(registerEmailField,           () -> registerPasswordField.requestFocus());
        onEnter(registerPasswordField,        () -> registerConfirmPasswordField.requestFocus());
        onEnter(registerConfirmPasswordField, this::handleRegister);
    }

    private void onEnter(TextField field, Runnable action) {
        if (field != null) field.setOnAction(e -> action.run());
    }

    private void setLoginLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Connexion…" : "Se connecter");
        loginEmailField.setDisable(loading);
        loginPasswordField.setDisable(loading);
    }

    private void setRegisterLoading(boolean loading) {
        registerButton.setDisable(loading);
        registerButton.setText(loading ? "Création…" : "Créer un compte");
        registerNomField.setDisable(loading);
        registerPrenomField.setDisable(loading);
        registerPhoneField.setDisable(loading);
        registerEmailField.setDisable(loading);
        registerPasswordField.setDisable(loading);
        registerConfirmPasswordField.setDisable(loading);
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.getStyleClass().remove("error-label-success");
        if (!label.getStyleClass().contains("error-label"))
            label.getStyleClass().add("error-label");
        fadeIn(label);
    }

    private void showSuccess(Label label, String msg) {
        label.setText(msg);
        label.getStyleClass().remove("error-label");
        if (!label.getStyleClass().contains("error-label-success"))
            label.getStyleClass().add("error-label-success");
        fadeIn(label);
    }

    private void clearErrors() {
        if (loginErrorLabel    != null) loginErrorLabel.setText("");
        if (registerErrorLabel != null) registerErrorLabel.setText("");
    }


    private void animateCardEntrance() {
        if (loginCard == null) return;
        loginCard.setOpacity(0);
        loginCard.setTranslateY(28);

        FadeTransition ft = new FadeTransition(Duration.millis(480), loginCard);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(480), loginCard);
        tt.setFromY(28); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt).play();
    }

    private void slideIn(VBox form) {
        form.setOpacity(0);
        form.setTranslateY(10);

        FadeTransition ft = new FadeTransition(Duration.millis(300), form);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), form);
        tt.setFromY(10); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt).play();
    }

    /** Horizontal shake — called on invalid input wrappers. */
    private void shake(Region node) {
        if (node == null) return;
        TranslateTransition t = new TranslateTransition(Duration.millis(50), node);
        t.setFromX(0); t.setByX(8);
        t.setCycleCount(6);
        t.setAutoReverse(true);
        t.setOnFinished(e -> node.setTranslateX(0));
        t.play();
    }

    private void fadeIn(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void delay(int millis, Runnable action) {
        PauseTransition p = new PauseTransition(Duration.millis(millis));
        p.setOnFinished(e -> action.run());
        p.play();
    }

    private String trim(TextField f) {
        return (f == null || f.getText() == null) ? "" : f.getText().trim();
    }


    private void setupRecaptcha(StackPane container, boolean isLogin) {
        if (container == null) return;
        container.getChildren().clear();

        WebView webView = new WebView();
        if (isLogin) loginWebView = webView; else registerWebView = webView;

        // Force full size from the VERY beginning to spoof Google's viewport checks
        // Google will compute the layout bounds based on this 420x600 size
        webView.setPrefSize(420, 600);
        webView.setMinSize(420, 600);
        webView.setMaxSize(420, 600);

        // Clip the webview so it ONLY shows the 304x78 checkbox in the form
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(304, 78);
        webView.setClip(clip);

        // Wrap it in a strictly sized 304x78 Pane to maintain form layout harmony
        Pane formWrapper = new Pane(webView);
        formWrapper.setPrefSize(304, 78);
        formWrapper.setMinSize(304, 78);
        formWrapper.setMaxSize(304, 78);
        
        WebEngine engine = webView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        engine.setJavaScriptEnabled(true);

        engine.setOnAlert(event -> {
            String msg = event.getData();
            if (msg == null) return;
            
            if ("CHALLENGE_STARTED".equals(msg)) {
                Platform.runLater(() -> {
                    System.out.println("[reCAPTCHA] Challenge detected, opening modal...");
                    openRecaptchaModal(isLogin, webView);
                });
            } else if ("CHALLENGE_CLOSED".equals(msg)) {
                Platform.runLater(() -> {
                    System.out.println("[reCAPTCHA] Challenge closed or hidden.");
                    if (currentModalStage != null && currentModalStage.isShowing()) {
                        currentModalStage.close();
                    }
                });
            } else if (msg.startsWith("TOKEN:")) {
                String token = msg.substring(6);
                Platform.runLater(() -> {
                    if (isLogin) {
                        loginRecaptchaToken = token;
                        loginErrorLabel.setText("");
                    } else {
                        registerRecaptchaToken = token;
                        registerErrorLabel.setText("");
                    }
                    System.out.println("[reCAPTCHA] Token received. State: " + (isLogin ? "Login" : "Register"));
                    if (currentModalStage != null && currentModalStage.isShowing()) {
                        currentModalStage.close();
                    }
                });
            } else if ("EXPIRED".equals(msg) || "ERROR".equals(msg)) {
                Platform.runLater(() -> {
                    if (isLogin) loginRecaptchaToken = "";
                    else registerRecaptchaToken = "";
                    System.out.println("[reCAPTCHA] Token expired or error.");
                });
            }
        });

        String url = client.utils.RecaptchaLocalServer.getUrl();
        if (url != null) engine.load(url);
        else {
            URL resource = getClass().getResource("/html/recaptcha.html");
            if (resource != null) engine.load(resource.toExternalForm());
        }

        container.getChildren().add(formWrapper);
    }

    private javafx.stage.Stage currentModalStage;

    private void openRecaptchaModal(boolean isLogin, WebView webView) {
        if (currentModalStage != null && currentModalStage.isShowing()) return;

        // Ensure we handle the stage reference immediately on FX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> openRecaptchaModal(isLogin, webView));
            return;
        }

        javafx.stage.Stage modalStage = new javafx.stage.Stage();
        currentModalStage = modalStage;
        modalStage.setTitle("V\u00E9rification de S\u00E9curit\u00E9");
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            modalStage.initOwner(rootPane.getScene().getWindow());
        }
        modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // Save original parent and remove webView from it
        Pane originalParent = (Pane) webView.getParent();
        if (originalParent != null) {
            originalParent.getChildren().remove(webView);
        }
        // Remove the clip so the full 420x600 puzzle is visible in the modal
        webView.setClip(null);

            VBox layout = new VBox(webView);
            layout.setAlignment(javafx.geometry.Pos.CENTER);
            layout.setStyle("-fx-background-color: #ffffff;");
            
            javafx.scene.Scene scene = new javafx.scene.Scene(layout);
            modalStage.setScene(scene);
            
            modalStage.setOnHidden(e -> {
                // Restore to form when modal closes
                Platform.runLater(() -> {
                    if (webView.getParent() != originalParent) {
                        layout.getChildren().remove(webView);
                        // Re-apply the strict tight clip
                        webView.setClip(new javafx.scene.shape.Rectangle(304, 78));
                        if (originalParent != null && !originalParent.getChildren().contains(webView)) {
                            originalParent.getChildren().add(webView);
                        }
                    }
                    currentModalStage = null;
                });
            });

        modalStage.show();
        
        // Check if token was received while we were already opening/showing
        if (isLogin && !loginRecaptchaToken.isEmpty() || !isLogin && !registerRecaptchaToken.isEmpty()) {
            modalStage.close();
        }
    }
}