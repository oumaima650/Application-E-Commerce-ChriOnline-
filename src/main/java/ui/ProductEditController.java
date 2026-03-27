package ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Produit;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import client.ClientSocket; // or client.ClientSocket, let's check
import client.ClientSocket;
import client.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;

public class ProductEditController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;

    private Stage dialogStage;
    private Produit produit;
    private boolean saveClicked = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProduit(Produit p) {
        this.produit = p;
        nameField.setText(p.getNom());
        descriptionField.setText(p.getDescription());
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            String token = SessionManager.getInstance().getSession().getAccessToken();
            Map<String, Object> params = new HashMap<>();
            params.put("idProduit", produit.getIdProduit());
            params.put("nom", nameField.getText());
            params.put("description", descriptionField.getText());

            Reponse rep = ClientSocket.getInstance().envoyer(new Requete(RequestType.ADMIN_UPDATE_PRODUCT, params, token));
            if (rep.isSucces()) {
                produit.setNom(nameField.getText());
                produit.setDescription(descriptionField.getText());
                saveClicked = true;
                dialogStage.close();
            } else {
                // Show error alert
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setContentText(rep.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            return false;
        }
        return true;
    }
}
