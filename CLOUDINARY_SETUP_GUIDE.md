# Guide de Configuration Cloudinary pour ChriOnline

## 📋 Étapes de Configuration

### 1. Créer un compte Cloudinary

1. Allez sur https://cloudinary.com/
2. Cliquez sur "Sign up for free"
3. Choisissez le plan "Free" (25 crédits/mois, suffisant pour commencer)
4. Remplissez le formulaire avec:
   - Email professionnel
   - Mot de passe
   - Nom de votre entreprise: "ChriOnline"
5. Vérifiez votre email

### 2. Obtenir vos clés Cloudinary

Après connexion, allez dans:
- **Dashboard** → **Settings** (icône d'engrenage)
- **Security** → **API Keys**
- Notez ces 3 informations:
  - **Cloud name**: `your-cloud-name`
  - **API Key**: `123456789012345`
  - **API Secret**: `AbCdEf1234567890`

### 3. Configurer le projet Maven

Ajoutez ces dépendances dans votre `pom.xml`:

```xml
<!-- Cloudinary SDK -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
    <version>1.33.0</version>
</dependency>

<!-- Si vous utilisez Java 11+, vous pouvez aussi utiliser: -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http5</artifactId>
    <version>1.33.0</version>
</dependency>
```

### 4. Mettre à jour la configuration

Modifiez le fichier `src/main/java/config/CloudinaryConfig.java`:

```java
// Remplacez ces valeurs par vos vraies clés
private static final String CLOUD_NAME = "votre-cloud-name";
private static final String API_KEY = "votre-api-key";
private static final String API_SECRET = "votre-api-secret";
```

### 5. Tester la configuration

Compilez et testez:

```bash
mvn clean compile
mvn test
```

## 🚀 Utilisation

### Upload d'images via l'interface

1. Lancez l'application
2. Allez dans la section admin
3. Cliquez sur "Upload Images"
4. Entrez l'ID du produit
5. Sélectionnez une image
6. Cliquez sur "Uploader"

### Upload par code

```java
// Upload depuis un fichier
String url = ImageService.uploadProductImage("C:/path/to/image.jpg", 123);

// Upload depuis des bytes
String url = ImageService.uploadProductImage(imageBytes, 123);

// URL optimisée pour l'affichage
String optimizedUrl = ImageService.getProductImageUrl(url, 300, 300);
```

## 📁 Structure des URLs Cloudinary

Les images seront organisées comme suit:
```
https://res.cloudinary.com/votre-cloud-name/image/upload/
v1234567890/chrionline/products/product_123_abcde123.jpg
```

- **chrionline/products/**: Dossier de base
- **product_123_abcde123**: ID unique (product_{productId}_{random})
- **Optimisations automatiques**: Format WebP, compression, etc.

## 🔧 Fonctionnalités disponibles

### 1. Upload d'images
- Supporte JPG, PNG, GIF, WebP
- Compression automatique
- Conversion au format optimal
- Redimensionnement intelligent

### 2. Optimisation d'URL
```java
// Génère une URL optimisée pour 300x300px
String url = ImageService.getProductImageUrl(cloudinaryUrl, 300, 300);
```

### 3. Suppression d'images
```java
boolean success = ImageService.deleteProductImage(cloudinaryUrl);
```

### 4. Validation d'URL
```java
boolean isValid = ImageService.isImageUrlValid(url);
```

## 🎨 Intégration avec l'interface

### Dans MainHomeController
Les images sont déjà configurées pour s'afficher dans:
- Hero banner (slider automatique)
- Cards de produits
- Pages de détails

### Dans ProductDetailController
```java
// Affichage optimisé
String optimizedUrl = ImageService.getProductImageUrl(product.getImage(), 400, 400);
productImageView.setImage(new Image(optimizedUrl, 400, 400, true, true, true));
```

## 🔒 Sécurité

1. **Ne jamais exposer** l'API Secret dans le code client
2. **Utiliser** des signed URLs pour les uploads privés
3. **Configurer** les transformations autorisées
4. **Surveiller** l'utilisation des crédits

## 📊 Monitoring

Sur votre dashboard Cloudinary:
- **Usage**: Credits consommés
- **Storage**: Espace utilisé
- **Bandwidth**: Transfert de données
- **Transformations**: Optimisations appliquées

## 🆘 Dépannage

### Erreur: "Invalid credentials"
- Vérifiez vos clés dans CloudinaryConfig.java
- Assurez-vous que le cloud name est correct

### Erreur: "Upload failed"
- Vérifiez la connexion internet
- Vérifiez que le fichier n'est pas corrompu
- Vérifiez les crédits disponibles

### Images ne s'affichent pas
- Vérifiez que l'URL est correcte
- Testez avec `ImageService.isImageUrlValid(url)`
- Vérifiez les CORS settings dans Cloudinary

## 📈 Coûts

Plan Free (25 crédits/mois):
- **Storage**: 25GB
- **Bandwidth**: 25GB/month  
- **Transformations**: 25K/month
- **Master accounts**: 3

Pour ChriOnline, le plan free est largement suffisant pour commencer!

## 🔄 Migration depuis les images locales

Pour migrer vos images existantes:

```java
// Script de migration
for (Produit produit : produits) {
    if (produit.getImageLocale() != null) {
        String cloudinaryUrl = ImageService.uploadProductImage(
            produit.getImageLocale(), 
            produit.getIdProduit()
        );
        produit.setImage(cloudinaryUrl);
        produitDAO.update(produit);
    }
}
```

---

**🎉 Félicitations!** Votre application ChriOnline est maintenant configurée avec Cloudinary pour une gestion optimisée des images!
