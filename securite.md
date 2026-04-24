# Documentation de la Sécurité - ChriOnline 🛡️

Cette documentation détaille les couches de sécurité ajoutées à l'application e-commerce ChriOnline pour garantir la confidentialité, l'intégrité et la disponibilité des données des utilisateurs. Le modèle de sécurité est basé sur une approche de "Défense en Profondeur" (Defense in Depth) et s'articule autour d'un **Gestionnaire de Sécurité Centralisé (`SecurityManager`)**.


## 1. Chiffrement des Données en Transit (TLS 1.3)
**Quoi :** Utilisation exclusive du protocole **TLS 1.3** pour toutes les communications de bout en bout (TCP).
**Comment :** 
- Le serveur utilise un `SSLServerSocket` configuré strictement avec `TLSv1.3`.
- Un certificat chiffré (`PKCS12`) est utilisé pour authentifier le serveur.
**Pourquoi :** Empêche les attaques de type "Man-in-the-Middle", l'écoute illicite (sniffing) et l'usurpation d'adresse (IP Spoofing). Les mots de passe et les données bancaires ne circulent jamais en texte clair.

## 2. Hachage des Mots de Passe (SHA-256)
**Quoi :** Stockage sécurisé des identifiants via un mécanisme de hachage unidirectionnel.
**Comment :** Le mot de passe est haché en **SHA-256** combiné à un système de validation de force de mot de passe (PasswordService).
**Pourquoi :** Si la base de données venait à être compromise, les pirates obtiendraient des hashes illisibles, rendant les identifiants originaux impossibles à récupérer.

## 3. Authentification Stateless et Gestion des Sessions (JWT)
**Quoi :** Remplacement des anciens jetons en mémoire par des **Json Web Tokens (JWT) signés (Algorithme RS256)**.
**Comment :** 
- À la connexion, un `Access Token` (durée courte, 15 min) et un `Refresh Token` (durée longue, 7 jours) sont générés.
- Les JWT sont protégés par une paire de clés asymétriques RSA 2048-bits.
**Pourquoi :** Permet au serveur de valider cryptographiquement l'identité de l'utilisateur sans être altérable côté client. En cas de vol du JWT, l'expiration rapide (15 min) limite la fenêtre d'attaque.

## 4. Protection Anti-Rejeu (Replay Attack Protection)
**Quoi :** Prévention système contre la capture et la réutilisation répétée de requêtes valides.
**Comment :** 
- Chaque requête entrante est complétée par un **Nonce** (valeur unique) et un **Timestamp** (horodatage).
- Le `ReplayProtectionService` vérifie la fenêtre de validité (30 secondes) et s'assure qu'aucun nonce n'est traité deux fois.
**Pourquoi :** Même si un attaquant intercepte un transfert de fonds ou un panier, la re-soumettre provoquera un rejet immédiat "REPLAY_ATTACK_DETECTED".

## 5. Défense Anti-Bruteforce et Rate Limiting
**Quoi :** Verrouillage automatique face aux attaques de mots de passe par dictionnaire ou Credential Stuffing.
**Comment :** 
- Un composant `LoginAttemptLimitService` scrute en temps réel le taux d'erreur par **Adresse IP** et par **E-mail**.
- Application de blocages exponentiels à la volée en cas de violations successives et traçage dans l'audit de sécurité.
- Notifications par e-mail en cas de suspension du compte.
**Pourquoi :** Rend le piratage massif ou robotisé technologiquement et temporellement inefficace.

## 6. Détection Antibot (reCAPTCHA)
**Quoi :** Séparation des vrais utilisateurs humains des moteurs automatisés agressifs.
**Comment :** Lors de l'inscription ("Signup") ou de l'authentification ("Login"), l'architecture intègre un service de vérification `RecaptchaService`.
**Pourquoi :** Évite la pollution de la base de données par de faux comptes et l'inondation de requêtes spam.

## 7. Rôle et Ségrégation des Privilèges
**Quoi :** Séparation étanche entre les données Client et Administrateur.
**Comment :** Le rôle de l'utilisateur n'est pas simplement injecté ; il est **scellé** dans la charge utile du JWT. Le SecurityManager analyse cette empreinte inaltérable pour le contrôle d'accès.
**Pourquoi :** Les tentatives d'escalade de privilèges (Horizontal/Vertical) sont étouffées, un simple client ne pouvant pas altérer les APIs administratives.

