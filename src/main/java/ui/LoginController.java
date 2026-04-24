package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import client.ClientSocket;
import shared.Reponse;
import shared.RequestType;
import shared.Requete;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Utilisateur;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

public class LoginController implements Initializable {

    private static final int     PW_MIN       = 8;
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

    // --- New Fields ---
    @FXML private HBox registerBirthdayWrapper;
    @FXML private DatePicker registerBirthdayPicker;
    @FXML private TextField loginPasswordVisibleField;
    @FXML private Button toggleLoginPasswordBtn;
    @FXML private TextField registerPasswordVisibleField;
    @FXML private Button toggleRegisterPasswordBtn;

    // Verification Overlay
    @FXML private VBox  verificationOverlay;
    @FXML private Label verificationTitle;
    @FXML private Label verificationSubtitle;
    @FXML private Label verificationErrorLabel;
    @FXML private Label verificationRetryLabel;
    @FXML private HBox  otpContainer;
    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Button verifyCodeBtn;

    @FXML private VBox passwordResetContainer;
    @FXML private PasswordField newResetPasswordField;
    @FXML private PasswordField confirmResetPasswordField;

    private String pendingEmail = "";
    private String pendingPassword = ""; // Mot de passe temporaire pour dériver la KEK après login
    private enum OverlayMode { SIGNUP, LOGIN_2FA, PASSWORD_RESET }
    private OverlayMode currentOverlayMode = OverlayMode.SIGNUP;

