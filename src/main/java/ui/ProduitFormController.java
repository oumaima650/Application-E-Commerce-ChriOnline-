package ui;
/*
import client.models.MockDataService.ProduitMock;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ProduitFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNom;
    @FXML private TextField txtCategorie;
    @FXML private TextField txtPrix;
    @FXML private TextField txtStock;

    private ProduitMock produitSelectionne;
    private Runnable onSaveCallback; // Appelé quand la sauvegarde est effectuée

    public void setProduit(ProduitMock produit, Runnable onSave) {
        this.produitSelectionne = produit;
        this.onSaveCallback = onSave;

        if (produit != null) {
            lblTitle.setText("Modifier Produit #" + produit.getId());
            txtNom.setText(produit.getNom());
            txtCategorie.setText(produit.getCategorie());
            txtPrix.setText(produit.getPrix().replace(" MAD", "")); // Enlever ' MAD' pour éditer le chiffre pur
            txtStock.setText(String.valueOf(produit.getStock()));
        }
    }

    @FXML
    private void sauvegarderProduit() {
        if (produitSelectionne != null) {
            // Dans un vrai projet, on appellerait le DAO ici ou un Service TCP pour faire l'UPDATE
            produitSelectionne.nom = txtNom.getText();
            produitSelectionne.categorie = txtCategorie.getText();
            produitSelectionne.prix = txtPrix.getText() + " MAD";
            try {
                produitSelectionne.stock = Integer.parseInt(txtStock.getText());
            } catch (NumberFormatException ignored) {}

            if (onSaveCallback != null) {
                onSaveCallback.run(); // Rafraichir le tableau dans l'admin
            }
        }
        fermerFenetre();
    }

    @FXML
    private void fermerFenetre() {
        Stage stage = (Stage) lblTitle.getScene().getWindow();
        stage.close();
    }
}
*/