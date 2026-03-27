package ui;

import client.ClientSocket;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import model.Categorie;
import model.Produit;
import model.Variante;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;

import java.util.*;
import java.util.stream.Collectors;

public class ProductAddWizardController {

    @FXML private VBox step1Box, step2Box, step3Box, step4Box;
    @FXML private HBox step1Indicator, step2Indicator, step3Indicator, step4Indicator;
    @FXML private Button btnBack, btnNext, btnFinish;

    // Step 1
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Categorie> cbCategory;

    // Step 2
    @FXML private VBox variantsContainer;

    // Step 3
    @FXML private ListView<String> listCombinations;
    @FXML private FlowPane manualEntryContainer;

    // Step 4
    @FXML private TableView<SkuRow> tableFinalSkus;
    @FXML private TableColumn<SkuRow, String> colSkuName;
    @FXML private TableColumn<SkuRow, String> colSkuPrice;
    @FXML private TableColumn<SkuRow, String> colSkuQuantity;
    @FXML private TableColumn<SkuRow, String> colSkuImage;

    private int currentStep = 1;
    private List<Variante> selectedCategoryVariants = new ArrayList<>();
    private Map<Integer, List<String>> variantValuesMap = new HashMap<>(); // idVariante -> List of values
    private List<Map<Integer, String>> generatedCombinations = new ArrayList<>();
    private Map<Integer, ComboBox<String>> manualComboBoxes = new HashMap<>();

    @FXML
    public void initialize() {
        setupStep1();
        setupStep4Table();
    }