## 8. Architecture Anti-Déni de Service (Virtual Threads)
**Quoi :** Résilience face à un trafic inattendu (DDoS partiel).
**Comment :** Déploiement basé sur les **Virtual Threads**. Le Handler de connexion alloue des threads ultra-légers pour traiter la charge.
**Pourquoi :** Les connexions malveillantes ouvertes au ralenti (Slowloris) ne peuvent plus épuiser la piscine (pool) de tâches principale. Le système survit sans ralentissement à plusieurs milliers d'états d'attente concurrents.

## 9. Protection contre la Désérialisation Malveillante (ObjectInputFilter)
**Quoi :** Prévention contre les injections de commandes réseau par désérialisation Java (RCE).
**Comment :** Utilisation d'un `ObjectInputFilter` sur le flux d'entrée (`ObjectInputStream`) du `ClientHandler`. 
- **Whitelist stricte :** Seuls les packages `shared`, `model`, `java.lang`, `java.util` et `java.time` sont autorisés.
- Tout autre objet (gadgets malveillants, librairies tierces non autorisées) est rejeté immédiatement avant instanciation.
**Pourquoi :** Élimine le vecteur d'attaque le plus critique sur les sockets Java, garantissant que même un attaquant ne peut pas exécuter de code arbitraire sur le serveur via des payloads sérialisés.

## 10. Inscription en Deux Étapes et Activation par Email
**Quoi :** Obligation pour chaque nouvel utilisateur de valider son identité avant toute interaction.
**Comment :** 
- Un compte créé est initialement marqué comme `EN_ATTENTE`.
- Un code de vérification **alphanumérique à 6 caractères** (haute entropie) est envoyé. Seule la validation de ce code bascule le compte vers le statut `ACTIF`.
**Pourquoi :** Empêche la création de comptes "fantômes" avec des e-mails volés ou inexistants, garantissant que chaque utilisateur dispose d'un canal de communication valide.

## 11. Inscription Atomique (Self-Cleaning Logic)
**Quoi :** Nettoyage automatique des tentatives d'inscription échouées.
**Comment :** Si l'envoi de l'e-mail de vérification initial échoue (problème SMTP, adresse invalide), le système supprime immédiatement l'enregistrement `Utilisateur`.
**Pourquoi :** Maintient l'intégrité de la base de données en évitant les enregistrements orphelins et garantit que l'adresse e-mail reste disponible pour une nouvelle tentative.

## 12. Validation de Mot de Passe Sensible à l'Identité
**Quoi :** Politique de mot de passe intelligente empêchant l'utilisation d'informations personnelles (PII).
**Comment :** Le `PasswordService` compare le mot de passe choisi avec le nom, le prénom, l'e-mail et la date de naissance (composants du jour/mois/année).
**Pourquoi :** Contre les attaques par ingénierie sociale où les pirates tentent de deviner des mots de passe basés sur les données personnelles de la victime.

## 13. Résilience et Sécurité du Canal SMTP
**Quoi :** Fiabilisation de la livraison des codes de sécurité (OTP).
**Comment :** 
- Implémentation de timeouts (connexion, lecture, écriture) pour éviter les blocages réseaux.
- Système de **retry automatique** (3 tentatives avec délai de 2s) pour surmonter les erreurs serveurs Gmail (421 Busy).
**Pourquoi :** Garantit qu'un utilisateur n'est jamais bloqué hors de son compte par une instabilité réseau momentanée.


## 14. Validation de l'Âge Légal
**Quoi :** Contrôle de conformité dès l'inscription.
**Comment :** Calcul automatique de l'âge à partir de la date de naissance avec rejet immédiat des inscriptions pour les moins de 16 ans.
**Pourquoi :** Conformité aux réglementations sur la protection des données des mineurs et limitation des risques juridiques.

