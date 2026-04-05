package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

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
    @FXML private TextField     registerNomField;        
    @FXML private TextField     registerPrenomField;     
    @FXML private TextField     registerPhoneField;
    @FXML private TextField     registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private Button        registerButton;
    @FXML private Label         registerErrorLabel;

    @FXML private HBox  strengthBarBox;
    @FXML private Label strengthLabel;
    @FXML private Hyperlink forgotPasswordLink;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
    }

    @FXML
    private void handleBackToHome() {
        SceneManager.switchTo("main-home.fxml", "Boutique - ChriOnline");
    }

    @FXML
    private void handleForgotPassword() {
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Réinitialisation - Étape 1");
        emailDialog.setHeaderText("Mot de passe oublié ?");
        emailDialog.setContentText("Entrez votre adresse e-mail :");
        emailDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        emailDialog.getDialogPane().getStyleClass().add("custom-dialog");

        Optional<String> emailResult = emailDialog.showAndWait();
        if (emailResult.isPresent()) {
            String email = emailResult.get().trim();
            if (email.isEmpty() || !EMAIL_REGEX.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer une adresse e-mail valide.");
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("email", email);
            Requete req = new Requete(RequestType.REQUEST_PASSWORD_RESET, params, null);
            setLoginLoading(true);
            runAsync(req, res -> {
                setLoginLoading(false);
                if (res != null && res.isSucces()) {
                    showResetConfirmationDialog(email);
                } else {
                    String msg = (res != null) ? res.getMessage() : "Serveur injoignable.";
                    showAlert(Alert.AlertType.ERROR, "Erreur", msg);
                }
            });
        }
    }

    private void showResetConfirmationDialog(String email) {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Réinitialisation - Étape 2");
        dialog.setHeaderText("Un code a été envoyé à : " + email);
        ButtonType confirmButtonType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("Code OTP (6 chiffres)");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer le mot de passe");

        grid.add(new Label("Code reçu :"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Nouveau mot de passe :"), 0, 1);
        grid.add(newPasswordField, 1, 1);

        VBox strengthContainer = new VBox(5);
        HBox dialogStrengthBar = new HBox(5);
        dialogStrengthBar.setPrefHeight(4);
        for (int i = 0; i < 4; i++) {
            Region seg = new Region();
            HBox.setHgrow(seg, Priority.ALWAYS);
            seg.setStyle("-fx-background-color: #e8e8ef; -fx-background-radius: 2;");
            dialogStrengthBar.getChildren().add(seg);
        }
        Label dialogStrengthText = new Label();
        dialogStrengthText.setStyle("-fx-font-size: 11px;");
        Label dialogHint = new Label("Min. 8 carac. (A-Z, 0-9, !@#...)");
        dialogHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #8892A4;");
        strengthContainer.getChildren().addAll(dialogStrengthBar, new HBox(10, dialogStrengthText, dialogHint));
        grid.add(strengthContainer, 1, 2);

        grid.add(new Label("Confirmer :"), 0, 3);
        grid.add(confirmField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);

        Runnable updateButton = () -> {
            boolean pwOk = validatePassword(newPasswordField.getText()) == null;
            boolean match = newPasswordField.getText().equals(confirmField.getText());
            boolean codeOk = !codeField.getText().trim().isEmpty();
            confirmButton.setDisable(!codeOk || !pwOk || !match);
        };

        codeField.textProperty().addListener((obs, old, val) -> updateButton.run());
        confirmField.textProperty().addListener((obs, old, val) -> updateButton.run());
        newPasswordField.textProperty().addListener((obs, old, val) -> {
            updateStrengthDisplay(val, dialogStrengthBar, dialogStrengthText);
            updateButton.run();
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                Map<String, String> results = new HashMap<>();
                results.put("code", codeField.getText().trim());
                results.put("newPassword", newPasswordField.getText());
                results.put("confirm", confirmField.getText());
                return results;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(results -> {
            String code = results.get("code");
            String newPass = results.get("newPassword");
            Map<String, Object> confirmParams = new HashMap<>();
            confirmParams.put("email", email);
            confirmParams.put("code", code);
            confirmParams.put("newPassword", newPass);

            Requete confirmReq = new Requete(RequestType.CONFIRM_PASSWORD_RESET, confirmParams, null);
            setLoginLoading(true);
            runAsync(confirmReq, res -> {
                setLoginLoading(false);
                if (res != null && res.isSucces()) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", res.getMessage());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", (res != null) ? res.getMessage() : "Échec.");
                }
            });
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleLogin() {
        String email = trim(loginEmailField);
        String password = loginPasswordField.getText();
        if (email.isEmpty()) { showError(loginErrorLabel, "⚠ Email requis."); shake(loginEmailWrapper); return; }
        if (!EMAIL_REGEX.matcher(email).matches()) { showError(loginErrorLabel, "⚠ Email invalide."); shake(loginEmailWrapper); return; }
        if (password.isEmpty()) { showError(loginErrorLabel, "⚠ Mot de passe requis."); shake(loginPasswordWrapper); return; }

        Map<String, Object> params = new HashMap<>();
        params.put("email", email); params.put("motDePasse", password);
        setLoginLoading(true);
        runAsync(new Requete(RequestType.LOGIN, params, null), reponse -> {
            setLoginLoading(false);
            if (reponse != null && reponse.isSucces()) {
                Map<String, Object> donnees = reponse.getDonnees();
                SessionManager.getInstance().ouvrir((String)donnees.get("accessToken"), (String)donnees.get("refreshToken"), (Utilisateur)donnees.get("utilisateur"));
                navigateToMain((String)donnees.get("typeUtilisateur"));
            } else {
                showError(loginErrorLabel, "⚠ " + (reponse != null ? reponse.getMessage() : "Erreur"));
                shake(loginEmailWrapper); shake(loginPasswordWrapper);
            }
        });
    }

    @FXML
    private void handleRegister() {
        String nom = trim(registerNomField);
        String prenom = trim(registerPrenomField);
        String phone = trim(registerPhoneField);
        String email = trim(registerEmailField);
        String password = registerPasswordField.getText();
        String confirm = registerConfirmPasswordField.getText();

        if (nom.isEmpty()) { showError(registerErrorLabel, "⚠ Nom requis."); shake(registerNameWrapper); return; }
        if (prenom.isEmpty()) { showError(registerErrorLabel, "⚠ Prénom requis."); shake(registerFirstNameWrapper); return; }
        if (!EMAIL_REGEX.matcher(email).matches()) { showError(registerErrorLabel, "⚠ Email invalide."); shake(registerEmailWrapper); return; }
        
        String pwError = validatePassword(password);
        if (pwError != null) { showError(registerErrorLabel, pwError); shake(registerPasswordWrapper); return; }
        if (!password.equals(confirm)) { showError(registerErrorLabel, "⚠ Confirmation différente."); shake(registerConfirmWrapper); return; }

        Map<String, Object> params = new HashMap<>();
        params.put("email", email); params.put("motDePasse", password);
        params.put("nom", nom); params.put("prenom", prenom); params.put("telephone", phone);

        setRegisterLoading(true);
        runAsync(new Requete(RequestType.REGISTER, params, null), reponse -> {
            setRegisterLoading(false);
            if (reponse != null && reponse.isSucces()) {
                showSuccess(registerErrorLabel, "✓ Compte créé ! Connectez-vous.");
                mainTabPane.getSelectionModel().select(loginTab);
            } else {
                showError(registerErrorLabel, "⚠ " + (reponse != null ? reponse.getMessage() : "Serveur error"));
            }
        });
    }

    @FXML private void handleSwitchToRegister() { mainTabPane.getSelectionModel().select(registerTab); }

    private String validatePassword(String pw) {
        if (pw == null || pw.isEmpty()) return "⚠ Requis.";
        if (pw.length() < PW_MIN) return "⚠ Min " + PW_MIN + " caractères.";
        if (!HAS_UPPER.matcher(pw).matches()) return "⚠ Majuscule requise (A-Z).";
        if (!HAS_DIGIT.matcher(pw).matches()) return "⚠ Chiffre requis (0-9).";
        if (!HAS_SPECIAL.matcher(pw).matches()) return "⚠ Caractère spécial requis.";
        return null;
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
        if (registerPasswordField != null && strengthBarBox != null)
            registerPasswordField.textProperty().addListener((obs, old, val) -> updateStrengthDisplay(val, strengthBarBox, strengthLabel));
    }

    private void updateStrengthDisplay(String pw, HBox barBox, Label textLabel) {
        int score = scorePassword(pw);
        String[] barColors = { "#e8e8ef", "#E74C3C", "#E67E22", "#FDBF50", "#27AE60" };
        var segs = barBox.getChildren();
        for (int i = 0; i < segs.size(); i++) {
            if (segs.get(i) instanceof Region seg) {
                String color = (i < score && score > 0) ? barColors[score] : "#e8e8ef";
                seg.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
            }
        }
        if (textLabel != null) {
            String[] labels = { "", "Faible", "Moyen", "Fort", "Très fort" };
            String[] txtColors = { "transparent", "#E74C3C", "#E67E22", "#c89600", "#27AE60" };
            textLabel.setText(pw.isEmpty() ? "" : labels[score]);
            textLabel.setStyle("-fx-text-fill:" + txtColors[score] + ";-fx-font-weight:bold;");
        }
    }

    private void runAsync(Requete requete, java.util.function.Consumer<Reponse> onDone) {
        Task<Reponse> task = new Task<>() {
            @Override protected Reponse call() { return ClientSocket.getInstance().envoyer(requete); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> onDone.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> onDone.accept(null)));
        Thread t = new Thread(task); t.setDaemon(true); t.start();
    }

    private void navigateToMain(String type) {
        SceneManager.clearHistory();
        registerUdpPort(SessionManager.getInstance().getSession().getAccessToken());
        if ("ADMIN".equals(type)) SceneManager.switchTo("admin.fxml", "Administration");
        else SceneManager.switchTo("panier.fxml", "Panier");
    }

    private void registerUdpPort(String token) {
        if (token == null) return;
        new Thread(() -> {
            try {
                Map<String, Object> p = new HashMap<>(); 
                p.put("udpPort", client.ClientApp.UDP_PORT);
                ClientSocket.getInstance().envoyer(new Requete(RequestType.REGISTER_UDP_PORT, p, token));
            } catch (Exception e) {}
        }).start();
    }

    private void setupTabSwitching() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            clearErrors();
            VBox form = (selected == loginTab) ? loginFormBox : registerFormBox;
            if (form != null) slideIn(form);
        });
    }

    private void setupFocusEffects() {
        bindFocus(loginEmailField, loginEmailWrapper);
        bindFocus(loginPasswordField, loginPasswordWrapper);
        bindFocus(registerNomField, registerNameWrapper);
        bindFocus(registerPrenomField, registerFirstNameWrapper);
        bindFocus(registerPhoneField, registerPhoneWrapper);
        bindFocus(registerEmailField, registerEmailWrapper);
        bindFocus(registerPasswordField, registerPasswordWrapper);
        bindFocus(registerConfirmPasswordField, registerConfirmWrapper);
    }
    private void bindFocus(TextField f, HBox w) {
        if (f == null || w == null) return;
        f.focusedProperty().addListener((obs, was, is) -> {
            if (is) w.getStyleClass().add("input-wrapper-focused");
            else w.getStyleClass().remove("input-wrapper-focused");
        });
    }
    private void setupLiveValidation() {
        for (TextField f : new TextField[]{loginEmailField, loginPasswordField, registerNomField, registerPrenomField, registerPhoneField, registerEmailField, registerPasswordField, registerConfirmPasswordField}) {
            if (f != null) f.textProperty().addListener((o, old, n) -> clearErrors());
        }
    }
    private void setupEnterNavigation() {
        if (loginEmailField != null) loginEmailField.setOnAction(e -> loginPasswordField.requestFocus());
        if (loginPasswordField != null) loginPasswordField.setOnAction(e -> handleLogin());
        if (registerConfirmPasswordField != null) registerConfirmPasswordField.setOnAction(e -> handleRegister());
    }
    private void setLoginLoading(boolean l) { if (loginButton != null) loginButton.setDisable(l); }
    private void setRegisterLoading(boolean l) { if (registerButton != null) registerButton.setDisable(l); }
    private void showError(Label l, String m) { if (l != null) { l.setText(m); l.getStyleClass().add("error-label"); fadeIn(l); } }
    private void showSuccess(Label l, String m) { if (l != null) { l.setText(m); l.getStyleClass().add("error-label-success"); fadeIn(l); } }
    private void clearErrors() { 
        if (loginErrorLabel != null) loginErrorLabel.setText(""); 
        if (registerErrorLabel != null) registerErrorLabel.setText(""); 
    }
    private void animateCardEntrance() {
        if (loginCard == null) return;
        loginCard.setOpacity(0); loginCard.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(500), loginCard);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), loginCard);
        tt.setFromY(20); tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }
    private void slideIn(VBox f) {
        if (f == null) return;
        f.setOpacity(0); f.setTranslateY(10);
        FadeTransition ft = new FadeTransition(Duration.millis(300), f);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), f);
        tt.setFromY(10); tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }
    private void shake(Region n) {
        if (n == null) return;
        TranslateTransition t = new TranslateTransition(Duration.millis(50), n);
        t.setFromX(0); t.setByX(5); t.setCycleCount(6); t.setAutoReverse(true); t.play();
    }
    private void fadeIn(Node n) { if (n != null) { FadeTransition ft = new FadeTransition(Duration.millis(300), n); ft.setFromValue(0); ft.setToValue(1); ft.play(); } }
    private void delay(int ms, Runnable a) { PauseTransition p = new PauseTransition(Duration.millis(ms)); p.setOnFinished(e -> a.run()); p.play(); }
    private String trim(TextField f) { return (f == null || f.getText() == null) ? "" : f.getText().trim(); }
}