    private void setupStep1() {
        String token = client.utils.SessionManager.getInstance().isAuthenticated() ? 
                      client.utils.SessionManager.getInstance().getSession().getAccessToken() : "";
        Requete req = new Requete(RequestType.GET_ALL_CATEGORIES, null, token);
        Reponse rep = ClientSocket.getInstance().envoyer(req);
        if (rep.isSucces()) {
            List<Categorie> categories = (List<Categorie>) rep.getDonnees().get("categories");
            cbCategory.setItems(FXCollections.observableArrayList(categories));
            cbCategory.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Categorie item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getNom());
                }
            });
            cbCategory.setButtonCell(cbCategory.getCellFactory().call(null));
        }
    }

    private void setupStep4Table() {
        colSkuName.setCellValueFactory(data -> data.getValue().combinationProperty());
        colSkuName.setCellFactory(tc -> new TableCell<SkuRow, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2A2C41;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });
        
        colSkuPrice.setCellFactory(tc -> new TableCell<SkuRow, String>() {
            private final TextField textField = new TextField();
            {
                textField.setPromptText("0.00");
                textField.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #2A2C41; -fx-alignment: center;");
                textField.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV && getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setPrice(textField.getText());
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    textField.setText(getTableRow().getItem().getPrice());
                    setGraphic(textField);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colSkuQuantity.setCellFactory(tc -> new TableCell<SkuRow, String>() {
            private final TextField textField = new TextField();
            {
                textField.setPromptText("0");
                textField.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #2A2C41; -fx-alignment: center;");
                textField.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV && getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setQuantity(textField.getText());
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    textField.setText(getTableRow().getItem().getQuantity());
                    setGraphic(textField);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colSkuImage.setCellFactory(tc -> new TableCell<SkuRow, String>() {
            private final TextField textField = new TextField();
            private final Button uploadBtn = new Button("📷");
            private final HBox container = new HBox(5, textField, uploadBtn);
            {
                textField.setPromptText("URL ou Upload...");
                textField.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #2A2C41;");
                HBox.setHgrow(textField, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(0, 5, 0, 5));

                textField.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV && getTableRow() != null && getTableRow().getItem() != null) {
                        getTableRow().getItem().setImageUrl(textField.getText());
                    }
                });

                uploadBtn.setStyle("-fx-background-color: #F0F4FF; -fx-text-fill: #2A2C41; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4; -fx-border-color: #DDE4FF; -fx-border-radius: 4;");
                uploadBtn.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Sélectionner une image");
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
                    File file = fileChooser.showOpenDialog(uploadBtn.getScene().getWindow());
                    if (file != null) {
                        String oldText = textField.getText();
                        textField.setText("Upload...");
                        textField.setDisable(true);
                        uploadBtn.setDisable(true);

                        new Thread(() -> {
                            String url = service.ImageService.uploadProductImage(file.getAbsolutePath(), 0);
                            javafx.application.Platform.runLater(() -> {
                                textField.setDisable(false);
                                uploadBtn.setDisable(false);
                                if (url != null) {
                                    textField.setText(url);
                                    if (getTableRow() != null && getTableRow().getItem() != null) {
                                        getTableRow().getItem().setImageUrl(url);
                                    }
                                } else {
                                    textField.setText(oldText);
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Erreur");
                                    alert.setHeaderText("Échec de l'upload");
                                    alert.setContentText("Une erreur est survenue lors de l'upload vers Cloudinary.");
                                    alert.showAndWait();
                                }
                            });
                        }).start();
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    textField.setText(getTableRow().getItem().getImageUrl());
                    setGraphic(container);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    @FXML
    private void handleNext() {
        if (currentStep == 1) {
            if (validateStep1()) {
                currentStep = 2;
                loadStep2();
                updateUI();
            }
        } else if (currentStep == 2) {
            if (validateStep2()) {
                currentStep = 3;
                loadStep3();
                updateUI();
            }
        } else if (currentStep == 3) {
            currentStep = 4;
            loadStep4();
            updateUI();
        }
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            updateUI();
        }
    }

    private void updateUI() {
        step1Box.setVisible(currentStep == 1);
        step2Box.setVisible(currentStep == 2);
        step3Box.setVisible(currentStep == 3);
        step4Box.setVisible(currentStep == 4);

        btnBack.setVisible(currentStep > 1);
        btnNext.setVisible(currentStep < 4);
        btnFinish.setVisible(currentStep == 4);
        btnFinish.setManaged(currentStep == 4);

        updateIndicators();
    }

    private void updateIndicators() {
        step1Indicator.getStyleClass().setAll(currentStep >= 1 ? "step-active" : "step-inactive");
        step2Indicator.getStyleClass().setAll(currentStep >= 2 ? "step-active" : "step-inactive");
        step3Indicator.getStyleClass().setAll(currentStep >= 3 ? "step-active" : "step-inactive");
        step4Indicator.getStyleClass().setAll(currentStep >= 4 ? "step-active" : "step-inactive");
    }

    private boolean validateStep1() {
        if (txtName.getText().trim().isEmpty() || cbCategory.getValue() == null) {
            showAlert("Données manquantes", "Veuillez saisir un nom et choisir une catégorie.");
            return false;
        }
        return true;
    }

    private void loadStep2() {
        int idCat = cbCategory.getValue().getIdCategorie();
        String token = client.utils.SessionManager.getInstance().isAuthenticated() ? 
                      client.utils.SessionManager.getInstance().getSession().getAccessToken() : "";
        Map<String, Object> params = new HashMap<>();
        params.put("idCategorie", idCat);
        Requete req = new Requete(RequestType.ADMIN_GET_VARIANTES_BY_CATEGORIE, params, token);
        Reponse rep = ClientSocket.getInstance().envoyer(req);
        if (rep.isSucces()) {
            selectedCategoryVariants = (List<Variante>) rep.getDonnees().get("variantes");
            
            variantsContainer.getChildren().clear();
            for (Variante v : selectedCategoryVariants) {
                VBox vBox = new VBox(10);
                Label lbl = new Label(v.getNom());
                lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1A1C2E;");
                
                FlowPane tagsPane = new FlowPane(10, 10);
                TextField input = new TextField();
                input.setPromptText("Ajouter une valeur (ex: Rouge, XL...)");
                input.setPrefWidth(200);
                
                Button btnAdd = new Button("+");
                btnAdd.setOnAction(e -> {
                    String val = input.getText().trim();
                    if (!val.isEmpty()) {
                        addValueTag(v.getIdVariante(), val, tagsPane);
                        input.clear();
                    }
                });
                
                HBox inputRow = new HBox(12);
                inputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label prefix = new Label(v.getNom() + " :");
                prefix.setStyle("-fx-text-fill: #8892A4; -fx-font-size: 13px;");
                
                inputRow.getChildren().addAll(prefix, input, btnAdd);
                
                vBox.getChildren().addAll(lbl, tagsPane, inputRow);
                variantsContainer.getChildren().add(vBox);
            }
        }
    }

    private void addValueTag(int variantId, String value, FlowPane pane) {
        variantValuesMap.computeIfAbsent(variantId, k -> new ArrayList<>()).add(value);
        
        HBox tag = new HBox(8);
        tag.setStyle("-fx-background-color: #F0F4FF; -fx-padding: 6 12; -fx-background-radius: 20; -fx-border-color: #D1D5DB; -fx-border-radius: 20;");
        Label lbl = new Label(value);
        lbl.setStyle("-fx-text-fill: #1A1C2E; -fx-font-weight: bold;");
        Button btnDel = new Button("×");
        btnDel.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand; -fx-text-fill: #FF724C; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnDel.setOnAction(e -> {
            pane.getChildren().remove(tag);
            variantValuesMap.get(variantId).remove(value);
        });
        tag.getChildren().addAll(lbl, btnDel);
        pane.getChildren().add(tag);
    }

    private boolean validateStep2() {
        if (variantValuesMap.isEmpty()) {
            showAlert("Aucure variante", "Veuillez définir au moins une valeur pour une variante.");
            return false;
        }
        return true;
    }

    private void loadStep3() {
        setupManualEntry();
        // Optionnel: auto-générer par défaut ? Non, on laisse l'utilisateur choisir.
    }

    private void setupManualEntry() {
        manualEntryContainer.getChildren().clear();
        manualComboBoxes.clear();
        
        for (Variante v : selectedCategoryVariants) {
            List<String> values = variantValuesMap.get(v.getIdVariante());
            if (values != null && !values.isEmpty()) {
                VBox box = new VBox(5);
                Label lbl = new Label(v.getNom());
                lbl.setStyle("-fx-text-fill: #2A2C41; -fx-font-weight: bold; -fx-font-size: 12px;");
                
                ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(values));
                cb.setPromptText("Choisir...");
                cb.setPrefWidth(120);
                cb.setStyle("-fx-background-radius: 8; -fx-border-radius: 8;");
                
                manualComboBoxes.put(v.getIdVariante(), cb);
                box.getChildren().addAll(lbl, cb);
                manualEntryContainer.getChildren().add(box);
            }
        }
    }

    @FXML
    private void handleAddManual() {
        Map<Integer, String> combination = new HashMap<>();
        for (Map.Entry<Integer, ComboBox<String>> entry : manualComboBoxes.entrySet()) {
            String val = entry.getValue().getValue();
            if (val == null) {
                showAlert("Incomplet", "Veuillez sélectionner une valeur pour chaque variante.");
                return;
            }
            combination.put(entry.getKey(), val);
        }

        // Vérifier unicité
        boolean exists = generatedCombinations.stream().anyMatch(existing -> existing.equals(combination));
        if (exists) {
            showAlert("Doublon", "Cette combinaison existe déjà dans la liste.");
            return;
        }

        generatedCombinations.add(combination);
        updateCombinationsList();
    }

    @FXML
    private void handleAutoGenerate() {
        generatedCombinations.clear();
        List<Integer> keys = new ArrayList<>(variantValuesMap.keySet());
        generateCombinationsRecursive(0, keys, new HashMap<>());
        
        updateCombinationsList();
    }

    private void generateCombinationsRecursive(int index, List<Integer> keys, Map<Integer, String> current) {
        if (index == keys.size()) {
            generatedCombinations.add(new HashMap<>(current));
            return;
        }
        int variantId = keys.get(index);
        List<String> values = variantValuesMap.get(variantId);
        for (String v : values) {
            current.put(variantId, v);
            generateCombinationsRecursive(index + 1, keys, current);
        }
    }

    private void updateCombinationsList() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Map<Integer, String> comb : generatedCombinations) {
            items.add(formatCombination(comb));
        }
        listCombinations.setItems(items);
        listCombinations.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox box = new HBox(15);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    box.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1.5;");
                    
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: #2A2C41; -fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    Button btnRem = new Button("Supprimer");
                    btnRem.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #E74C3C; -fx-border-radius: 8; -fx-padding: 5 10;");
                    btnRem.setOnAction(e -> {
                        int idx = getIndex();
                        if (idx >= 0 && idx < generatedCombinations.size()) {
                            generatedCombinations.remove(idx);
                            updateCombinationsList();
                        }
                    });
                    
                    Region r = new Region(); HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
                    box.getChildren().addAll(lbl, r, btnRem);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadStep4() {
        ObservableList<SkuRow> rows = FXCollections.observableArrayList();
        for (Map<Integer, String> comb : generatedCombinations) {
            rows.add(new SkuRow(comb));
        }
        tableFinalSkus.setItems(rows);
    }

    @FXML
    private void handleFinish() {
        // Envoi au serveur
        Produit p = new Produit();
        p.setNom(txtName.getText());
        p.setDescription(txtDescription.getText());

        List<Map<String, Object>> variantsData = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : variantValuesMap.entrySet()) {
            Map<String, Object> vMap = new HashMap<>();
            vMap.put("idVariante", entry.getKey());
            vMap.put("values", entry.getValue());
            variantsData.add(vMap);
        }

        List<Map<String, Object>> skusData = new ArrayList<>();
        int i = 1;
        for (SkuRow row : tableFinalSkus.getItems()) {
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("sku", p.getNom().replaceAll("\\s+", "-").toUpperCase() + "-" + System.currentTimeMillis() + "-" + i++);
            skuMap.put("price", row.getPrice());
            skuMap.put("quantity", Integer.parseInt(row.getQuantity()));
            skuMap.put("imageUrl", row.getImageUrl());
            skuMap.put("combinations", row.getCombMap());
            skusData.add(skuMap);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("produit", p);
        params.put("variantsData", variantsData);
        params.put("skusData", skusData);

        String token = client.utils.SessionManager.getInstance().getSession().getAccessToken();
        Requete req = new Requete(RequestType.ADMIN_ADD_PRODUCT_COMPLET, params, token);
        Reponse rep = ClientSocket.getInstance().envoyer(req);
        if (rep.isSucces()) {
            showAlert("Succès", "Le produit et ses SKUs ont été créés avec succès !");
            btnFinish.getScene().getWindow().hide();
        } else {
            showAlert("Erreur", "Erreur serveur : " + rep.getMessage());
        }
    }

    private String formatCombination(Map<Integer, String> comb) {
        return comb.entrySet().stream()
                .map(e -> getVariantName(e.getKey()) + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private String getVariantName(int id) {
        return selectedCategoryVariants.stream()
                .filter(v -> v.getIdVariante() == id)
                .map(Variante::getNom)
                .findFirst().orElse("?");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Class interne pour la table du Step 4
    public static class SkuRow {
        private final Map<Integer, String> combMap;
        private final javafx.beans.property.StringProperty combination;
        private final javafx.beans.property.StringProperty price = new javafx.beans.property.SimpleStringProperty("0.00");
        private final javafx.beans.property.StringProperty quantity = new javafx.beans.property.SimpleStringProperty("0");
        private final javafx.beans.property.StringProperty imageUrl = new javafx.beans.property.SimpleStringProperty("");

        public SkuRow(Map<Integer, String> combMap) {
            this.combMap = combMap;
            this.combination = new javafx.beans.property.SimpleStringProperty(
                combMap.values().stream().collect(Collectors.joining(" / "))
            );
        }

        public Map<Integer, String> getCombMap() { return combMap; }
        public String getCombination() { return combination.get(); }
        public javafx.beans.property.StringProperty combinationProperty() { return combination; }
        public String getPrice() { return price.get(); }
        public void setPrice(String p) { price.set(p); }
        public String getQuantity() { return quantity.get(); }
        public void setQuantity(String q) { quantity.set(q); }
        public String getImageUrl() { return imageUrl.get(); }
        public void setImageUrl(String u) { imageUrl.set(u); }
    }
}