## 15. Nettoyage Automatique des Comptes Abandonnés (Ghost Account Cleaner)
**Quoi :** Suppression périodique et automatique des comptes non vérifiés qui encombrent la base de données.
**Comment :**
- Un service `CleanupService` démarre en arrière-plan au lancement du serveur, sur un **thread daemon** (ne bloque jamais l'arrêt du serveur).
- Il s'exécute **immédiatement au démarrage**, puis toutes les **24 heures**.
- Il cible uniquement les comptes avec le statut `EN_ATTENTE` créés il y a plus de **24 heures** via une requête `DELETE ... JOIN` sûre et précise (ne touche jamais les comptes `ACTIF` ou `BANNI`).
- Chaque exécution est consignée dans les logs du serveur avec le nombre de comptes supprimés.
**Pourquoi :** Empêche l'accumulation de comptes "fantômes" créés par des bots ou par des utilisateurs qui n'ont jamais terminé leur inscription, maintenant ainsi une base de données propre et limitant la surface d'attaque (exemple : énumération d'e-mails).


## 16. Whitelist IP Administrateur (Admin IP Whitelist)
**Quoi :** Restriction d'accès aux fonctions administratives aux seules adresses IP de confiance pré-configurées.

**Comment :**
- Les IPs autorisées sont définies dans `config.properties` via la clé `security.admin.allowed_ips` (support des adresses exactes et des jokers `*`, ex: `192.168.1.*`).
- La vérification s'effectue en **deux points complémentaires** :
  1. **Lors du Login :** Dans `AuthService.login()`, après récupération de l'utilisateur, si le rôle est `ADMIN`, son IP est vérifiée avant toute émission de JWT. Un accès depuis une IP non autorisée est rejeté avec le code `IP_NOT_AUTHORIZED` et journalisé en `ERROR`.
  2. **Sur chaque requête ADMIN\_ :** Dans `SecurityManager.validateRequest()`, toute requête dont le type commence par `ADMIN_` est vérifiée en **première priorité**, avant le JWT, avant le 2FA et avant tout autre contrôle.
- L'IP utilisée est **toujours celle du socket TCP réel** (`socket.getInetAddress().getHostAddress()`). Les headers HTTP (`X-Forwarded-For`) ne sont jamais lus, les rendant inopérants pour contourner ce contrôle.
- Chaque tentative bloquée est journalisée via Log4j2 (`logger.error`) avec l'IP, l'email et le type de requête, constituant une piste d'audit complète.
- Côté client (JavaFX), la méthode statique `SessionManager.handleIpNotAuthorized()` gère globalement ce cas : elle affiche une alerte, efface la session locale et redirige vers la page de connexion.

**Pourquoi :** Constitue une couche de **Défense en Profondeur** critique. Même si un attaquant parvenait à voler un JWT ou à compromettre les identifiants d'un administrateur, il resterait bloqué s'il n'opère pas depuis une IP de confiance. Réduit drastiquement la surface d'attaque des fonctions les plus sensibles de l'application.

## 17. Limitation des Connexions TCP et Protection DoS
**Quoi :** Protection au niveau de la couche transport (Socket) contre l'épuisement des ressources par saturation de connexions.

**Comment :**
- Le serveur impose trois mécanismes de protection via le `TcpDosProtectionService` et le `ClientHandler` :
    1. **Limite Globale :** Maximum de **10** connexions TCP simultanées sur tout le serveur.
    2. **Limite par IP :** Maximum de **3** connexions TCP simultanées pour une même adresse IP.
    3. **Timeout d'Inactivité :** Toute connexion n'ayant envoyé aucune requête pendant **5 minutes** est automatiquement coupée (`SocketTimeoutException`).
- Si un seuil est atteint, le serveur rejette immédiatement la connexion (`socket.close()`) avant même d'allouer un thread, économisant ainsi la RAM et le CPU.

- Chaque connexion acceptée subit une simulation de délai de 10 secondes (TP3) pour tester la robustesse du système.

**Gestion des Sessions vs Connexions :**
- **Connexion (Temporaire) :** Le "tuyau" physique entre le client et le serveur. Il est fermé automatiquement en cas d'inactivité (Timeout) pour libérer de la place.
- **Session (Persistante) :** L'identité de l'utilisateur stockée en base de données (JWT). Même si le serveur "raccroche" la connexion TCP pour économiser des ressources, la session reste vivante.
- **Auto-Réparation :** Le code client (`ClientSocket.java`) détecte automatiquement si la connexion est fermée et en ouvre une nouvelle de manière transparente dès que l'utilisateur effectue une action, tout en transmettant son jeton de session existant.

