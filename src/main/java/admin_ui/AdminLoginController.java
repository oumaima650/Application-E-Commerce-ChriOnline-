package admin_ui;

import client.ClientSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import shared.Reponse;
import shared.RequestType;
import shared.Requete;
import util.P12Signer;
import client.utils.SceneManager;
import client.utils.SessionManager;
import model.Utilisateur;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

public class AdminLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField p12PasswordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = p12PasswordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        File p12File = new File("keys/admin.p12");
        if (!p12File.exists()) {
            errorLabel.setText("Fichier keys/admin.p12 introuvable.");
            return;
        }

        setLoading(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                // 1. Demande de challenge
                Map<String, Object> challengeParams = new HashMap<>();
                challengeParams.put("email", email);
                Requete challengeReq = new Requete(RequestType.ADMIN_CHALLENGE_REQUEST, challengeParams, null);
                Reponse challengeRes = ClientSocket.getInstance().envoyer(challengeReq);

                if (challengeRes == null || !challengeRes.isSucces()) {
                    showError(challengeRes != null ? challengeRes.getMessage() : "Serveur injoignable");
                    return;
                }

                String challenge = (String) challengeRes.getDonnees().get("challenge");

                // 2. Signature locale
                String signature = P12Signer.signChallenge("keys/admin.p12", password, email, challenge);

                // 3. Vérification de la signature
                Map<String, Object> verifyParams = new HashMap<>();
                verifyParams.put("email", email);
                verifyParams.put("signature", signature);
                Requete verifyReq = new Requete(RequestType.ADMIN_CHALLENGE_VERIFY, verifyParams, null);
                Reponse verifyRes = ClientSocket.getInstance().envoyer(verifyReq);

                if (verifyRes != null && verifyRes.isSucces()) {
                    // Succès ! Connexion établie
                    Utilisateur user = (Utilisateur) verifyRes.getDonnees().get("utilisateur");
                    String accessToken = (String) verifyRes.getDonnees().get("accessToken");
                    String refreshToken = (String) verifyRes.getDonnees().get("refreshToken");
                    
                    SessionManager.getInstance().ouvrir(accessToken, refreshToken, user);

                    Platform.runLater(() -> {
                        SceneManager.switchTo("admin.fxml", "Tableau de Bord Administrateur");
                    });
                } else {
                    showError(verifyRes != null ? verifyRes.getMessage() : "Échec de signature");
                }

            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            } finally {
                setLoading(false);
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(loading);
            loginBtn.setDisable(loading);
        });
    }

    private void showError(String msg) {
        Platform.runLater(() -> errorLabel.setText(msg));
    }
}
