package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import client.utils.SceneManager;
import ui.utils.IconLibrary;
import javafx.scene.shape.SVGPath;

public class CheckoutController {
    private static java.util.List<String> selectedSkus;

    public static void setSelectedSkus(java.util.List<String> skus) {
        selectedSkus = skus;
    }


    @FXML private VBox step1Form;
    @FXML private VBox step2Form;
    @FXML private VBox step3Form;

    @FXML private Circle step1Circle;
    @FXML private Circle step2Circle;
    @FXML private Circle step3Circle;

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

    @FXML
    public void initialize() {
        // Gérer les options de paiement
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
            if (formatted.length() > 19) {
                formatted = formatted.substring(0, 19);
            }
            lblCardNumberPreview.setText(formatted);
        } else {
            lblCardNumberPreview.setText("•••• •••• •••• 3456");
        }
        
        String exp = txtExpiry.getText();
        lblExpiryPreview.setText(exp.matches("\\d{2}/\\d{2}") ? exp : "03/28");
    }

    @FXML
    private void goToStep1() {
        step1Form.setVisible(true); step1Form.setManaged(true);
        step2Form.setVisible(false); step2Form.setManaged(false);
        step3Form.setVisible(false); step3Form.setManaged(false);

        step1Circle.getStyleClass().setAll("step-circle-active");
        step2Circle.getStyleClass().setAll("step-circle-done");
    }

    @FXML
    private void goToStep2() {
        step1Form.setVisible(false); step1Form.setManaged(false);
        step2Form.setVisible(true); step2Form.setManaged(true);
        step3Form.setVisible(false); step3Form.setManaged(false);

        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-active");
        step3Circle.getStyleClass().setAll("step-circle-done");
    }

    @FXML
    private void goToStep3() {
        step1Form.setVisible(false); step1Form.setManaged(false);
        step2Form.setVisible(false); step2Form.setManaged(false);
        step3Form.setVisible(true); step3Form.setManaged(true);
        
        step1Circle.getStyleClass().setAll("step-circle-done");
        step2Circle.getStyleClass().setAll("step-circle-done");
        step3Circle.getStyleClass().setAll("step-circle-done");
        
        // Envoyer la requête de validation au serveur avec les SKUs sélectionnés
        shared.Requete req = new shared.Requete(shared.RequestType.VALIDATE_ORDER, 
            java.util.Map.of("idClient", 7, "skus", selectedSkus != null ? selectedSkus : java.util.Collections.emptyList()), 
            "DEBUG");
        
        client.ClientSocket.getInstance().envoyer(req);
        
        // Simuler un montant basé sur la réponse (simplifié ici pour la démo UI)
        double baseTotal = (selectedSkus != null) ? selectedSkus.size() * 500.0 : 0.0;
        lblOrderTotal.setText("Montant : " + String.format("%,.2f MAD", baseTotal));


    }
    
    @FXML
    private void goToHome() {
        SceneManager.switchTo("produits.fxml", "ChriOnline - Catalogue");
    }
    
    @FXML
    private void goBack() {
        SceneManager.back();
    }
}