**Pourquoi :** Garantit que même si un attaquant tente d'ouvrir des milliers de connexions "fantômes", il ne peut pas saturer le serveur. La limite par IP empêche un seul pirate de monopoliser toutes les places, laissant toujours des "slots" libres pour les clients légitimes.

## 18. Orchestration Centrale (Security Facade Pattern)
**Quoi :** Centralisation de tous les services critiques de sécurité.

**Comment :**
- Le `SecurityManager` agit comme une **façade unique** pour l'application.
- Il orchestre les services suivants :
    - **Anti-DoS TCP** (`TcpDosProtectionService`)
    - **Anti-Flood UDP** (`UdpSecurityService`)
    - **Double Authentification** (`TwoFactorAuthService`)
    - **Réinitialisation de Mot de Passe** (`PasswordResetService`)
    - **Protection Anti-Bot** (`RecaptchaService`)
    - **Protection Anti-Rejeu** (`ReplayProtectionService`)
    - **Limitation Brute Force** (`LoginAttemptLimitService`)

**Pourquoi :** Cette architecture garantit que l'application reste modulaire et facile à auditer. Tous les événements de sécurité passent par un point unique, permettant une journalisation cohérente et empêchant les services métiers (`AuthService`, `ClientHandler`) d'être pollués par de la logique de sécurité complexe.

## 19. Chiffrement des Données au Repos (Server-Side Storage Encryption)
**Quoi :** Protection intégrale des données sensibles (PII) stockées dans la base de données MySQL.
**Comment :** 
- Utilisation du `StorageEncryptionService` basé sur l'algorithme **AES-256 GCM**.
- Les clés de chiffrement (DEK - Data Encryption Key) sont chargées depuis un **KeyStore Java (`.p12`)** protégé par mot de passe et ne sont jamais stockées en clair.
- **Données concernées :** Nom, Prénom, Téléphone, Adresse, Date de Naissance, Numéros de carte bancaire.
**Pourquoi :** Garantit la confidentialité totale des données même en cas de dump complet de la base de données ou d'accès physique non autorisé aux fichiers de stockage.

## 20. Recherche Déterministe Sécurisée (Searchable Encryption)
**Quoi :** Capacité à effectuer des recherches SQL précises sur des colonnes chiffrées sans compromettre la sécurité.
**Comment :** 
- Utilisation d'un **chiffrement déterministe** (IV fixe dérivé de la StorageKey) pour les champs indexables (`nom`, `prenom`, `telephone`).
- Le serveur transforme la requête de recherche en ciphertext avant de l'envoyer à MySQL (`SELECT ... WHERE nom = 'ENCRYPTED_VALUE'`).
- Les données non indexables (ex: adresse) utilisent un **chiffrement probabiliste** (IV aléatoire) pour une sécurité maximale.
**Pourquoi :** Concilie le besoin opérationnel de recherche administrative avec l'exigence de confidentialité stricte du RGPD.

## 21. Chiffrement Applicatif de la Couche Réseau (Double Protection)
**Quoi :** Chiffrement des données sensibles directement au niveau de la couche applicative, avant même l'envoi sur le socket TLS.
**Comment :** 
- Chaque champ sensible dans les objets `Requete` et `Reponse` est chiffré individuellement en **AES-GCM** avec la clé de session établie lors du Handshake RSA.
- Le jeton JWT de session est lui-même chiffré avant transit.
**Pourquoi :** Ajoute une couche de "Défense en Profondeur". Même en cas de compromission théorique de la couche TLS 1.3, les données extraites resteraient chiffrées par une clé de session unique et volatile.

## 22. Audit de Sécurité et Traçabilité Cryptographique
**Quoi :** Journalisation systématique des opérations critiques de sécurité.
**Comment :** 
- Utilisation du tag **`[AUDIT SECU]`** dans les logs (via Log4j2).
- Tracement du chargement des clés, des opérations de chiffrement/déchiffrement et des échecs de validation de tokens.
- Affichage des empreintes (fingerprints) de clés pour vérifier l'intégrité du matériel cryptographique au démarrage du serveur.
**Pourquoi :** Permet une détection rapide des anomalies et facilite les audits de conformité en prouvant que les données sont effectivement protégées avant chaque écriture en base de données.

---
*Dernière mise à jour : Avril 2026 - Migration vers Storage Encryption*


