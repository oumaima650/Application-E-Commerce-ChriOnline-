package ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CheckoutController {

    @FXML private VBox step1Form;
    @FXML private VBox step2Form;
    @FXML private VBox step3Form;

    @FXML private Label step1Circle;
    @FXML private Label step2Circle;
    @FXML private Label step3Circle;

    @FXML private RadioButton radioCard;
    @FXML private RadioButton radioCash;
    @FXML private VBox cardFormBox;

    @FXML private TextField txtCardNumber;
    @FXML private Label lblCardNumberPreview;

    @FXML private TextField txtExpiry;
    @FXML private Label lblExpiryPreview;

    @FXML
    public void initialize() {
        // Gérer l'affichage des champs de la carte bancaire selon le radio button
        radioCard.selectedProperty().addListener((obs, oldVal, newVal) -> {
            cardFormBox.setVisible(newVal);
            cardFormBox.setManaged(newVal);
        });
        
        radioCash.selectedProperty().addListener((obs, oldVal, newVal) -> {
            radioCard.setSelected(!newVal);
        });
        
        radioCard.selectedProperty().addListener((obs, oldVal, newVal) -> {
            radioCash.setSelected(!newVal);
        });
    }

    @FXML
    private void updateCardPreview() {
        String num = txtCardNumber.getText();
        lblCardNumberPreview.setText(num.isEmpty() ? "**** **** **** ****" : num);
        
        String exp = txtExpiry.getText();
        lblExpiryPreview.setText(exp.isEmpty() ? "MM/YY" : exp);
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

        step2Circle.getStyleClass().setAll("step-circle-done");
        step3Circle.getStyleClass().setAll("step-circle-active");
        
        // Simulation de réception d'une notification via UDP
        // Normalement ceci serait géré par ClientUDP via Platform.runLater
    }
}

