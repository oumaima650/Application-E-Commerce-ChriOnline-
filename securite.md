# Documentation de la Sécurité - ChriOnline 🛡️

Cette documentation détaille les couches de sécurité ajoutées à l'application e-commerce ChriOnline pour garantir la confidentialité et l'intégrité des données des utilisateurs.

## 1. Chiffrement des Données en Transit (TLS 1.3)
**Quoi :** Utilisation du protocole **TLS 1.3** pour toutes les communications TCP.
**Comment :** 
- Le serveur utilise un `SSLServerSocket` configuré uniquement avec `TLSv1.3`.
- Le client utilise un `SSLSocket` avec le même protocole.
- Un certificat auto-signé (PKCS12) est utilisé pour le développement.
**Pourquoi :** Empêche les attaques de type "Man-in-the-Middle". Sans TLS, les mots de passe et les données bancaires circuleraient en texte clair sur le réseau, lisibles par n'importe qui.

## 2. Hachage des Mots de Passe (SHA-256)
**Quoi :** Chiffrement irréversible des mots de passe.
**Comment :** Le client hache le mot de passe avec l'algorithme **SHA-256** avant même de l'envoyer au serveur. Le serveur ne stocke que ce "hash".
**Pourquoi :** Si la base de données est compromise, les pirates ne pourront pas lire les mots de passe réels des utilisateurs.

## 3. Gestion des Sessions Sécurisée
**Quoi :** Système de jetons (Tokens) pour identifier les utilisateurs connectés.
**Comment :** 
- Un **Token UUID** unique est généré à chaque connexion réussie.
- Le serveur garde en mémoire (`SessionManager`) le lien entre le token et l'utilisateur.
- Le mot de passe de l'utilisateur est **supprimé** de l'objet en mémoire (mis à `null`) dès que la session est créée.
**Pourquoi :** Évite de renvoyer le mot de passe à chaque requête. Le token sert de "preuve" d'identité temporaire.

## 4. Expiration et Nettoyage de Session
**Quoi :** Durée de vie limitée des sessions.
**Comment :** 
- **Durée** : Les sessions expirent après **30 minutes** d'inactivité.
- **Sliding Window** : Chaque requête valide réinitialise le délai (la session reste active tant que l'utilisateur travaille).
- **Auto-Cleanup** : Un thread en arrière-plan nettoie les sessions mortes de la mémoire toutes les 10 minutes.
**Pourquoi :** Limite l'impact si un utilisateur oublie de se déconnecter ou si un token est volé.

## 5. Isolation des Rôles
**Quoi :** Distinction claire entre `Client` et `Administrateur`.
**Comment :** Le type d'utilisateur est stocké dans la session et vérifié par le serveur avant chaque action sensible.
**Pourquoi :** Garantit qu'un client ne peut pas accéder aux interfaces d'administration en manipulant les requêtes.

## 6. Architecture de Performance & Sécurité (Virtual Threads)
**Quoi :** Utilisation des `Virtual Threads` (Java 21+).
**Comment :** Chaque connexion client est gérée par un thread virtuel ultra-léger.
**Pourquoi :** Empêche les attaques par déni de service (DoS) par épuisement de threads, le serveur pouvant gérer des milliers de connexions simultanées sans bloquer.

---
*Dernière mise à jour : 26 Mars 2026*
