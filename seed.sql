-- ============================================================
-- Script SEED MySQL — Données de test
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- Utilisateurs (2 admins + 5 clients)
-- motDePasse = 'password123' hashé en bcrypt
-- ------------------------------------------------------------
INSERT INTO Utilisateur (IdUtilisateur, email, motDePasse) VALUES
(1,  'admin1@shop.com',   '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(2,  'admin2@shop.com',   '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(3,  'alice@gmail.com',   '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(4,  'bob@gmail.com',     '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(5,  'clara@gmail.com',   '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(6,  'driss@gmail.com',   '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop'),
(7,  'emma@gmail.com',    '$2b$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnop');

-- ------------------------------------------------------------
-- Admins
-- ------------------------------------------------------------
INSERT INTO Admin (IdUtilisateur) VALUES (1), (2);

-- ------------------------------------------------------------
-- Clients
-- ------------------------------------------------------------
INSERT INTO Client (IdUtilisateur, nom, prenom, telephone) VALUES
(3, 'Dupont',   'Alice',  '0612345678'),
(4, 'Martin',   'Bob',    '0623456789'),
(5, 'Bernard',  'Clara',  '0634567890'),
(6, 'Alaoui',   'Driss',  '0645678901'),
(7, 'Leroy',    'Emma',   NULL);

-- ------------------------------------------------------------
-- Adresses
-- ------------------------------------------------------------
INSERT INTO Adresse (idAdresse, IdClient, addresseComplete, ville) VALUES
(1, 3, '12 Rue des Fleurs',       'Rabat'),
(2, 3, '5 Avenue Hassan II',      'Casablanca'),
(3, 4, '8 Rue Ibn Battouta',      'Fès'),
(4, 5, '23 Boulevard Mohammed V', 'Marrakech'),
(5, 6, '14 Rue Allal Ben Abdallah','Rabat'),
(6, 7, '9 Rue de la Paix',        'Tanger');

-- ------------------------------------------------------------
-- Cartes bancaires
-- ------------------------------------------------------------
INSERT INTO Carte_bancaire (idCarte, IdClient, numeroCarte, typeCarte) VALUES
(1, 3, '4111111111111111', 'Visa'),
(2, 3, '5500005555555559', 'Mastercard'),
(3, 4, '4111111111111112', 'Visa'),
(4, 5, '5500005555555560', 'Mastercard'),
(5, 6, '4111111111111113', 'Visa');

-- ------------------------------------------------------------
-- Catégories
-- ------------------------------------------------------------
INSERT INTO Categorie (idCategorie, nom, description) VALUES
(1, 'Vêtements',     'Habits et accessoires de mode'),
(2, 'Électronique',  'Téléphones, ordinateurs, accessoires'),
(3, 'Chaussures',    'Chaussures homme, femme, enfant'),
(4, 'Maison',        'Décoration et mobilier');

-- ------------------------------------------------------------
-- Variantes
-- ------------------------------------------------------------
INSERT INTO Variante (idVariante, nom, description) VALUES
(1, 'Couleur', 'Couleur du produit'),
(2, 'Taille',  'Taille du produit'),
(3, 'Stockage','Capacité de stockage'),
(4, 'Matière', 'Matière du produit');

-- ------------------------------------------------------------
-- CategorieVariante
-- ------------------------------------------------------------
INSERT INTO CategorieVariante (idCategorie, idVariante) VALUES
(1, 1), -- Vêtements → Couleur
(1, 2), -- Vêtements → Taille
(1, 4), -- Vêtements → Matière
(2, 1), -- Électronique → Couleur
(2, 3), -- Électronique → Stockage
(3, 1), -- Chaussures → Couleur
(3, 2); -- Chaussures → Taille

-- ------------------------------------------------------------
-- Produits
-- ------------------------------------------------------------
INSERT INTO Produit (idProduit, nom, description) VALUES
(1, 'T-shirt Basique',    'T-shirt en coton 100% bio'),
(2, 'Jean Slim',          'Jean coupe slim stretch'),
(3, 'Smartphone ProMax',  'Smartphone dernière génération'),
(4, 'Sneakers Classic',   'Chaussures de sport casual'),
(5, 'Vase Décoratif',     'Vase en céramique artisanal');

-- ------------------------------------------------------------
-- ProduitVarValeur
-- ------------------------------------------------------------
INSERT INTO ProduitVarValeur (idPVV, idProduit, idVariante, valeur) VALUES
-- T-shirt : Couleur
(1,  1, 1, 'Blanc'),
(2,  1, 1, 'Noir'),
(3,  1, 1, 'Rouge'),
-- T-shirt : Taille
(4,  1, 2, 'S'),
(5,  1, 2, 'M'),
(6,  1, 2, 'L'),
(7,  1, 2, 'XL'),
-- Jean : Couleur
(8,  2, 1, 'Bleu'),
(9,  2, 1, 'Noir'),
-- Jean : Taille
(10, 2, 2, '38'),
(11, 2, 2, '40'),
(12, 2, 2, '42'),
-- Smartphone : Couleur
(13, 3, 1, 'Noir'),
(14, 3, 1, 'Blanc'),
(15, 3, 1, 'Bleu'),
-- Smartphone : Stockage
(16, 3, 3, '128GB'),
(17, 3, 3, '256GB'),
(18, 3, 3, '512GB'),
-- Sneakers : Couleur
(19, 4, 1, 'Blanc'),
(20, 4, 1, 'Noir'),
-- Sneakers : Taille
(21, 4, 2, '40'),
(22, 4, 2, '42'),
(23, 4, 2, '44'),
-- Vase : Matière
(24, 5, 4, 'Céramique'),
(25, 5, 4, 'Verre');

-- ------------------------------------------------------------
-- SKU
-- ------------------------------------------------------------
INSERT INTO SKU (SKU, prix, quantite, image) VALUES
('TSHIRT-BLANC-M',      89.99,  50, 'tshirt_blanc.jpg'),
('TSHIRT-NOIR-L',       89.99,  30, 'tshirt_noir.jpg'),
('TSHIRT-ROUGE-S',      89.99,  20, 'tshirt_rouge.jpg'),
('JEAN-BLEU-40',       199.99,  25, 'jean_bleu.jpg'),
('JEAN-NOIR-42',       199.99,  15, 'jean_noir.jpg'),
('PHONE-NOIR-128',    2499.99,  10, 'phone_noir.jpg'),
('PHONE-BLANC-256',   2699.99,   8, 'phone_blanc.jpg'),
('PHONE-BLEU-512',    2899.99,   5, 'phone_bleu.jpg'),
('SNEAK-BLANC-42',     349.99,  40, 'sneak_blanc.jpg'),
('SNEAK-NOIR-44',      349.99,  35, 'sneak_noir.jpg'),
('VASE-CERAMIQUE',     129.99,  60, 'vase_ceram.jpg'),
('VASE-VERRE',         149.99,  45, 'vase_verre.jpg');

-- ------------------------------------------------------------
-- SKUVarValeur  (lien SKU ↔ ProduitVarValeur)
-- ------------------------------------------------------------
INSERT INTO SKUVarValeur (SKU, idPVV) VALUES
-- TSHIRT-BLANC-M → Couleur:Blanc + Taille:M
('TSHIRT-BLANC-M', 1),
('TSHIRT-BLANC-M', 5),
-- TSHIRT-NOIR-L → Couleur:Noir + Taille:L
('TSHIRT-NOIR-L',  2),
('TSHIRT-NOIR-L',  6),
-- TSHIRT-ROUGE-S → Couleur:Rouge + Taille:S
('TSHIRT-ROUGE-S', 3),
('TSHIRT-ROUGE-S', 4),
-- JEAN-BLEU-40 → Couleur:Bleu + Taille:40
('JEAN-BLEU-40',   8),
('JEAN-BLEU-40',   10),
-- JEAN-NOIR-42 → Couleur:Noir + Taille:42
('JEAN-NOIR-42',   9),
('JEAN-NOIR-42',   12),
-- PHONE-NOIR-128 → Couleur:Noir + Stockage:128GB
('PHONE-NOIR-128', 13),
('PHONE-NOIR-128', 16),
-- PHONE-BLANC-256 → Couleur:Blanc + Stockage:256GB
('PHONE-BLANC-256',14),
('PHONE-BLANC-256',17),
-- PHONE-BLEU-512 → Couleur:Bleu + Stockage:512GB
('PHONE-BLEU-512', 15),
('PHONE-BLEU-512', 18),
-- SNEAK-BLANC-42 → Couleur:Blanc + Taille:42
('SNEAK-BLANC-42', 19),
('SNEAK-BLANC-42', 22),
-- SNEAK-NOIR-44 → Couleur:Noir + Taille:44
('SNEAK-NOIR-44',  20),
('SNEAK-NOIR-44',  23),
-- VASE-CERAMIQUE → Matière:Céramique
('VASE-CERAMIQUE', 24),
-- VASE-VERRE → Matière:Verre
('VASE-VERRE',     25);

-- ------------------------------------------------------------
-- Commandes
-- ------------------------------------------------------------
INSERT INTO Commande (idCommande, IdClient, idAdresse, reference, statut, dateLivraisonPrevue, dateLivraisonReelle) VALUES
(1, 3, 1, 'CMD-20260301-A1B2', 'livrée',    '2026-03-05 12:00:00', '2026-03-04 10:30:00'),
(2, 3, 2, 'CMD-20260305-C3D4', 'expédiée',  '2026-03-20 12:00:00', NULL),
(3, 4, 3, 'CMD-20260308-E5F6', 'validée',   '2026-03-22 12:00:00', NULL),
(4, 5, 4, 'CMD-20260310-G7H8', 'en_attente', NULL,                 NULL),
(5, 6, 5, 'CMD-20260312-I9J0', 'livrée',    '2026-03-15 12:00:00', '2026-03-14 09:00:00');

-- ------------------------------------------------------------
-- Paiements
-- ------------------------------------------------------------
INSERT INTO Paiement (idPaiement, idCommande, idCarte, montant, statutPaiement, methodePaiement) VALUES
(1, 1, 1,    179.98, 'approuve', 'card'),
(2, 2, 2,   2499.99, 'approuve', 'card'),
(3, 3, 3,    199.99, 'approuve', 'card'),
(4, 4, NULL, 349.99, 'en_attente', 'cash'),
(5, 5, 5,    129.99, 'approuve', 'card');

-- ------------------------------------------------------------
-- LigneCommande
-- ------------------------------------------------------------
INSERT INTO LigneCommande (idCommande, SKU, quantite, prixAchat) VALUES
(1, 'TSHIRT-BLANC-M',  1,  89.99),
(1, 'TSHIRT-NOIR-L',   1,  89.99),
(2, 'PHONE-NOIR-128',  1, 2499.99),
(3, 'JEAN-BLEU-40',    1,  199.99),
(4, 'SNEAK-BLANC-42',  1,  349.99),
(5, 'VASE-CERAMIQUE',  1,  129.99);

-- ------------------------------------------------------------
-- Paniers
-- ------------------------------------------------------------
INSERT INTO Panier (idPanier, IdClient) VALUES
(1, 4),
(2, 7);

-- ------------------------------------------------------------
-- LignePanier
-- ------------------------------------------------------------
INSERT INTO LignePanier (idPanier, SKU, quantite) VALUES
(1, 'PHONE-BLANC-256', 1),
(1, 'TSHIRT-ROUGE-S',  2),
(2, 'VASE-VERRE',      1),
(2, 'SNEAK-NOIR-44',   1);

-- ------------------------------------------------------------
-- Avis
-- ------------------------------------------------------------
INSERT INTO Avis (idCommentaire, IdClient, idProduit, idCommande, contenu, evaluation) VALUES
(1, 3, 1, 1, 'Très bonne qualité, tissu doux et confortable.',     5),
(2, 3, 3, 2, 'Excellent smartphone, rapide et belle batterie.',    5),
(3, 4, 2, 3, 'Jean bien coupé mais un peu rigide au début.',       4),
(4, 6, 5, 5, 'Très beau vase, exactement comme sur la photo.',     5),
(5, 5, 4, NULL, 'Belles chaussures mais taille un peu petit.',     3);

-- ------------------------------------------------------------
-- Notifications
-- ------------------------------------------------------------
INSERT INTO Notification (idNotification, IdUtilisateur, contenu, statut) VALUES
(1, 3, 'Votre commande CMD-20260301-A1B2 a été livrée avec succès.',     'lu'),
(2, 3, 'Votre commande CMD-20260305-C3D4 a été expédiée.',               'non_lu'),
(3, 4, 'Votre commande CMD-20260308-E5F6 a été validée.',                'non_lu'),
(4, 5, 'Votre commande CMD-20260310-G7H8 est en attente de paiement.',   'non_lu'),
(5, 6, 'Votre commande CMD-20260312-I9J0 a été livrée avec succès.',     'lu'),
(6, 7, 'Bienvenue sur notre boutique ! Profitez de -10% sur votre 1ère commande.', 'non_lu');

SET FOREIGN_KEY_CHECKS = 1;
