package admin_ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import model.SKU;
import shared.Reponse;
import shared.Requete;
import shared.RequestType;
import client.ClientSocket;
import client.utils.SessionManager;
import javafx.stage.FileChooser;
import service.ImageService;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SKUEditController {

    @FXML private TextField skuCodeField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private TextField imageField;

    private SKU sku;
    private boolean saveClicked = false;
    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setSKU(SKU sku) {
        this.sku = sku;
        skuCodeField.setText(sku.getSku());
        prixField.setText(String.valueOf(sku.getPrix()));
        stockField.setText(String.valueOf(sku.getQuantite()));
        imageField.setText(sku.getImage());
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            sku.setPrix(new BigDecimal(prixField.getText()));
            sku.setQuantite(Integer.parseInt(stockField.getText()));
            sku.setImage(imageField.getText());

            String token = SessionManager.getInstance().getSession().getAccessToken();
            Map<String, Object> params = new HashMap<>();
            params.put("sku", sku);

            new Thread(() -> {
                Reponse rep = ClientSocket.getInstance().envoyer(new Requete(RequestType.UPDATE_SKU, params, token));
                if (rep.isSucces()) {
                    javafx.application.Platform.runLater(() -> {
                        saveClicked = true;
                        dialogStage.close();
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        // Show error alert
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText("Erreur lors de la modification");
                        alert.setContentText(rep.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image pour le SKU");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            String currentText = imageField.getText();
            imageField.setText("Téléchargement en cours...");
            imageField.setDisable(true);

            new Thread(() -> {
                String imageUrl = ImageService.uploadProductImage(selectedFile.getAbsolutePath(), 0);
                javafx.application.Platform.runLater(() -> {
                    imageField.setDisable(false);
                    if (imageUrl != null) {
                        imageField.setText(imageUrl);
                    } else {
                        imageField.setText(currentText);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur Upload");
                        alert.setHeaderText("Échec de l'envoi vers Cloudinary");
                        alert.setContentText("Une erreur est survenue lors de l'upload de l'image.");
                        alert.showAndWait();
                    }
                });
            }).start();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (prixField.getText() == null || prixField.getText().length() == 0) {
            errorMessage += "Prix non valide !\n";
        } else {
            try {
                new BigDecimal(prixField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Le prix doit être un nombre !\n";
            }
        }

        if (stockField.getText() == null || stockField.getText().length() == 0) {
            errorMessage += "Stock non valide !\n";
        } else {
            try {
                Integer.parseInt(stockField.getText());
            } catch (NumberFormatException e) {
                errorMessage += "Le stock doit être un nombre entier !\n";
            }
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Champs invalides");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
