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

    @FXML private Label lblFullName;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;
    @FXML private Label lblMemberSince;
    
    @FXML private VBox addressesContainer;
    @FXML private Label lblCartItemCount;

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
            // Basic since we already have these in the Session object
        }
    }

    private void loadAddresses() {
        executor.submit(() -> {
            Requete req = new Requete(RequestType.GET_ADDRESSES, null, SessionManager.getInstance().getSession().getAccessToken());
            Reponse rep = client.ClientSocket.getInstance().envoyer(req);
            
            Platform.runLater(() -> {
                addressesContainer.getChildren().clear();
                if (rep.isSucces() && rep.getDonnees() != null) {
                    List<Map<String, Object>> ads = (List<Map<String, Object>>) rep.getDonnees().get("adresses");
                    if (ads != null && !ads.isEmpty()) {
                        for (Map<String, Object> ad : ads) {
                            String full = (String) ad.get("addresseComplete") + ", " + ad.get("ville") + " (" + ad.get("codePostal") + ")";
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
            Requete req = new Requete(RequestType.GET_CART, null, SessionManager.getInstance().getSession().getAccessToken());
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
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Adresse");
        dialog.setHeaderText("Ajouter une adresse de livraison");

        // Custom buttons
        ButtonType btnAjouter = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnAjouter, ButtonType.CANCEL);

        // Fields
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField txtAddr = new TextField();
        txtAddr.setPromptText("Adresse complète (Rue, n°, etc.)");
        TextField txtVille = new TextField();
        txtVille.setPromptText("Ville");
        TextField txtCP = new TextField();
        txtCP.setPromptText("Code postal (5 chiffres)");

        content.getChildren().addAll(
            new Label("Adresse :"), txtAddr,
            new Label("Ville :"), txtVille,
            new Label("Code Postal :"), txtCP
        );
        dialog.getDialogPane().setContent(content);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAjouter) {
                return Map.of(
                    "addr", txtAddr.getText().trim(),
                    "ville", txtVille.getText().trim(),
                    "cp", txtCP.getText().trim()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            String addr = res.get("addr");
            String ville = res.get("ville");
            String cp = res.get("cp");

            if (addr.isEmpty() || ville.isEmpty() || !cp.matches("\\d{5}")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de validation");
                alert.setHeaderText("Données invalides");
                alert.setContentText("L'adresse et la ville sont obligatoires. Le code postal doit contenir exactement 5 chiffres.");
                alert.showAndWait();
                return;
            }

            executor.submit(() -> {
                Map<String, Object> params = Map.of(
                    "idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur(),
                    "addresseComplete", addr,
                    "ville", ville,
                    "codePostal", cp
                );
                Requete req = new Requete(RequestType.ADD_ADDRESS, params, SessionManager.getInstance().getSession().getAccessToken());
                Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                Platform.runLater(() -> {
                    if (rep.isSucces()) loadAddresses();
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
                Requete req = new Requete(RequestType.LOGOUT_ALL, null, SessionManager.getInstance().getSession().getAccessToken());
                Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                Platform.runLater(() -> {
                    if (rep.isSucces()) handleLogout();
                });
            });
        }
    }

    @FXML private void goBack() { SceneManager.back(); }
    @FXML private void goToCart() { SceneManager.switchTo("panier.fxml", "ChriOnline - Panier"); }
}
