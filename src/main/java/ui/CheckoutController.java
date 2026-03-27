package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    private static String resumingOrderReference;

    public static void setSelectedSkus(List<String> skus) {
        selectedSkus = skus;
    }

    public static void setResumingOrderReference(String ref) {
        resumingOrderReference = ref;
    }

    // --- Step panels ---
    @FXML
    private VBox step1Form;
    @FXML
    private VBox step2Form;
    @FXML
    private VBox step3Form;
    @FXML
    private VBox vboxOrderSummary;

    @FXML
    private Circle step1Circle;
    @FXML
    private Circle step2Circle;
    @FXML
    private Circle step3Circle;
    @FXML
    private ScrollPane checkoutScrollPane;
    @FXML
    private StackPane rootStackPane;

    // --- Step 1 fields ---
    @FXML
    private TextField txtPrenom;
    @FXML
    private TextField txtNom;
    @FXML
    private ComboBox<String> cmbAdresse;
    @FXML
    private TextField txtNouvelleAdresse;
    @FXML
    private TextField txtVille;
    @FXML
    private TextField txtCodePostal;
    @FXML
    private TextField txtTelephone;

    // --- Step 2 fields ---
    @FXML
    private VBox cardOption;
    @FXML
    private VBox cashOption;
    @FXML
    private Label lblOrderId;
    @FXML
    private Label lblOrderTotal;
    @FXML
    private Label lblDeliveryDate;
    @FXML
    private RadioButton radioCard;
    @FXML
    private RadioButton radioCash;
    @FXML
    private VBox cardFormBox;
    @FXML
    private TextField txtCardNumber;
    @FXML
    private Label lblCardNumberPreview;
    @FXML
    private TextField txtExpiry;
    @FXML
    private TextField txtCvv;
    @FXML
    private Label lblExpiryPreview;

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
        if (!SessionManager.getInstance().isAuthenticated()) {
            SceneManager.switchTo("login.fxml", "Connexion - ChriOnline");
            return;
        }
        setupPaymentOptions();
        
        if (resumingOrderReference != null && !resumingOrderReference.isEmpty()) {
            loadResumingOrderData();
        } else {
            // Personal info fields must NOT be modifiable (use Profile for that)
            txtPrenom.setEditable(false);
            txtNom.setEditable(false);
            txtTelephone.setEditable(false);
            
            String lockedStyle = "-fx-background-color: #F0F2F5; -fx-opacity: 0.8;";
            txtPrenom.setStyle(lockedStyle);
            txtNom.setStyle(lockedStyle);
            txtTelephone.setStyle(lockedStyle);
            
            prefillUserData();
            loadAddresses();
        }
    }

    private void loadResumingOrderData() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                shared.Requete req = new shared.Requete(
                        shared.RequestType.GET_ORDER,
                        Map.of("reference", resumingOrderReference),
                        SessionManager.getInstance().getSession().getAccessToken()
                );
                shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                if (rep.isSucces() && rep.getDonnees() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderData = (Map<String, Object>) rep.getDonnees().get("commande");
                    
                    String adresse = (String) orderData.get("adresse_complete");
                    String methode = (String) orderData.get("methode_paiement");
                    String prenom = (String) orderData.get("prenom");
                    String nom = (String) orderData.get("nom");
                    String tel = (String) orderData.get("telephone");
                    
                    javafx.application.Platform.runLater(() -> {
                        // Pré-remplir les infos personnelles et les rendre inchangeables
                        if (prenom != null) txtPrenom.setText(prenom);
                        if (nom != null) txtNom.setText(nom);
                        if (tel != null) txtTelephone.setText(tel);
                        
                        txtPrenom.setEditable(false);
                        txtNom.setEditable(false);
                        txtTelephone.setEditable(false);
                        // Style visuel pour indiquer que c'est verrouillé
                        txtPrenom.setStyle("-fx-background-color: #F0F2F5; -fx-opacity: 0.8;");
                        txtNom.setStyle("-fx-background-color: #F0F2F5; -fx-opacity: 0.8;");
                        txtTelephone.setStyle("-fx-background-color: #F0F2F5; -fx-opacity: 0.8;");

                        if (adresse != null && !adresse.equals("N/A") && !adresse.equals("Non spécifiée")) {
                            cmbAdresse.setValue(NEW_ADDRESS_OPTION);
                            txtNouvelleAdresse.setText(adresse);
                        }
                        
                        if (methode != null) {
                            if (methode.contains("CASH") || methode.contains("Cash")) {
                                radioCash.setSelected(true);
                            } else if (methode.contains("CARTE") || methode.contains("Carte")) {
                                radioCard.setSelected(true);
                            }
                        }
                    });
                }
                return null;
            }
        };
        executor.submit(task);
        // On charge aussi les adresses habituelles au cas où il veut changer
        loadAddresses();
        
        if (checkoutScrollPane != null) {
            checkoutScrollPane.setFitToHeight(false);
            checkoutScrollPane.setFitToWidth(true);
        }
    }

    // ──────────────────────────────────────────
    // Pre-fill user info from session / server
    // ──────────────────────────────────────────
    private void prefillUserData() {
        SessionManager sm = SessionManager.getInstance();
        if (sm.getCurrentUser() instanceof model.Client clientUser) {
            if (clientUser.getPrenom() != null && !clientUser.getPrenom().isEmpty()) {
                txtPrenom.setText(clientUser.getPrenom());
                txtNom.setText(clientUser.getNom());
                txtTelephone.setText(clientUser.getTelephone());
            } else {
                // Fetch from server in background
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        shared.Requete req = new shared.Requete(
                                shared.RequestType.GET_PROFILE,
                                Map.of("idClient", sm.getCurrentUser().getIdUtilisateur()),
                                sm.getSession().getAccessToken()
                        );
                        shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);
                        if (rep.isSucces() && rep.getDonnees() != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
                            String prenom = (String) data.get("prenom");
                            String nom = (String) data.get("nom");
                            String telephone = (String) data.get("telephone");

                            clientUser.setPrenom(prenom);
                            clientUser.setNom(nom);
                            clientUser.setTelephone(telephone);

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
                        Map.of("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur()),
                        SessionManager.getInstance().getSession().getAccessToken()
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
        step1Form.setVisible(true);
        step1Form.setManaged(true);
        step2Form.setVisible(false);
        step2Form.setManaged(false);
        step3Form.setVisible(false);
        step3Form.setManaged(false);
        step1Circle.getStyleClass().setAll("step-circle-active");
        step2Circle.getStyleClass().setAll("step-circle-done");
    }

    @FXML
    private void goToStep2() {
        if (txtCodePostal == null || txtVille == null || txtNouvelleAdresse == null) {
            System.err.println("[CheckoutController] Erreur fatale : Les champs FXML ne sont pas injectés !");
            return;
        }

        String codePostal = txtCodePostal.getText() != null ? txtCodePostal.getText().trim() : "";
        String ville = txtVille.getText() != null ? txtVille.getText().trim() : "";
        String newAddr = txtNouvelleAdresse.getText() != null ? txtNouvelleAdresse.getText().trim() : "";
        boolean isNewAddress = NEW_ADDRESS_OPTION.equals(cmbAdresse.getValue());

        if (isNewAddress && newAddr.isEmpty()) {
            showAlertErreur("L'adresse complète est obligatoire.");
            return;
        }

        if (ville.isEmpty()) {
            showAlertErreur("La ville est obligatoire.");
            return;
        }

        // Validation basique du code postal (exactement 5 chiffres)
        if (!codePostal.matches("\\d{5}")) {
            showAlertErreur("Le code postal doit contenir exactement 5 chiffres (ex: 20000).");
            return;
        }

        // If "Nouvelle adresse" — save it first
        if (isNewAddress) {
            executor.submit(() -> {
                java.util.Map<String, Object> p = new java.util.HashMap<>();
                p.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
                p.put("addresseComplete", newAddr);
                p.put("ville", ville);
                p.put("codePostal", codePostal);
                shared.Requete req = new shared.Requete(shared.RequestType.ADD_ADDRESS, p, SessionManager.getInstance().getSession().getAccessToken());
                client.ClientSocket.getInstance().envoyer(req);
            });
        }
        step1Form.setVisible(false);
        step1Form.setManaged(false);
        step2Form.setVisible(true);
        step2Form.setManaged(true);
        step3Form.setVisible(false);
        step3Form.setManaged(false);
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-active");
        step3Circle.getStyleClass().setAll("step-circle-future");
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void goToStep3() {
        boolean isCard = radioCard.isSelected();
        if (isCard) {
            String cardNumber = txtCardNumber.getText() != null ? txtCardNumber.getText().replaceAll("\\s+", "") : "";
            String expiry = txtExpiry.getText() != null ? txtExpiry.getText().trim() : "";
            String cvv = txtCvv.getText() != null ? txtCvv.getText().trim() : "";

            if (cardNumber.length() < 13 || cardNumber.length() > 19) {
                showAlertErreur("Le numéro de carte est invalide.");
                return;
            }
            if (!expiry.matches("\\d{2}/\\d{2}")) {
                showAlertErreur("La date d'expiration doit être au format MM/YY.");
                return;
            }
            if (cvv.length() < 3 || cvv.length() > 4) {
                showAlertErreur("Le code CVV est invalide (3 ou 4 chiffres).");
                return;
            }
        }

        // --- Visuel des étapes ---
        step1Form.setVisible(false);
        step1Form.setManaged(false);
        step2Form.setVisible(false);
        step2Form.setManaged(false);
        step3Form.setVisible(true);
        step3Form.setManaged(true);
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-done");
        step3Circle.getStyleClass().setAll("step-circle-done");

        // --- Envoi au serveur ---
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
        params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
        params.put("statut", "VALIDEE");
        
        if (resumingOrderReference != null) {
            params.put("reference", resumingOrderReference);
            resumingOrderReference = null;
        }

        shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getSession().getAccessToken());
        shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);

        if (!rep.isSucces() || rep.getDonnees() == null) {
            lblOrderId.setText("Erreur");
            lblOrderTotal.setText(rep.getMessage() != null ? rep.getMessage() : "Erreur de validation.");
            return;
        }

        javafx.application.Platform.runLater(() -> {
            Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
            lblOrderId.setText("#" + data.get("reference"));
            lblOrderTotal.setText("Montant : " + String.format("%,.2f MAD", ((Number) data.get("total")).doubleValue()));

            String dateLiv = (String) data.get("dateLivraison");
            if (dateLiv != null) lblDeliveryDate.setText("Livraison par ChriOnline prévue le : " + dateLiv);

            vboxOrderSummary.getChildren().clear();
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    vboxOrderSummary.getChildren().add(createSummaryRow(
                        (String) item.get("nom"),
                        ((Number) item.get("quantite")).intValue(),
                        ((Number) item.get("prixUnitaire")).doubleValue(),
                        (String) item.get("image")
                    ));
                }
            }
            SceneManager.clearCache("panier.fxml");
        });
    }

    private HBox createSummaryRow(String nom, int qty, double prix, String imgPath) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-background-radius: 8;");

        javafx.scene.Node visual;
        if (imgPath != null && !imgPath.isEmpty() && (imgPath.startsWith("http"))) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(imgPath, true));
                iv.setFitWidth(45); iv.setFitHeight(45); iv.setPreserveRatio(true);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(45, 45);
                clip.setArcWidth(10); clip.setArcHeight(10);
                iv.setClip(clip);
                visual = iv;
            } catch (Exception ex) {
                visual = ui.utils.IconLibrary.getIcon(ui.utils.IconLibrary.PHONE, 24, "#95A5A6");
            }
        } else {
            visual = ui.utils.IconLibrary.getIcon(ui.utils.IconLibrary.PHONE, 24, "#95A5A6");
        }

        StackPane imgContainer = new StackPane(visual);
        imgContainer.setPrefSize(50, 50);
        imgContainer.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8;");

        VBox details = new VBox(2);
        Label lblNom = new Label(nom); 
        lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #2A2C41; -fx-font-size: 13px;");
        Label lblQtyPrice = new Label(qty + " x " + String.format("%.2f", prix) + " MAD");
        lblQtyPrice.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        details.getChildren().addAll(lblNom, lblQtyPrice);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblTotal = new Label(String.format("%.2f MAD", qty * prix));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF724C; -fx-font-size: 14px;");

        row.getChildren().addAll(imgContainer, details, spacer, lblTotal);
        return row;
    }

    @FXML private void goToHome() { SceneManager.switchTo("main-home.fxml", "ChriOnline - Accueil"); }

    @FXML
    private void goBack() {
        if (step1Form.isVisible() || step2Form.isVisible()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Quitter le paiement");
            alert.setHeaderText("Souhaitez-vous enregistrer cette commande en brouillon ?");
            alert.setContentText("Vous pourrez la retrouver plus tard dans 'Mes Commandes'.");

            ButtonType btnSave = new ButtonType("Enregistrer Brouillon");
            ButtonType btnCancel = new ButtonType("Annuler Commande");
            ButtonType btnStay = new ButtonType("Rester ici", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(btnSave, btnCancel, btnStay);

            var result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnSave) {
                    saveAsDraftAndExit();
                } else if (result.get() == btnCancel) {
                    SceneManager.back();
                }
            }
        } else {
            SceneManager.back();
        }
    }

    private void saveAsDraftAndExit() {
        // Envoi au serveur avec statut EN_ATTENTE
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
        params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
        params.put("statut", "EN_ATTENTE");
        
        if (resumingOrderReference != null) {
            params.put("reference", resumingOrderReference);
            resumingOrderReference = null;
        }

        shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getSession().getAccessToken());
        
        Task<shared.Reponse> task = new Task<>() {
            @Override
            protected shared.Reponse call() {
                return client.ClientSocket.getInstance().envoyer(req);
            }
        };

        task.setOnSucceeded(e -> {
            shared.Reponse rep = task.getValue();
            if (rep != null && rep.isSucces()) {
                SceneManager.clearCache("panier.fxml");
                SceneManager.switchTo("main-home.fxml", "ChriOnline - Accueil");
            } else {
                showAlertErreur("Erreur lors de l'enregistrement du brouillon: " + (rep != null ? rep.getMessage() : "Serveur injoignable"));
            }
        });
        
        new Thread(task).start();
    }

    private void showAlertErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
