# Documentation de la Sécurité - ChriOnline 🛡️

Cette documentation détaille les couches de sécurité ajoutées à l'application e-commerce ChriOnline pour garantir la confidentialité, l'intégrité et la disponibilité des données des utilisateurs. Le modèle de sécurité est basé sur une approche de "Défense en Profondeur" (Defense in Depth).

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

---
*Dernière mise à jour : Avril 2026*
