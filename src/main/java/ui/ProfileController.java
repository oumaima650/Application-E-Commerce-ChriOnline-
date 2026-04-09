package ui;

import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.Utilisateur;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileController {

    @FXML
    private Label lblFullName;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblPhone;
    @FXML
    private Label lblInitials;
    @FXML
    private Label lblMemberSince;
    @FXML
    private ToggleButton btnToggle2FA;
    @FXML
    private VBox addressesContainer;
    @FXML
    private Label lblCartItemCount;

    // Verification Overlay
    @FXML
    private VBox verificationOverlay;
    @FXML
    private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML
    private Label verificationErrorLabel;

    private String pendingCode = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }
        loadProfileData();
        loadAddresses();
        loadCartSummary();
        setupOTPHandling();
    }

    private void setupOTPHandling() {
        TextField[] boxes = { otp1, otp2, otp3, otp4, otp5, otp6 };
        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            boxes[i].textProperty().addListener((obs, old, val) -> {
                if (val.length() >= 1) {
                    boxes[idx].setText(val.substring(0, 1).toUpperCase());
                    if (idx < 5)
                        boxes[idx + 1].requestFocus();
                }
            });
            boxes[i].setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE && boxes[idx].getText().isEmpty() && idx > 0) {
                    boxes[idx - 1].requestFocus();
                }
            });
        }
    }

    private void loadProfileData() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String fullName = "";
            if (user instanceof model.Client client) {
                fullName = client.getPrenom() + " " + client.getNom();
                lblPhone.setText(client.getTelephone());
            } else if (user instanceof model.Administrateur) {
                fullName = "Administrateur";
                lblPhone.setText("N/A");
            }
            lblFullName.setText(fullName.isBlank() ? user.getEmail() : fullName);
            lblEmail.setText(user.getEmail());

            // Initials
            if (!fullName.isBlank()) {
                String[] parts = fullName.split(" ");
                String initials = parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1)
                    initials += parts[1].substring(0, 1).toUpperCase();
                lblInitials.setText(initials);
            } else {
                lblInitials.setText(user.getEmail().substring(0, 1).toUpperCase());
            }

            // 2FA Toggle
            btnToggle2FA.setSelected(user.isTwoFactorEnabled());
            updateToggle2FAText();
        }
    }

    private void updateToggle2FAText() {
        if (btnToggle2FA.isSelected()) {
            btnToggle2FA.setText("Activé");
            btnToggle2FA.getStyleClass().add("active");
        } else {
            btnToggle2FA.setText("Désactivé");
            btnToggle2FA.getStyleClass().remove("active");
        }
    }

    @FXML
    private void handleToggle2FA() {
        boolean selected = btnToggle2FA.isSelected();
        Utilisateur user = SessionManager.getInstance().getCurrentUser();

        if (selected) {
            // Activation process: Request code first
            btnToggle2FA.setSelected(false); // Revert until confirmed

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Activer la Double Authentification");
            confirmAlert.setHeaderText("Sécurisez votre compte");
            confirmAlert.setContentText(
                    "Un code de confirmation va être envoyé à votre adresse e-mail. Voulez-vous continuer ?");

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                executor.submit(() -> {
                    Requete reqGen = new Requete(RequestType.GENERATE_2FA_CODE, null,
                            SessionManager.getInstance().getSession().getAccessToken());
                    Reponse repGen = client.ClientSocket.getInstance().envoyer(reqGen);

                    Platform.runLater(() -> {
                        if (repGen.isSucces()) {
                            showVerificationOverlay();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur", repGen.getMessage());
                        }
                    });
                });
            }
        } else {
            // Deactivation process
            executor.submit(() -> {
                Map<String, Object> params = Map.of("enabled", false);
                Requete req = new Requete(RequestType.TOGGLE_2FA, params,
                        SessionManager.getInstance().getSession().getAccessToken());
                Reponse rep = client.ClientSocket.getInstance().envoyer(req);

                Platform.runLater(() -> {
                    if (rep.isSucces()) {
                        user.setTwoFactorEnabled(false);
                        updateToggle2FAText();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Double authentification désactivée.");
                    } else {
                        btnToggle2FA.setSelected(true); // Revert
                        showAlert(Alert.AlertType.ERROR, "Erreur", rep.getMessage());
                    }
                });
            });
        }
    }

    private void showVerificationOverlay() {
        TextField[] boxes = { otp1, otp2, otp3, otp4, otp5, otp6 };
        for (TextField b : boxes)
            b.clear();
        verificationErrorLabel.setVisible(false);
        verificationOverlay.setManaged(true);
        verificationOverlay.setVisible(true);
        otp1.requestFocus();
    }

    @FXML
    private void handleConfirm2FA() {
        String code = otp1.getText() + otp2.getText() + otp3.getText() + otp4.getText() + otp5.getText()
                + otp6.getText();
        if (code.length() < 6)
            return;

        executor.submit(() -> {
            Map<String, Object> params = Map.of("enabled", true, "code", code);
            Requete req = new Requete(RequestType.TOGGLE_2FA, params,
                    SessionManager.getInstance().getSession().getAccessToken());
            Reponse rep = client.ClientSocket.getInstance().envoyer(req);

            Platform.runLater(() -> {
                if (rep.isSucces()) {
                    SessionManager.getInstance().getCurrentUser().setTwoFactorEnabled(true);
                    btnToggle2FA.setSelected(true);
                    updateToggle2FAText();
                    handleCancel2FA();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Double authentification activée !");
                } else {
                    verificationErrorLabel.setText(rep.getMessage());
                    verificationErrorLabel.setVisible(true);
                }
            });
        });
    }

    @FXML
    private void handleCancel2FA() {
        verificationOverlay.setVisible(false);
        verificationOverlay.setManaged(false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadAddresses() {
        executor.submit(() -> {
            Requete req = new Requete(RequestType.GET_ADDRESSES, null,
                    SessionManager.getInstance().getSession().getAccessToken());
            Reponse rep = client.ClientSocket.getInstance().envoyer(req);

            Platform.runLater(() -> {
                addressesContainer.getChildren().clear();
                if (rep.isSucces() && rep.getDonnees() != null) {
                    List<Map<String, Object>> ads = (List<Map<String, Object>>) rep.getDonnees().get("adresses");
                    if (ads != null && !ads.isEmpty()) {
                        for (Map<String, Object> ad : ads) {
                            String full = (String) ad.get("addresseComplete") + ", " + ad.get("ville") + " ("
                                    + ad.get("codePostal") + ")";
                            Label l = new Label("• " + full);
                            l.getStyleClass().add("address-item");
                            addressesContainer.getChildren().add(l);
                        }
                    } else {
                        addressesContainer.getChildren().add(new Label("Aucune adresse enregistrée."));
                    }
                }
            });
        });
    }

    private void loadCartSummary() {
        executor.submit(() -> {
            Requete req = new Requete(RequestType.GET_CART, null,
                    SessionManager.getInstance().getSession().getAccessToken());
            Reponse rep = client.ClientSocket.getInstance().envoyer(req);
            Platform.runLater(() -> {
                if (rep.isSucces() && rep.getDonnees() != null) {
                    List<?> items = (List<?>) rep.getDonnees().get("panier");
                    lblCartItemCount.setText(items != null ? items.size() + " articles" : "0 articles");
                }
            });
        });
    }

    @FXML
    private void handleAddAddress() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouvelle Adresse");
        dialog.setHeaderText("Ajouter une adresse de livraison");
        dialog.setContentText("Adresse complète (Rue, n°, etc.):");

        dialog.showAndWait().ifPresent(addr -> {
            // Simplified: asking for 3 parts in one dialog is tricky, let's just do a
            // simple add
            String ville = "Casablanca"; // Default for mockup
            String cp = "20000";

            executor.submit(() -> {
                Map<String, Object> params = Map.of(
                        "addresseComplete", addr,
                        "ville", ville,
                        "codePostal", cp);
                Requete req = new Requete(RequestType.ADD_ADDRESS, params,
                        SessionManager.getInstance().getSession().getAccessToken());
                Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                Platform.runLater(() -> {
                    if (rep.isSucces())
                        loadAddresses();
                });
            });
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().fermer();
        SceneManager.clearHistory();
        SceneManager.switchTo("login.fxml", "ChriOnline - Connexion");
    }

    @FXML
    private void handleLogoutAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion globale");
        alert.setHeaderText("Déconnecter TOUS les appareils ?");
        alert.setContentText("Cette action invalidera toutes vos sessions actives.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            executor.submit(() -> {
                Requete req = new Requete(RequestType.LOGOUT_ALL, null,
                        SessionManager.getInstance().getSession().getAccessToken());
                Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                Platform.runLater(() -> {
                    if (rep.isSucces())
                        handleLogout();
                });
            });
        }
    }

    @FXML
    private void goBack() {
        SceneManager.back();
    }

    @FXML
    private void goToCart() {
        SceneManager.switchTo("panier.fxml", "ChriOnline - Panier");
    }}

    
            
    
    
        
            
    
    

    
    
        
        
        
    

    
    
        
        
        
        

        
            
                
                        
                
                
                    
                        
                
            
        
    

    
    
        
    

    
    
        
    

