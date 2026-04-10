

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;


-- updates :
ALTER TABLE Client  ADD COLUMN statut     ENUM('ACTIF', 'BANNI', 'EN_ATTENTE') DEFAULT 'EN_ATTENTE' AFTER telephone;
ALTER TABLE Client  ADD COLUMN dateNaissance DATE NULL AFTER statut;
ALTER TABLE Adresse ADD COLUMN codePostal VARCHAR(10) NULL AFTER ville;

-- ------------------------------------------------------------
-- Table : Utilisateur (entité parente de Admin et Client)
-- ------------------------------------------------------------
CREATE TABLE Utilisateur (
    IdUtilisateur   INT             NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    motDePasse      VARCHAR(255)    NOT NULL,
    two_factor_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    createdAt       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (IdUtilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Client (sous-type de Utilisateur)
-- ------------------------------------------------------------
CREATE TABLE Client (
    IdUtilisateur   INT             NOT NULL,
    nom             VARCHAR(100)    NOT NULL,
    prenom          VARCHAR(100)    NOT NULL,
    telephone       VARCHAR(20),
    deletedAt       DATETIME        NULL,
    PRIMARY KEY (IdUtilisateur),
    CONSTRAINT fk_client_utilisateur FOREIGN KEY (IdUtilisateur)
        REFERENCES Utilisateur(IdUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Admin (sous-type de Utilisateur)
-- ------------------------------------------------------------
CREATE TABLE Admin (
    IdUtilisateur   INT             NOT NULL,
    PRIMARY KEY (IdUtilisateur),
    CONSTRAINT fk_admin_utilisateur FOREIGN KEY (IdUtilisateur)
        REFERENCES Utilisateur(IdUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Adresse (0..N adresses par client)
-- ------------------------------------------------------------
CREATE TABLE Adresse (
    idAdresse           INT             NOT NULL AUTO_INCREMENT,
    IdClient            INT             NOT NULL,   -- ← renommé
    addresseComplete    VARCHAR(255)    NOT NULL,
    ville               VARCHAR(100)    NOT NULL,
    createdAt           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletedAt           DATETIME        NULL,
    PRIMARY KEY (idAdresse),
    CONSTRAINT fk_adresse_client FOREIGN KEY (IdClient)
        REFERENCES Client(IdUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Carte_bancaire (0..N cartes par client)
-- ------------------------------------------------------------
CREATE TABLE Carte_bancaire (
    idCarte         INT             NOT NULL AUTO_INCREMENT,
    IdClient        INT             NOT NULL,   -- ← renommé
    numeroCarte     VARCHAR(20)     NOT NULL,
    typeCarte       VARCHAR(50)     NOT NULL,
    PRIMARY KEY (idCarte),
    CONSTRAINT fk_carte_client FOREIGN KEY (IdClient)
        REFERENCES Client(IdUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Categorie
-- ------------------------------------------------------------
CREATE TABLE Categorie (
    idCategorie     INT             NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(100)    NOT NULL,
    description     TEXT,
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (idCategorie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table associative : CategorieVariante  (M:N entre Categorie et Variante)
-- Une variante peut appartenir à plusieurs catégories et vice-versa
-- ------------------------------------------------------------
CREATE TABLE CategorieVariante (
    idCategorie     INT     NOT NULL,
    idVariante      INT     NOT NULL,
    PRIMARY KEY (idCategorie, idVariante),
    CONSTRAINT fk_cv_categorie FOREIGN KEY (idCategorie) REFERENCES Categorie(idCategorie) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cv_variante  FOREIGN KEY (idVariante)  REFERENCES Variante(idVariante)   ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Produit
-- ------------------------------------------------------------
CREATE TABLE Produit (
    idProduit       INT             NOT NULL AUTO_INCREMENT,
    idCategorie     INT             NULL,
    nom             VARCHAR(150)    NOT NULL,
    description     TEXT,
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletedAt       DATETIME        NULL,
    PRIMARY KEY (idProduit)
    CONSTRAINT fk_produit_categorie FOREIGN KEY (idCategorie) REFERENCES Categorie(idCategorie) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Variante (appartient à Produit — 1..N variantes par produit)
-- ------------------------------------------------------------
CREATE TABLE Variante (
    idVariante      INT             NOT NULL AUTO_INCREMENT,
    nom             VARCHAR(150)    NOT NULL,
    description     TEXT,
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (idVariante)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table associative : ProduitVarValeur
-- Lie un Produit à une Variante avec la valeur de l'attribut
-- (ex: Produit "T-shirt" -- Variante "Couleur" -- valeur "Rouge")
-- ------------------------------------------------------------
CREATE TABLE ProduitVarValeur (
    idPVV           INT             NOT NULL AUTO_INCREMENT,
    idProduit       INT             NOT NULL,
    idVariante      INT             NOT NULL,
    valeur          VARCHAR(100)    NOT NULL,
    PRIMARY KEY (idPVV),
    UNIQUE KEY uq_pvv (idProduit, idVariante, valeur),
    CONSTRAINT fk_pvv_produit  FOREIGN KEY (idProduit)  REFERENCES Produit(idProduit)   ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pvv_variante FOREIGN KEY (idVariante) REFERENCES Variante(idVariante) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : SKU  (stock keeping unit – déclinaison vendable)
-- Attributs : SKU (PK), prix, quantite, image
-- ------------------------------------------------------------
CREATE TABLE SKU (
    SKU             VARCHAR(100)    NOT NULL,
    prix            DECIMAL(10,2)   NOT NULL,
    quantite        INT             NOT NULL DEFAULT 0,
    image           VARCHAR(255),
    deletedAt       DATETIME        NULL,
    PRIMARY KEY (SKU)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table associative : SKUVarValeur
-- Lie un SKU à une ligne de ProduitVarValeur (idProduit + idVariante + valeur)
-- ------------------------------------------------------------
CREATE TABLE SKUVarValeur (
    SKU             VARCHAR(100)    NOT NULL,
    idPVV           INT             NOT NULL,
    PRIMARY KEY (SKU, idPVV),
    CONSTRAINT fk_svv_sku FOREIGN KEY (SKU)    REFERENCES SKU(SKU)                ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_svv_pvv FOREIGN KEY (idPVV)  REFERENCES ProduitVarValeur(idPVV) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Commande
-- ------------------------------------------------------------
CREATE TABLE Commande (
    idCommande          INT             NOT NULL AUTO_INCREMENT,
    IdClient            INT             NOT NULL,
    idAdresse           INT             NULL,
    reference       VARCHAR(50)     NOT NULL UNIQUE, 
    statut              ENUM('en_attente','validée','expédiée','livrée') NOT NULL DEFAULT 'en_attente',
    dateLivraisonPrevue     DATETIME        NULL,   -- estimée à la commande
    dateLivraisonReelle     DATETIME        NULL,   -- remplie quand livrée
    created_At          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_At          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (idCommande),
    CONSTRAINT fk_commande_client  FOREIGN KEY (IdClient)  REFERENCES Client(IdUtilisateur) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_commande_adresse FOREIGN KEY (idAdresse) REFERENCES Adresse(idAdresse)    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Paiement (relation Payer entre Commande et Carte_bancaire)
-- Cardinalité : Commande 0..N —- 1 Paiement 0..1 —- 0..N Carte_bancaire
-- ------------------------------------------------------------
CREATE TABLE Paiement (
    idPaiement      INT             NOT NULL AUTO_INCREMENT,
    idCommande      INT             NOT NULL,
    idCarte         INT             NULL,
    montant         DECIMAL(10,2)   NOT NULL,
    statutPaiement      ENUM('approuve', 'refuse', 'en_attente', 'rembourse')  NOT NULL DEFAULT 'en_attente',
    methodePaiement     ENUM('card','cash')                 NOT NULL DEFAULT 'card',
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idPaiement),
    CONSTRAINT fk_paiement_commande FOREIGN KEY (idCommande) REFERENCES Commande(idCommande) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_paiement_carte    FOREIGN KEY (idCarte)    REFERENCES Carte_bancaire(idCarte) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : LigneCommande  (relation contient entre SKU et Commande)
-- Cardinalité : Commande 1..N -- contient -- 0..N SKU
-- ------------------------------------------------------------
CREATE TABLE LigneCommande (
    idCommande      INT             NOT NULL,
    SKU             VARCHAR(100)    NOT NULL,
    quantite        INT             NOT NULL DEFAULT 1,
    prixAchat       DECIMAL(10,2)   NOT NULL,
    PRIMARY KEY (idCommande, SKU),
    CONSTRAINT fk_lc_commande FOREIGN KEY (idCommande) REFERENCES Commande(idCommande) ON DELETE CASCADE  ON UPDATE CASCADE,
    CONSTRAINT fk_lc_sku      FOREIGN KEY (SKU)        REFERENCES SKU(SKU)             ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Panier
-- ------------------------------------------------------------
CREATE TABLE Panier (
    idPanier        INT             NOT NULL AUTO_INCREMENT,
    IdClient   INT             NOT NULL,
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idPanier),
    CONSTRAINT fk_panier_client FOREIGN KEY (IdClient)
        REFERENCES Client(IdUtilisateur) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : LignePanier  (relation contient entre Panier et SKU)
-- Cardinalité : Panier 0..N -- contient -- 0..N SKU
-- ------------------------------------------------------------
CREATE TABLE LignePanier (
    idPanier        INT             NOT NULL,
    SKU             VARCHAR(100)    NOT NULL,
    quantite        INT             NOT NULL DEFAULT 1,
    PRIMARY KEY (idPanier, SKU),
    CONSTRAINT fk_lp_panier FOREIGN KEY (idPanier) REFERENCES Panier(idPanier) ON DELETE CASCADE  ON UPDATE CASCADE,
    CONSTRAINT fk_lp_sku    FOREIGN KEY (SKU)      REFERENCES SKU(SKU)         ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Avis  (relation commande entre Client, Produit et Commande)
-- ------------------------------------------------------------
CREATE TABLE Avis (
    idCommentaire   INT             NOT NULL AUTO_INCREMENT,
    IdClient   INT             NOT NULL,
    idProduit       INT             NOT NULL,
    idCommande      INT             NULL,
    contenu         TEXT            NOT NULL,
    evaluation      TINYINT         NULL CHECK (evaluation BETWEEN 1 AND 5),
    image           VARCHAR(255),
    PRIMARY KEY (idCommentaire),
    CONSTRAINT fk_com_client   FOREIGN KEY (IdClient) REFERENCES Client(IdUtilisateur)   ON DELETE CASCADE  ON UPDATE CASCADE,
    CONSTRAINT fk_com_produit  FOREIGN KEY (idProduit)     REFERENCES Produit(idProduit)       ON DELETE CASCADE  ON UPDATE CASCADE,
    CONSTRAINT fk_com_commande FOREIGN KEY (idCommande)    REFERENCES Commande(idCommande)     ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : Notification
-- ------------------------------------------------------------
CREATE TABLE Notification (
    idNotification  INT             NOT NULL AUTO_INCREMENT,
    IdUtilisateur   INT             NOT NULL,
    contenu         TEXT            NOT NULL,
    statut          ENUM('lu','non_lu') NOT NULL DEFAULT 'non_lu',
    created_At      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idNotification),
    CONSTRAINT fk_notif_utilisateur FOREIGN KEY (IdUtilisateur) REFERENCES Utilisateur(IdUtilisateur) ON DELETE CASCADE  ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table : TwoFactorCodes
-- ------------------------------------------------------------
CREATE TABLE TwoFactorCodes (
    email           VARCHAR(255)    NOT NULL,
    code_hash       VARCHAR(255)    NOT NULL,
    expires_at      DATETIME        NOT NULL,
    attempts        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
