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
import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import client.ClientSocket;
import model.Utilisateur;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javax.net.ssl.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;


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
        animateCardEntrance();
        
        // Initialize reCAPTCHA
        setupRecaptcha(loginRecaptchaContainer, true);
        setupRecaptcha(registerRecaptchaContainer, false);
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

    private int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return -1;
        }
    }

    private void setupRecaptcha(StackPane container, boolean isLogin) {
        if (container == null) return;
        container.getChildren().clear();

        // Create a stylized button to trigger the modal
        Button verifyBtn = new Button("🛡\uFE0F Valider la s\u00E9curit\u00E9 (reCAPTCHA)");
        verifyBtn.setStyle("-fx-background-color: #f7f9fc; -fx-text-fill: #333333; -fx-border-color: #dcdde1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 18; -fx-cursor: hand;");
        verifyBtn.setOnAction(e -> openRecaptchaModal(isLogin, verifyBtn));
        container.getChildren().add(verifyBtn);
    }

    private void openRecaptchaModal(boolean isLogin, Button triggerBtn) {
        javafx.stage.Stage modalStage = new javafx.stage.Stage();
        modalStage.setTitle("V\u00E9rification de S\u00E9curit\u00E9");
        modalStage.initOwner(rootPane.getScene().getWindow());
        modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        WebView webView = new WebView();
        // Give it plenty of room for Google's image challenge popup (typically ~400x580)
        webView.setPrefSize(420, 600);
        webView.setMaxSize(420, 600);
        
        WebEngine engine = webView.getEngine();
        engine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
        );
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new RecaptchaBridge(isLogin, modalStage, triggerBtn));
                System.out.println("[reCAPTCHA] Modal loaded for " + (isLogin ? "login" : "register"));
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("[reCAPTCHA] Modal load FAILED: " + engine.getLoadWorker().getException());
            }
        });

        String recaptchaUrl = client.utils.RecaptchaLocalServer.getUrl();
        if (recaptchaUrl != null) {
            engine.load(recaptchaUrl);
        } else {
            URL resource = getClass().getResource("/html/recaptcha.html");
            if (resource != null) engine.load(resource.toExternalForm());
        }

        VBox layout = new VBox(webView);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");
        
        javafx.scene.Scene scene = new javafx.scene.Scene(layout);
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    /** Bridge class between JavaScript and Java for reCAPTCHA. */
    public class RecaptchaBridge {
        private final boolean isLogin;
        private final javafx.stage.Stage modalStage;
        private final Button triggerBtn;

        public RecaptchaBridge(boolean isLogin, javafx.stage.Stage modalStage, Button triggerBtn) {
            this.isLogin = isLogin;
            this.modalStage = modalStage;
            this.triggerBtn = triggerBtn;
        }

        public void onTokenReceived(String token) {
            Platform.runLater(() -> {
                if (isLogin) {
                    loginRecaptchaToken = token;
                    loginErrorLabel.setText("");
                } else {
                    registerRecaptchaToken = token;
                    registerErrorLabel.setText("");
                }
                
                // Update Button UI to show success
                triggerBtn.setText("\u2705 S\u00E9curit\u00E9 valid\u00E9e");
                triggerBtn.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-border-color: #c8e6c9; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 18;");
                triggerBtn.setDisable(true); // Don't let them click it again
                
                // Close the modal!
                if(modalStage != null && modalStage.isShowing()) {
                    modalStage.close();
                }
                
                System.out.println("[reCAPTCHA] Token received & Modal closed.");
            });
        }

        public void onTokenExpired() {
            Platform.runLater(() -> {
                if (isLogin) loginRecaptchaToken = "";
                else registerRecaptchaToken = "";
                // Reset button if token expires
                triggerBtn.setDisable(false);
                triggerBtn.setText("\u26A0\uFE0F Jeton expir\u00E9, revalider");
                triggerBtn.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-border-color: #ffe0b2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 18; -fx-cursor: hand;");
            });
        }

        public void onTokenError() {
            Platform.runLater(() -> {
                if (isLogin) loginRecaptchaToken = "";
                else registerRecaptchaToken = "";
                System.err.println("[reCAPTCHA] Error occurred");
            });
        }
    }
}