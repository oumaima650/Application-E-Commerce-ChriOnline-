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
            // S'assurer que les champs sont éditables pour une nouvelle commande
            txtPrenom.setEditable(true);
            txtNom.setEditable(true);
            txtTelephone.setEditable(true);
            txtPrenom.setStyle("");
            txtNom.setStyle("");
            txtTelephone.setStyle("");
            
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
        step1Form.setVisible(false);
        step1Form.setManaged(false);
        step2Form.setVisible(false);
        step2Form.setManaged(false);
        step3Form.setVisible(true);
        step3Form.setManaged(true);
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-done");
        step3Circle.getStyleClass().setAll("step-circle-done");

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
        params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
        params.put("statut", "VALIDEE");
        
        if (resumingOrderReference != null) {
            params.put("reference", resumingOrderReference);
            // On reset après usage pour les prochaines commandes normales
            resumingOrderReference = null;
        }

        shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getSession().getAccessToken());
        shared.Reponse rep = client.ClientSocket.getInstance().envoyer(req);

        if (!rep.isSucces() || rep.getDonnees() == null) {
            lblOrderId.setText("Erreur");
            lblOrderTotal.setText("Une erreur est survenue lors de la validation.");
            return;
        }

        javafx.application.Platform.runLater(() -> {
            Map<String, Object> data = (Map<String, Object>) rep.getDonnees();
            String ref = (String) data.get("reference");
            
            // Extraction sécurisée du total
            double total = 0.0;
            Object totalObj = data.get("total");
            if (totalObj instanceof Number n) {
                total = n.doubleValue();
            } else if (totalObj instanceof String s) {
                try { total = Double.parseDouble(s); } catch (Exception ignored) {}
            }
            
            lblOrderId.setText("#" + (ref != null ? ref : "INC"));
            lblOrderTotal.setText("Montant : " + String.format("%,.2f MAD", total));

            String dateLiv = (String) data.get("dateLivraison");
            if (dateLiv != null) {
                lblDeliveryDate.setText("Livraison par ChriOnline prévue le : " + dateLiv);
            }

            // Afficher le récapitulatif des produits
            vboxOrderSummary.getChildren().clear();
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    try {
                        String nom = (String) item.get("nom");
                        Object qObj = item.get("quantite");
                        int qty = (qObj instanceof Number n) ? n.intValue() : 1;
                        
                        Object pObj = item.get("prixUnitaire");
                        double prix = (pObj instanceof Number n) ? n.doubleValue() : 0.0;
                        
                        String imgPath = (String) item.get("image");

                        vboxOrderSummary.getChildren().add(createSummaryRow(nom, qty, prix, imgPath));
                    } catch (Exception e) {
                        System.err.println("Erreur item summary: " + e.getMessage());
                    }
                }
            }
            // Forcer le rafraîchissement du panier lors du prochain accès
            SceneManager.clearCache("panier.fxml");
        });
    }

    private HBox createSummaryRow(String nom, int qty, double prix, String imgPath) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 5; -fx-background-color: white; -fx-background-radius: 8;");

        // Photo ou Icone
        javafx.scene.Node visual;
        if (imgPath != null && !imgPath.isEmpty() && (imgPath.startsWith("http") || imgPath.startsWith("https"))) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(imgPath, true));
                iv.setFitWidth(45);
                iv.setFitHeight(45);
                iv.setPreserveRatio(true);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(45, 45);
                clip.setArcWidth(10); clip.setArcHeight(10);
                iv.setClip(clip);
                visual = iv;
            } catch (Exception ex) {
                visual = ui.utils.IconLibrary.getIcon(ui.utils.IconLibrary.PHONE, 24, "#95A5A6");
            }
        } else {
            String iconPath = (imgPath == null || imgPath.isEmpty() || imgPath.endsWith(".jpg") || imgPath.endsWith(".png"))
                    ? ui.utils.IconLibrary.PHONE : imgPath;
            visual = ui.utils.IconLibrary.getIcon(iconPath, 24, "#95A5A6");
        }

        StackPane imgContainer = new StackPane(visual);
        imgContainer.setMinWidth(50);
        imgContainer.setPrefSize(50, 50);
        imgContainer.setAlignment(javafx.geometry.Pos.CENTER);
        imgContainer.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8;");

        VBox details = new VBox(2);
        Label lblNom = new Label(nom != null ? nom : "Produit");
        lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #2F3640;");
        Label lblQtyPrice = new Label(qty + " x " + String.format("%.2f", prix) + " MAD");
        lblQtyPrice.setStyle("-fx-font-size: 11; -fx-text-fill: #7F8C8D;");
        details.getChildren().addAll(lblNom, lblQtyPrice);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblTotal = new Label(String.format("%.2f MAD", qty * prix));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        row.getChildren().addAll(imgContainer, details, spacer, lblTotal);
        return row;
    }

    @FXML
    private void goToHome() {
        SceneManager.switchTo("main-home.fxml", "ChriOnline - Accueil");
    }

    @FXML
    private void goBack() {
        if (step1Form.isVisible() || step2Form.isVisible()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation d'annulation");
            alert.setHeaderText("Voulez-vous enregistrer cette commande en brouillon ?");
            alert.setContentText("Choisissez 'Draft' pour sauvegarder en attente, ou 'Annuler la commande' pour retourner au panier sans rien sauvegarder.");

            ButtonType buttonDraft = new ButtonType("Draft");
            ButtonType buttonAnnuler = new ButtonType("Annuler la commande");
            ButtonType buttonRester = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonDraft, buttonAnnuler, buttonRester);

            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent() || result.get() == buttonRester) {
                return;
            }
            if (result.get() == buttonDraft) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("idClient", SessionManager.getInstance().getCurrentUser().getIdUtilisateur());
                params.put("skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList());
                params.put("statut", "EN_ATTENTE");

                shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, params, SessionManager.getInstance().getSession().getAccessToken());
                client.ClientSocket.getInstance().envoyer(req);

                SceneManager.clearCache("panier.fxml");
                SceneManager.switchTo("panier.fxml", "ChriOnline - Mon Panier");
                return;
            }
        }
        SceneManager.back();
    }

    private void showAlertErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de saisie");
        alert.setHeaderText("Validation requise");
        alert.setContentText(message);
        alert.showAndWait();
    }
}