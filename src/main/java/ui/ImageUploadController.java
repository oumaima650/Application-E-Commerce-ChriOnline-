package ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.ImageService;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'upload d'images de produits
 */
public class ImageUploadController implements Initializable {

    @FXML private VBox mainContainer;
    @FXML private ImageView previewImage;
    @FXML private Label uploadStatusLabel;
    @FXML private ProgressBar uploadProgressBar;
    @FXML private TextField productIdField;
    @FXML private TextArea urlResultField;
    @FXML private Button uploadButton;
    @FXML private Button browseButton;
    @FXML private Button testConnectionButton;
    
    private File selectedFile;
    private Stage currentStage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        testCloudinaryConnection();
    }
    
    public void setStage(Stage stage) {
        this.currentStage = stage;
    }
    
    private void setupUI() {
        // Configuration initiale de l'UI
        if (previewImage != null) {
            previewImage.setFitWidth(300);
            previewImage.setFitHeight(300);
            previewImage.setPreserveRatio(true);
        }
        
        if (uploadProgressBar != null) {
            uploadProgressBar.setVisible(false);
        }
        
        if (uploadStatusLabel != null) {
            uploadStatusLabel.setText("Prêt à uploader une image");
        }
        
        if (urlResultField != null) {
            urlResultField.setEditable(false);
            urlResultField.setWrapText(true);
        }
    }
    
    @FXML
    private void handleBrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image de produit");
        
        // Filtrer pour les images
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
            "Images (*.jpg, *.jpeg, *.png, *.gif)", 
            "*.jpg", "*.jpeg", "*.png", "*.gif"
        );
        fileChooser.getExtensionFilters().add(extFilter);
        
        File file = fileChooser.showOpenDialog(currentStage);
        if (file != null) {
            selectedFile = file;
            displaySelectedImage(file);
            updateUploadStatus("Image sélectionnée: " + file.getName(), false);
        }
    }
    
    @FXML
    private void handleUploadButton() {
        if (selectedFile == null) {
            updateUploadStatus("Veuillez d'abord sélectionner une image", true);
            return;
        }
        
        String productIdText = productIdField.getText();
        if (productIdText == null || productIdText.trim().isEmpty()) {
            updateUploadStatus("Veuillez entrer l'ID du produit", true);
            return;
        }
        
        try {
            int productId = Integer.parseInt(productIdText.trim());
            uploadImage(selectedFile, productId);
        } catch (NumberFormatException e) {
            updateUploadStatus("ID du produit invalide", true);
        }
    }
    
    @FXML
    private void handleTestConnection() {
        testCloudinaryConnection();
    }
    
    private void uploadImage(File file, int productId) {
        // Désactiver les boutons pendant l'upload
        uploadButton.setDisable(true);
        browseButton.setDisable(true);
        uploadProgressBar.setVisible(true);
        updateUploadStatus("Upload en cours...", false);
        
        // Upload dans un thread séparé pour ne pas bloquer l'UI
        Thread uploadThread = new Thread(() -> {
            try {
                String cloudinaryUrl = ImageService.uploadProductImage(file.getAbsolutePath(), productId);
                
                // Mise à jour de l'UI sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    uploadProgressBar.setVisible(false);
                    uploadButton.setDisable(false);
                    browseButton.setDisable(false);
                    
                    if (cloudinaryUrl != null) {
                        urlResultField.setText(cloudinaryUrl);
                        updateUploadStatus("✅ Image uploadée avec succès!", false);
                        
                        // Afficher l'image uploadée
                        try {
                            Image uploadedImage = new Image(cloudinaryUrl, 300, 300, true, true, true);
                            previewImage.setImage(uploadedImage);
                        } catch (Exception e) {
                            System.err.println("Erreur affichage image uploadée: " + e.getMessage());
                        }
                    } else {
                        updateUploadStatus("❌ Échec de l'upload", true);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    uploadProgressBar.setVisible(false);
                    uploadButton.setDisable(false);
                    browseButton.setDisable(false);
                    updateUploadStatus("❌ Erreur: " + e.getMessage(), true);
                });
            }
        });
        
        uploadThread.start();
    }
    
    private void displaySelectedImage(File file) {
        try {
            Image image = new Image(file.toURI().toString(), 300, 300, true, true, true);
            previewImage.setImage(image);
        } catch (Exception e) {
            System.err.println("Erreur affichage image: " + e.getMessage());
            updateUploadStatus("Erreur d'affichage de l'image", true);
        }
    }
    
    private void testCloudinaryConnection() {
        if (testConnectionButton != null) {
            testConnectionButton.setDisable(true);
            testConnectionButton.setText("Test en cours...");
        }
        
        Thread testThread = new Thread(() -> {
            boolean isConnected = false;
            try {
                isConnected = config.CloudinaryConfig.testConnection();
            } catch (Exception e) {
                System.err.println("Erreur test connexion: " + e.getMessage());
            }
            
            final boolean connected = isConnected;
            javafx.application.Platform.runLater(() -> {
                if (testConnectionButton != null) {
                    testConnectionButton.setDisable(false);
                    testConnectionButton.setText("Tester Connexion");
                }
                
                if (connected) {
                    updateUploadStatus("✅ Connexion Cloudinary établie", false);
                } else {
                    updateUploadStatus("❌ Erreur de connexion Cloudinary - Vérifiez vos clés", true);
                }
            });
        });
        
        testThread.start();
    }
    
    private void updateUploadStatus(String message, boolean isError) {
        if (uploadStatusLabel != null) {
            uploadStatusLabel.setText(message);
            if (isError) {
                uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                uploadStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: normal;");
            }
        }
    }
    
    @FXML
    private void handleCopyUrl() {
        String url = urlResultField.getText();
        if (url != null && !url.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(url);
            clipboard.setContent(content);
            updateUploadStatus("📋 URL copiée dans le presse-papiers", false);
        }
    }
    
    @FXML
    private void handleClose() {
        if (currentStage != null) {
            currentStage.close();
        }
    }
}