    // --- reCAPTCHA ---
    @FXML private StackPane loginRecaptchaContainer;
    @FXML private StackPane registerRecaptchaContainer;
    private String loginRecaptchaToken = "";
    private String registerRecaptchaToken = "";
    private WebView loginWebView;
    private WebView registerWebView;

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
        setupRecaptcha(loginRecaptchaContainer, true);
        setupRecaptcha(registerRecaptchaContainer, false);
        setupOtpLogic();
        setupPasswordVisibilitySync();
        animateCardEntrance();
    }

    private void setupOtpLogic() {
        TextField[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].textProperty().addListener((obs, old, val) -> {
                if (val.length() > 0) {
                    if (val.length() > 1) boxes[index].setText(val.substring(val.length() - 1).toUpperCase());
                    else boxes[index].setText(val.toUpperCase());
                    
                    if (index < 5) boxes[index + 1].requestFocus();
                }
            });

            boxes[i].setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") && boxes[index].getText().isEmpty() && index > 0) {
                    boxes[index - 1].requestFocus();
                }
            });
        }
    }

    private void setupPasswordVisibilitySync() {
        loginPasswordField.textProperty().bindBidirectional(loginPasswordVisibleField.textProperty());
        registerPasswordField.textProperty().bindBidirectional(registerPasswordVisibleField.textProperty());
    }

    @FXML
    private void toggleLoginPasswordVisibility() {
        boolean isVisible = loginPasswordVisibleField.isVisible();
        loginPasswordVisibleField.setVisible(!isVisible);
        loginPasswordField.setVisible(isVisible);
        toggleLoginPasswordBtn.setText(isVisible ? "👁" : "🙈");
    }

    @FXML
    private void toggleRegisterPasswordVisibility() {
        boolean isVisible = registerPasswordVisibleField.isVisible();
        registerPasswordVisibleField.setVisible(!isVisible);
        registerPasswordField.setVisible(isVisible);
        toggleRegisterPasswordBtn.setText(isVisible ? "👁" : "🙈");
    }

    @FXML
    private void handleBackToHome() {
        SceneManager.switchTo("main-home.fxml", "Boutique - ChriOnline");
    }

    @FXML
    private void handleForgotPassword() {
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Réinitialisation");
        emailDialog.setHeaderText("Mot de passe oublié ?");
        emailDialog.setContentText("Entrez votre adresse e-mail :");
        emailDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        emailDialog.getDialogPane().getStyleClass().add("custom-dialog");

        Optional<String> emailResult = emailDialog.showAndWait();
        if (emailResult.isPresent()) {
            String email = emailResult.get().trim();
            if (email.isEmpty() || !EMAIL_REGEX.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Email invalide.");
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("email", email);
            setLoginLoading(true);
            runAsync(new Requete(RequestType.REQUEST_PASSWORD_RESET, params, null), res -> {
                setLoginLoading(false);
                if (res != null && res.isSucces()) {
                    showVerificationOverlay(email, OverlayMode.PASSWORD_RESET);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", res != null ? res.getMessage() : "Échec.");
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

        // --- reCAPTCHA Validation ---
        if (loginRecaptchaToken == null || loginRecaptchaToken.isEmpty()) {
            showError(loginErrorLabel, "⚠ Veuillez valider le reCAPTCHA.");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("email", email); params.put("motDePasse", password);
        params.put("recaptchaToken", loginRecaptchaToken);
        pendingPassword = password; // Conserver pour dériver la KEK après authentification
        setLoginLoading(true);
        runAsync(new Requete(RequestType.LOGIN, params, null), reponse -> {
            setLoginLoading(false);
            if (reponse != null && reponse.isSucces()) {
                handleLoginSuccess(reponse);
            } else if (reponse != null && "2FA_REQUIRED".equals(reponse.getMessage())) {
                showVerificationOverlay(email, OverlayMode.LOGIN_2FA);
            } else if (reponse != null && "SIGNUP_VERIFICATION_REQUIRED".equals(reponse.getMessage())) {
                showVerificationOverlay(email, OverlayMode.SIGNUP);
            // --- [WHITELIST IP ADMIN] Vérification IP non autorisée ---
            // Si le serveur retourne IP_NOT_AUTHORIZED (admin depuis une IP interdite),
            // on appelle le gestionnaire global qui : affiche l'alerte + vide la session + redirige
            } else if (reponse != null && "IP_NOT_AUTHORIZED".equals(reponse.getMessage())) {
                SessionManager.handleIpNotAuthorized();
            // --- Fin vérification IP ---
            } else {
                showError(loginErrorLabel, "⚠ " + (reponse != null ? reponse.getMessage() : "Erreur"));
                shake(loginEmailWrapper); shake(loginPasswordWrapper);
                loginRecaptchaToken = "";
                if (loginWebView != null) loginWebView.getEngine().reload();
            }
        });
    }

    private void showVerificationOverlay(String email, OverlayMode mode) {
        this.pendingEmail = email;
        this.currentOverlayMode = mode;
        
        passwordResetContainer.setVisible(mode == OverlayMode.PASSWORD_RESET);
        passwordResetContainer.setManaged(mode == OverlayMode.PASSWORD_RESET);

        switch (mode) {
            case SIGNUP -> {
                verificationTitle.setText("Vérifiez votre inscription");
                verificationSubtitle.setText("Un code a été envoyé à : " + email);
                otpContainer.setManaged(true);
                otpContainer.setVisible(true);
            }
            case LOGIN_2FA -> {
                verificationTitle.setText("Double Authentification");
                verificationSubtitle.setText("Veuillez entrer le code reçu par e-mail.");
                otpContainer.setManaged(true);
                otpContainer.setVisible(true);
            }
            case PASSWORD_RESET -> {
                verificationTitle.setText("Réinitialisation du compte");
                verificationSubtitle.setText("Entrez le code reçu et votre nouveau mot de passe.");
                otpContainer.setManaged(true);
                otpContainer.setVisible(true);
            }
        }
        
        verificationErrorLabel.setVisible(false);
        
        // Clear fields
        TextField[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (TextField b : boxes) b.clear();
        newResetPasswordField.clear();
        confirmResetPasswordField.clear();
        
        verificationOverlay.setManaged(true);
        verificationOverlay.setVisible(true);
        fadeIn(verificationOverlay);
        otp1.requestFocus();
    }

    @FXML
    private void handleVerifyCode() {
        String code = otp1.getText() + otp2.getText() + otp3.getText() + otp4.getText() + otp5.getText() + otp6.getText();
        if (code.length() < 6) {
            verificationErrorLabel.setText("Veuillez entrer le code complet.");
            verificationErrorLabel.setVisible(true);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("email", pendingEmail);
        params.put("code", code);

        RequestType type;
        if (currentOverlayMode == OverlayMode.SIGNUP) {
            type = RequestType.VERIFY_SIGNUP;
        } else if (currentOverlayMode == OverlayMode.LOGIN_2FA) {
            type = RequestType.VERIFY_2FA_LOGIN;
        } else {
            // PASSWORD_RESET
            String newPass = newResetPasswordField.getText();
            String confirm = confirmResetPasswordField.getText();
            
            if (newPass.isEmpty() || !newPass.equals(confirm)) {
                showError(verificationErrorLabel, "⚠ Mots de passe non identiques.");
                return;
            }
            
            String validationErr = validatePassword(newPass);
            if (validationErr != null) {
                showError(verificationErrorLabel, "⚠ " + validationErr);
                return;
            }
            
            // Générer un nouveau sel + DEK pour le nouveau mot de passe
            String newSalt;
            String newWrappedDek;
            try {
                newSalt = client.crypto.KDFService.generateSalt();
                byte[] newKek = client.crypto.KDFService.deriveKEK(newPass, newSalt);
                javax.crypto.SecretKey newDek = client.crypto.EnvelopeEncryptionService.generateDEK();
                newWrappedDek = client.crypto.EnvelopeEncryptionService.wrapDEK(newDek, newKek);
            } catch (Exception e) {
                showError(verificationErrorLabel, "⚠ Erreur lors de la préparation du chiffrement.");
                return;
            }
            params.put("newPassword", newPass);
            params.put("newEncryptionSalt", newSalt);
            params.put("newWrappedDek", newWrappedDek);
            pendingPassword = newPass;
            type = RequestType.CONFIRM_PASSWORD_RESET;
        }
        
        verifyCodeBtn.setDisable(true);
        runAsync(new Requete(type, params, null), reponse -> {
            verifyCodeBtn.setDisable(false);
            if (reponse != null && reponse.isSucces()) {
                if (currentOverlayMode == OverlayMode.PASSWORD_RESET) {
                    handleCancelVerification();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre mot de passe a été réinitialisé.");
                } else {
                    handleLoginSuccess(reponse);
                    handleCancelVerification();
                }
            } else {
                verificationErrorLabel.setText(reponse != null ? reponse.getMessage() : "Erreur de vérification.");
                verificationErrorLabel.setVisible(true);
                shake(otpContainer);
            }
        });
    }

    @FXML
    private void handleCancelVerification() {
        verificationOverlay.setVisible(false);
        verificationOverlay.setManaged(false);
    }

    private void handleLoginSuccess(Reponse reponse) {
        Map<String, Object> donnees = reponse.getDonnees();
        Utilisateur user = (Utilisateur) donnees.get("utilisateur");

        // Dériver la KEK et unwrapper la DEK exclusivement en RAM (Zero-Knowledge)
        String encryptionSalt = (String) donnees.get("encryptionSalt");
        String wrappedDekStr  = (String) donnees.get("wrappedDek");
        
        System.out.println("[LoginController] 🔐 Début Zero-Knowledge unwrap...");
        System.out.println("[LoginController] | Sel présent: " + (encryptionSalt != null) + (encryptionSalt != null ? " (" + encryptionSalt.substring(0, Math.min(10, encryptionSalt.length())) + "...)" : ""));
        System.out.println("[LoginController] | DEK enveloppée présente: " + (wrappedDekStr != null) + (wrappedDekStr != null ? " (" + wrappedDekStr.substring(0, Math.min(10, wrappedDekStr.length())) + "...)" : ""));

        if (encryptionSalt != null && wrappedDekStr != null && !pendingPassword.isEmpty()) {
            try {
                System.out.println("[LoginController] | Longueur du mot de passe: " + pendingPassword.length());
                System.out.println("[LoginController] | Dérivation de la KEK via Argon2...");
                byte[] kek = client.crypto.KDFService.deriveKEK(pendingPassword, encryptionSalt);
                
                System.out.println("[LoginController] | Déballage de la DEK (AES-GCM)...");
                javax.crypto.SecretKey sessionDek =
                    client.crypto.EnvelopeEncryptionService.unwrapDEK(wrappedDekStr, kek);
                
                user.setSessionDek(sessionDek);
                
                // Log d'empreinte pour diagnostic (sans révéler la clé)
                byte[] encoded = sessionDek.getEncoded();
                String fingerPrint = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(encoded, 4));
                System.out.println("[LoginController] ✅ Zero-Knowledge : sessionDek prête (Fingerprint: " + fingerPrint + "...)");
            } catch (Exception e) {
                System.err.println("[LoginController] ❌ ÉCHEC unwrap DEK : " + e.getMessage());
                e.printStackTrace();
            } finally {
                pendingPassword = ""; // Effacer le mot de passe de la mémoire
            }
        } else {
            System.err.println("[LoginController] ⚠️ Données de chiffrement manquantes ou mot de passe vide.");
        }

        SessionManager.getInstance().ouvrir(
            (String)donnees.get("accessToken"),
            (String)donnees.get("refreshToken"),
            user
        );

        // Déchiffrer l'objet utilisateur maintenant que la session est ouverte et la DEK est accessible
        ClientSocket.getInstance().decryptStorageFieldsWithDEK(reponse);

        navigateToMain((String)donnees.get("typeUtilisateur"));
    }


    @FXML
    private void handleRegister() {
        String nom = trim(registerNomField);
        String prenom = trim(registerPrenomField);
        String phone = trim(registerPhoneField);
        String email = trim(registerEmailField);
        String password = registerPasswordField.getText().trim();
        String confirm = registerConfirmPasswordField.getText().trim();

        if (nom.isEmpty()) { showError(registerErrorLabel, "⚠ Nom requis."); shake(registerNameWrapper); return; }
        if (prenom.isEmpty()) { showError(registerErrorLabel, "⚠ Prénom requis."); shake(registerFirstNameWrapper); return; }
        if (registerBirthdayPicker.getValue() == null) { showError(registerErrorLabel, "⚠ Date de naissance requise."); shake(registerBirthdayWrapper); return; }
        if (!EMAIL_REGEX.matcher(email).matches()) { showError(registerErrorLabel, "⚠ Email invalide."); shake(registerEmailWrapper); return; }

        LocalDate dob = registerBirthdayPicker.getValue();

        // --- reCAPTCHA Validation ---
        if (registerRecaptchaToken == null || registerRecaptchaToken.isEmpty()) {
            showError(registerErrorLabel, "⚠ Veuillez valider le reCAPTCHA.");
            return;
        }
        String pwError = validatePassword(password);
        if (pwError != null) { showError(registerErrorLabel, pwError); shake(registerPasswordWrapper); return; }
        if (!password.equals(confirm)) { showError(registerErrorLabel, "⚠ Confirmation différente."); shake(registerConfirmWrapper); return; }

        Map<String, Object> params = new HashMap<>();
        params.put("email", email); params.put("motDePasse", password);
        params.put("nom", nom); params.put("prenom", prenom); params.put("telephone", phone);
        params.put("dateNaissance", dob.toString());
        params.put("recaptchaToken", registerRecaptchaToken);

        // Génération KEK/DEK côté client avant d'envoyer au serveur
        try {
            String encryptionSalt = client.crypto.KDFService.generateSalt();
            byte[] kek = client.crypto.KDFService.deriveKEK(password, encryptionSalt);
            javax.crypto.SecretKey dek = client.crypto.EnvelopeEncryptionService.generateDEK();
            String wrappedDek = client.crypto.EnvelopeEncryptionService.wrapDEK(dek, kek);

            params.put("encryptionSalt", encryptionSalt);
            params.put("wrappedDek", wrappedDek);
            pendingPassword = password; // Conserver pour dériver la KEK après vérification signup

            // Chiffrer les données de profil lors de l'inscription (Zero-Knowledge)
            if (params.containsKey("nom")) 
                params.put("nom", client.crypto.EnvelopeEncryptionService.encryptField((String)params.get("nom"), dek));
            if (params.containsKey("prenom")) 
                params.put("prenom", client.crypto.EnvelopeEncryptionService.encryptField((String)params.get("prenom"), dek));
            if (params.containsKey("telephone")) 
                params.put("telephone", client.crypto.EnvelopeEncryptionService.encryptField((String)params.get("telephone"), dek));
            if (params.containsKey("dateNaissance")) 
                params.put("dateNaissance", client.crypto.EnvelopeEncryptionService.encryptField((String)params.get("dateNaissance"), dek));

        } catch (Exception e) {
            System.err.println("[LoginController] Erreur préparation chiffrement : " + e.getMessage());
            showError(registerErrorLabel, "⚠ Erreur lors de la préparation du chiffrement.");
            return;
        }

        setRegisterLoading(true);
        runAsync(new Requete(RequestType.REGISTER, params, null), reponse -> {
            setRegisterLoading(false);
            if (reponse != null && "SIGNUP_VERIFICATION_REQUIRED".equals(reponse.getMessage())) {
                showVerificationOverlay(email, OverlayMode.SIGNUP);
            } else if (reponse != null && reponse.isSucces()) {
                showSuccess(registerErrorLabel, "✓ Compte créé ! Connectez-vous.");
                mainTabPane.getSelectionModel().select(loginTab);
            } else {
                showError(registerErrorLabel, "⚠ " + (reponse != null ? reponse.getMessage() : "Erreur"));
                registerRecaptchaToken = "";
                if (registerWebView != null) registerWebView.getEngine().reload();
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
            if (segs.get(i) instanceof Region) {
                Region seg = (Region) segs.get(i);
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
        SceneManager.clearHistory();
        registerUdpPort(SessionManager.getInstance().getSession().getAccessToken());

        if ("ADMIN".equals(type)) {
            SceneManager.switchTo("admin.fxml", "ChriOnline - Administration");
        } else {
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

    private void shake(Region n) {
        if (n == null) return;
        TranslateTransition t = new TranslateTransition(Duration.millis(50), n);
        t.setFromX(0); t.setByX(5); t.setCycleCount(6); t.setAutoReverse(true); t.setOnFinished(e -> n.setTranslateX(0)); t.play();
    }
    private void fadeIn(Node n) { if (n != null) { FadeTransition ft = new FadeTransition(Duration.millis(300), n); ft.setFromValue(0); ft.setToValue(1); ft.play(); } }
    private void delay(int ms, Runnable a) { PauseTransition p = new PauseTransition(Duration.millis(ms)); p.setOnFinished(e -> a.run()); p.play(); }
    private String trim(TextField f) { return (f == null || f.getText() == null) ? "" : f.getText().trim(); }
}