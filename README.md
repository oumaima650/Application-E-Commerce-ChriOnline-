# ChriOnline E-Commerce Application

[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Client--Server-green.svg)](#project-scope)

**ChriOnline** is a robust Java-based e-commerce platform designed with a strong focus on **Network Security** and **Scalability**. It demonstrates a complex multi-client/server architecture using native TCP and UDP sockets, implementing modern security protocols to protect sensitive user data and prevent common network attacks.

---

## Key Features

- **Full E-Commerce Flow**: Product browsing, cart management, and secure order processing.
- **Advanced Authentication**: Secure login system with refresh token mechanism.
- **Real-time Notifications**: Fast UDP-based push notifications for order updates and alerts.
- **Admin Dashboard**: Dedicated administrative interface for managing the catalog and monitoring security.
- **Modern UI**: Responsive JavaFX-based desktop interface.

---

## Security Implementation (Core Focus)

This project prioritizes defense-in-depth through several security layers:

### 1. Encrypted Communication (TLS 1.3)
All critical TCP traffic (Authentication, Payments, Orders) is encrypted using **TLSv1.3**, ensuring confidentiality and integrity.
- **Client/Server Handshake**: Uses custom SSLSocketFactory with strictly enforced TLS 1.3.
- **Certificate Management**: Uses local keystores for secure identity verification.

### 2. Replay Attack Protection
Prevents attackers from intercepting and re-sending valid requests.
- **Nonce & Timestamp**: Every request includes a unique UUID (nonce) and a millisecond timestamp.
- **Server Validation**: The `ReplayProtectionService` maintains a 30-second window to verify uniqueness and freshness.

### 3. UDP Flood & Volumetric Protection
Safeguards the notification system against Denial of Service (DoS) attacks.
- **Rate Limiting**: Limits clients to **3 packets per second**.
- **Payload Filtering**: Maximum payload size restricted to **1 KB** to prevent amplification.

### 4. Brute Force & Anti-Spam
- **Login Throttling**: Limits failed attempts by both **IP address** and **User Email**.
- **Active Defense**: Automatically blocks suspicious actors via the `SecurityManager` facade.

### 5. IP Whitelisting
- **Admin Security**: Critical administrative endpoints are restricted to specific authorized IPs/Subnets.

---

## Technical Stack

- **Core**: Java 17+
- **Network**: Native TCP/UDP Sockets (Socket, ServerSocket, DatagramSocket)
- **Encryption**: TLS 1.3 (JSSE)
- **Database**: MySQL 8.0+ / JDBC
- **Logging**: Log4j2 for security auditing
- **UI Framework**: JavaFX

---

## Project Structure

```text
├── src/main/java
│   ├── client          # Logic for socket connection and JavaFX App
│   ├── server          # Multi-threaded TCP/UDP server implementation
│   ├── shared          # Data Transfer Objects (Requete, Reponse, Session)
│   ├── service         # SecurityManager, Replay & UDP protection services
│   ├── dao             # Data Access Layer (JDBC / Singleton)
│   ├── model           # Domain entities (User, Product, Order)
│   └── ui              # JavaFX Controllers & UI Logic
└── src/main/resources
    └── fxml            # Visual layouts and styles
```

---

## Setup & Installation

### 1. Database Configuration
1. Ensure **MySQL** is running.
2. Create a database named `chri_online`.
3. Import the `db_setup.sql` file provided in the root directory.
4. Update credentials in `src/main/java/dao/ConnexionBDD.java`:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/chri_online";
   private static final String USER = "root";
   private static final String PASSWORD = "";
   ```

### 2. Running the Application
Always start the **Server** before the client.

- **Start Server**: Run `src/main/java/server/ServeurTCP.java`
- **Start Client**: Run `src/main/java/client/ClientApp.java`

---

## Security Audit & Logs
The system generates detailed logs for security events. Check the console or log files for:
- `ALERTE CRITIQUE`: Potential Replay Attacks.
- `DÉFENSE ACTIVE`: UDP Flood or Brute Force blocks.
- `ACCÈS ADMIN REJETÉ`: Unauthorized IP attempts.

---
*Projet de Sécurité Informatique - Application E-Commerce Sécurisée*

