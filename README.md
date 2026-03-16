# ChriOnline E-Commerce Application

This project is a Java-based e-commerce application designed to demonstrate a multi-client server architecture using native TCP/UDP sockets. It handles core e-commerce processes including user authentication, product catalog management, cart handling, and order processing.

## Project Scope

The application follows a client-server model where:
- The Client provides the user interface (Console or JavaFX) and communicates with the Server via Sockets.
- The Server handles business logic, security, and database interactions.
- TCP Sockets are used for critical operations requiring high reliability (Authentication, Orders, Payments).
- UDP Sockets are used for non-critical fast messages such as notifications.

## Technical Stack

- Language: Java
- Communication: TCP (Socket, ServerSocket) and UDP (DatagramSocket)
- Database: MySQL
- Access Layer: JDBC with Singleton connection pattern
- Build Tool: Maven

## Project Structure

- src/main/java/client: Client-side application logic and socket connection.
- src/main/java/server: Multi-threaded server implementation and request handling.
- src/main/java/model: Data entities (User, Product, Order, etc.).
- src/main/java/dao: Data Access Objects for database interaction.
- src/main/java/shared: Common objects sent between client and server (Requete, Reponse).
- src/main/java/service: Server-side business logic services.
- src/main/java/ui: UI controllers for the desktop application.

## Database Setup

A comprehensive database script is provided in the root directory.

File: db_setup.sql

This script initializes the 'chri_online' database with the following tables:
- Utilisateur / Client / Admin (Inheritance structure)
- Categorie / Variante / Produit / SKU (Product catalog with variations)
- Commande / LigneCommande / Paiement
- Panier / LignePanier
- Avis (Product reviews)
- Notification

To initialize the database:
1. Ensure a MySQL server is running (e.g., via XAMPP or WAMP).
2. Create the database 'chri_online'.
3. Import the db_setup.sql file.

## How to Start

### Server
The server must be started first to listen for incoming client connections.
Main class: src/main/java/server/ServeurTCP.java

### Client
Once the server is running, instances of the client can be launched.
Main class: src/main/java/client/ClientApp.java

## Configuration

The database connection parameters are defined in src/main/java/dao/ConnexionBDD.java.
Default settings:
- URL: jdbc:mysql://localhost:3306/chri_online
- User: root
- Password: (empty)
