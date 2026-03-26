package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import client.utils.SceneManager;
import client.utils.SessionManager;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutController {
    private static List<String> selectedSkus;

    public static void setSelectedSkus(List<String> skus) {
        selectedSkus = skus;
    }

    // --- Step panels ---
    @FXML private VBox step1Form;
    @FXML private VBox step2Form;
    @FXML private VBox step3Form;

    @FXML private Circle step1Circle;
    @FXML private Circle step2Circle;
    @FXML private Circle step3Circle;

    // --- Step 1 fields ---
    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private ComboBox<String> cmbAdresse;
    @FXML private TextField txtNouvelleAdresse;
    @FXML private TextField txtVille;
    @FXML private TextField txtCodePostal;
    @FXML private TextField txtTelephone;

    // --- Step 2 fields ---
    @FXML private VBox cardOption;
    @FXML private VBox cashOption;
    @FXML private Label lblOrderId;
    @FXML private Label lblOrderTotal;
    @FXML private RadioButton radioCard;
    @FXML private RadioButton radioCash;
    @FXML private VBox cardFormBox;
    @FXML private TextField txtCardNumber;
    @FXML private Label lblCardNumberPreview;
    @FXML private TextField txtExpiry;
    @FXML private Label lblExpiryPreview;

    // Stored addresses data
    private List<Map<String, Object>> savedAddresses = new ArrayList<>();
    private static final String NEW_ADDRESS_OPTION = "+ Nouvelle adresse";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {
        setupPaymentOptions();
        prefillUserData();
        loadAddresses();
    }

    // ──────────────────────────────────────────
    // Pre-fill user info from session / server
    // ──────────────────────────────────────────
    private void prefillUserData() {
        SessionManager sm = SessionManager.getInstance();
        // If session already has the profile data, fill immediately
        if (sm.getPrenom() != null) {
            txtPrenom.setText(sm.getPrenom());
            txtNom.setText(sm.getNom());
            txtTelephone.setText(sm.getTelephone());
        } else {
            // Fetch from server in background
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    shared.Requete req = new shared.Requete(
                        shared.RequestType.GET_PROFILE,
                        Map.of("idClient", sm.getUserId()),
                        sm.getToken()
                    );
                    shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                    if (rep.isSucces() && rep.getDonnees() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
                        String prenom = (String) data.get("prenom");
                        String nom = (String) data.get("nom");
                        String telephone = (String) data.get("telephone");
                        sm.setProfile(nom, prenom, telephone);
                        javafx.application.Platform.runLater(() -> {
                            txtPrenom.setText(prenom != null ? prenom : "");
                            txtNom.setText(nom != null ? nom : "");
                            txtTelephone.setText(telephone != null ? telephone : "");
                        });
                    }
                    return null;
                }
            };
            executor.submit(task);
        }
    }

    // ──────────────────────────────────────────
    // Load saved addresses into ComboBox
    // ──────────────────────────────────────────
    private void loadAddresses() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                shared.Requete req = new shared.Requete(
                    shared.RequestType.GET_ADDRESSES,
                    Map.of("idClient", SessionManager.getInstance().getUserId()),
                    SessionManager.getInstance().getToken()
                );
                shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                if (rep.isSucces() && rep.getDonnees() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> adresses = (List<Map<String, Object>>) data.get("adresses");
                    if (adresses != null) savedAddresses = adresses;
                }
                return null;
            }
            @Override
            protected void succeeded() {
                cmbAdresse.getItems().clear();
                for (Map<String, Object> a : savedAddresses) {
                    String display = a.get("addresseComplete") + ", " + a.get("ville");
                    cmbAdresse.getItems().add(display);
                }
                cmbAdresse.getItems().add(NEW_ADDRESS_OPTION);
                if (!cmbAdresse.getItems().isEmpty() && savedAddresses.size() > 0) {
                    cmbAdresse.setValue(cmbAdresse.getItems().get(0));
                }

                // Show/hide new address field based on selection
                cmbAdresse.valueProperty().addListener((obs, oldVal, newVal) -> {
                    boolean isNew = NEW_ADDRESS_OPTION.equals(newVal);
                    txtNouvelleAdresse.setVisible(isNew);
                    txtNouvelleAdresse.setManaged(isNew);
                    if (!isNew && newVal != null) {
                        // Pre-fill ville et code postal from selected address
                        int idx = cmbAdresse.getItems().indexOf(newVal);
                        if (idx >= 0 && idx < savedAddresses.size()) {
                            txtVille.setText((String) savedAddresses.get(idx).get("ville"));
                            txtCodePostal.setText((String) savedAddresses.get(idx).get("codePostal"));
                        }
                    } else if (isNew) {
                        txtVille.clear();
                        txtCodePostal.clear();
                    }
                });
                // Trigger initial ville/codePostal fill
                if (savedAddresses.size() > 0) {
                    txtVille.setText((String) savedAddresses.get(0).get("ville"));
                    txtCodePostal.setText((String) savedAddresses.get(0).get("codePostal"));
                }
            }
        };
        executor.submit(task);
    }

    // ──────────────────────────────────────────
    // Payment options
    // ──────────────────────────────────────────
    private void setupPaymentOptions() {
        if (cardOption != null && cashOption != null) {
            cardOption.setOnMouseClicked(e -> radioCard.setSelected(true));
            cashOption.setOnMouseClicked(e -> radioCash.setSelected(true));

            radioCard.selectedProperty().addListener((obs, oldVal, newVal) -> {
                cardFormBox.setVisible(newVal);
                cardFormBox.setManaged(newVal);
                updatePaymentStyles();
            });

            radioCash.selectedProperty().addListener((obs, oldVal, newVal) -> {
                radioCard.setSelected(!newVal);
                updatePaymentStyles();
            });
        }
    }

    private void updatePaymentStyles() {
        if (radioCard.isSelected()) {
            cardOption.getStyleClass().add("payment-selected");
            cashOption.getStyleClass().remove("payment-selected");
        } else {
            cashOption.getStyleClass().add("payment-selected");
            cardOption.getStyleClass().remove("payment-selected");
        }
    }

    @FXML
    private void updateCardPreview() {
        String num = txtCardNumber.getText().replaceAll("\\s", "");
        if (num.length() >= 4) {
            String formatted = num.replaceAll("(.{4})", "$1 ").trim();
            if (formatted.length() > 19) formatted = formatted.substring(0, 19);
            lblCardNumberPreview.setText(formatted);
        } else {
            lblCardNumberPreview.setText("•••• •••• •••• 3456");
        }
        String exp = txtExpiry.getText();
        lblExpiryPreview.setText(exp.matches("\\d{2}/\\d{2}") ? exp : "03/28");
    }

    // ──────────────────────────────────────────
    // Navigation between steps
    // ──────────────────────────────────────────
    @FXML
    private void goToStep1() {
        step1Form.setVisible(true);  step1Form.setManaged(true);
        step2Form.setVisible(false); step2Form.setManaged(false);
        step3Form.setVisible(false); step3Form.setManaged(false);
        step1Circle.getStyleClass().setAll("step-circle-active");
        step2Circle.getStyleClass().setAll("step-circle-done");
    }

    @FXML
    private void goToStep2() {
        // If "Nouvelle adresse" — save it first
        if (NEW_ADDRESS_OPTION.equals(cmbAdresse.getValue())) {
            String newAddr = txtNouvelleAdresse.getText().trim();
            String ville   = txtVille.getText().trim();
            String codePostal = txtCodePostal.getText().trim();
            if (!newAddr.isEmpty() && !ville.isEmpty() && !codePostal.isEmpty()) {
                executor.submit(() -> {
                    java.util.Map<String, Object> p = new java.util.HashMap<>();
                    p.put("idClient", SessionManager.getInstance().getUserId());
                    p.put("addresseComplete", newAddr);
                    p.put("ville", ville);
                    p.put("codePostal", codePostal);
                    shared.Requete req = new shared.Requete(shared.RequestType.ADD_ADDRESS, p, SessionManager.getInstance().getToken());
                    client.ClientSocket.getInstance().envoyer(req);
                });
            }
        }

        step1Form.setVisible(false); step1Form.setManaged(false);
        step2Form.setVisible(true);  step2Form.setManaged(true);
        step3Form.setVisible(false); step3Form.setManaged(false);
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-active");
        step3Circle.getStyleClass().setAll("step-circle-future");
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void goToStep3() {
        step1Form.setVisible(false); step1Form.setManaged(false);
        step2Form.setVisible(false); step2Form.setManaged(false);
        step3Form.setVisible(true);  step3Form.setManaged(true);
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-done");
        step3Circle.getStyleClass().setAll("step-circle-done");

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("idClient", SessionManager.getInstance().getUserId());
        params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
        params.put("statut", "VALIDEE");

        shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getToken());
        shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);

        if (rep.isSucces() && rep.getDonnees() != null) {
            Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
            String ref   = (String) data.get("reference");
            double total = ((Number) data.get("total")).doubleValue();
            lblOrderId.setText("#" + ref);
            lblOrderTotal.setText("Montant : " + String.format("%,.2f MAD", total));
        } else {
            lblOrderId.setText("Erreur");
            double baseTotal = (selectedSkus != null) ? selectedSkus.size() * 500.0 : 0.0;
            lblOrderTotal.setText("Montant : " + String.format("%,.2f MAD", baseTotal));
        }
    }

    @FXML
    private void goToHome() {
        SceneManager.switchTo("produits.fxml", "ChriOnline - Catalogue");
    }

    @FXML
    private void goBack() {
        if (step1Form.isVisible() || step2Form.isVisible()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation d'annulation");
            alert.setHeaderText("Voulez-vous enregistrer cette commande en brouillon ?");
            alert.setContentText("Choisissez 'Draft' pour sauvegarder en attente, ou 'Annuler la commande' pour retourner au panier sans rien sauvegarder.");

            ButtonType buttonDraft   = new ButtonType("Draft");
            ButtonType buttonAnnuler = new ButtonType("Annuler la commande");
            ButtonType buttonRester  = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonDraft, buttonAnnuler, buttonRester);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent() || result.get() == buttonRester) {
                return;
            }
            if (result.get() == buttonDraft) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("idClient", SessionManager.getInstance().getUserId());
                params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
                params.put("statut", "EN_ATTENTE");

                shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getToken());
                client.ClientSocket.getInstance().envoyer(req);

                SceneManager.clearCache("panier.fxml");
                SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier");
                return;
            }
        }
        SceneManager.back();
    }
}
