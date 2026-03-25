# Plan d'implémentation Détaillé : Catalogue et Détails de Produit

Puisque nous utilisons l'architecture **JavaFX** (pour l'interface graphique) et des **Sockets** (pour communiquer avec le serveur), voici comment nous allons construire votre partie (Catalogue et Détails) étape par étape. Ce plan est conçu pour être facile à comprendre.

## User Review Required
Lisez les explications ci-dessous. Si l'approche (Vue + Contrôleur + Réseau) vous semble claire et correspond à vos attentes, nous pourrons passer à l'écriture du code (Execution) !

---

## 🏗️ 1. La page principale : Le Catalogue des Produits

La page du catalogue est la vitrine de l'application. Elle doit afficher tous les produits disponibles sous forme de "cartes" disposées en grille.

### A. L'interface visuelle : [produits.fxml](file:///d:/MiniProjet1%20_securite_informatique/src/main/resources/com/chrionline/fxml/produits.fxml) (Vue)
Le fichier FXML décrit le "dessin" de l'interface. 
- Nous allons utiliser un **`BorderPane`** (un conteneur qui divise l'écran en 5 zones : Haut, Bas, Gauche, Droite, Centre).
- **Haut :** Un titre "Nos Produits" et potentiellement une barre de recherche.
- **Centre :** Un **`ScrollPane`** (qui permet de faire défiler la page avec la souris).
- **Dans le ScrollPane :** Un **`FlowPane`** (une boîte intelligente qui place les éléments côte à côte comme une grille, et passe à la ligne suivante quand il n'y a plus de place). C'est ici que viendront se loger nos cartes de produits.

### B. La logique : [ProduitsController.java](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/ui/ProduitsController.java) (Contrôleur)
C'est le cerveau de la page catalogue. 
1. **Au démarrage de la page (`initialize()`) :** Le contrôleur va construire un objet `Requete` de type `GET_PRODUITS` et l'envoyer au serveur via le `ClientSocket` (le tuyau de communication TCP).
2. **Réception des données :** Le serveur va répondre avec une `Reponse` contenant une liste de d'objets [Produit](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/model/Produit.java#6-43) (provenant de la base de données).
3. **Création des cartes :** Pour *chaque* produit reçu, le contrôleur va fabriquer "à la volée" une petite carte JavaFX (une `VBox` contenant une image, le nom du produit, et un bouton "Voir Détails"). Il ajoutera ensuite cette carte dans le `FlowPane` de l'interface.
4. **Action du bouton :** Quand l'utilisateur cliquera sur "Voir Détails", le contrôleur va activer le routeur ([SceneManager](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/client/utils/SceneManager.java#15-96)) pour changer de page et transmettre le produit cliqué à la page suivante.

---

## 🔍 2. La page de détails : Fiche d'un Produit

Quand un utilisateur clique sur un produit dans le catalogue, il arrive sur cette page pour voir la description complète et choisir ses options (Variantes).

### A. L'interface visuelle : `produit_details.fxml` (Vue)
Ce sera un nouveau fichier propre à cette page.
- Nous utiliserons un **`GridPane`** ou **`HBox`** pour diviser l'écran en deux colonnes.
- **Colonne de gauche :** Une grande image du produit.
- **Colonne de droite :** 
  - Le Nom du produit (en grand).
  - La Description.
  - Le Prix (qui peut varier selon les options).
  - Des listes déroulantes (`ComboBox`) pour choisir les options (Exemple : Taille M, Couleur Bleu).
  - Un bouton "Ajouter au panier".
- **En haut à gauche :** Un bouton "Retour au catalogue".

### B. La logique : `ProduitDetailsController.java` (Contrôleur)
Le cerveau de la fiche produit.
1. **Récupération du produit :** Il reçoit de la page précédente l'objet [Produit](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/model/Produit.java#6-43) que l'utilisateur a choisi.
2. **Affichage basique :** Il modifie les labels (textes) de la page pour afficher le nom et la description du produit.
3. **Interrogation du serveur :** Il envoie une `Requete` au serveur (`GET_SKUS_BY_PRODUIT`) pour obtenir toutes les déclinaisons (variantes) exactes du produit, avec leurs prix et stocks ([SKU](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/model/SKU.java#6-43)).
4. **Action d'ajout au panier :** Quand l'utilisateur clique sur le bouton, le contrôleur récupère l'option sélectionnée (le SKU), fabrique une `Requete` de type `ADD_TO_CART` contenant l'ID du produit, le SKU sélectionné, et la quantité, puis l'envoie au serveur.
5. **Action Retour :** Il utilise la méthode `SceneManager.back()` pour dire à l'application de revenir sur la page du catalogue sans avoir à tout recharger.

---

## 👩‍💻 Résumé du Workflow (Comment ça marche ensemble ?)

1. L'utilisateur lance l'application Client (votre Front-End).
2. L'application ouvre [produits.fxml](file:///d:/MiniProjet1%20_securite_informatique/src/main/resources/com/chrionline/fxml/produits.fxml) et lance [ProduitsController](file:///d:/MiniProjet1%20_securite_informatique/src/main/java/ui/ProduitsController.java#3-6).
3. Le contrôleur dit au Serveur : *"Donne-moi le catalogue"*.
4. Le Serveur consulte la BDD, construit la liste et la renvoie.
5. Le contrôleur dessine les boîtes des produits à l'écran.
6. L'utilisateur clique sur "Voir détails" d'un T-shirt.
7. L'application met le T-shirt en mémoire et ouvre `produit_details.fxml` géré par `ProduitDetailsController`.
8. Ce contrôleur affiche le T-shirt, demande les variantes (Taille, Couleur) au Serveur.
9. L'utilisateur choisit, et clique sur "Ajouter au Panier".
10. L'ordre part au Serveur.

Si ce fonctionnement vous parait clair et logique pour votre partie, nous pouvons démarrer l'implémentation.
