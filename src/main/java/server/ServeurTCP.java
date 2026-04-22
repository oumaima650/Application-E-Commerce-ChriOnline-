package server;

import service.CleanupService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.SecurityManager;

public class ServeurTCP {

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private final Properties config = new Properties();
    private ServerSocket serverSocket;
    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final CleanupService cleanupService = new CleanupService();
    private final SecurityManager securityManager = new SecurityManager();
    private static final Logger logger = LogManager.getLogger(ServeurTCP.class);

    private final ConcurrentHashMap<String, AtomicInteger> ipConnections = new ConcurrentHashMap<>();

    protected boolean checkIpLimit(String ip) {
        return securityManager.getTcpDosService().checkIpLimit(ip);
    }

    public void start() {
        try {
            loadConfig();
            loadKeys();

            int port = Integer.parseInt(config.getProperty("server.port", "8443"));
            serverSocket = new ServerSocket(port);

            logger.info("Serveur sécurisé (Hybride AES/RSA) lancé sur le port {}", port);

            // Start the background cleanup task
            cleanupService.start();

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    String clientIp = clientSocket.getInetAddress().getHostAddress();

                    if (!securityManager.canAcceptTcpConnection(clientIp)) {
                        clientSocket.close();
                        continue;
                    }

                    logger.info("ACCEPTED Nouvelle connexion de {}", clientIp);

                    // hand off to Virtual Thread
                    virtualThreadExecutor.submit(() -> {
                        try {
                            // TP3 - Simulate connection logic holdup for SYN Flood DoS
                            // Thread.sleep(10000); // Décommenter si DoS nécessaire
                            new ClientHandler(clientSocket, securityManager, serverPrivateKey, serverPublicKey).run();
                        } catch (Exception e) {
                            logger.error("Erreur dans le Thread du client : {}", e.getMessage(), e);
                        } finally {
                            securityManager.releaseTcpConnection(clientIp);
                        }
                    });

                } catch (Exception e) {
                    if (!serverSocket.isClosed()) {
                        logger.error("Erreur lors de l'accept : {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.fatal("Erreur critique au démarrage : {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void loadConfig() throws Exception {
        try (InputStream input = new FileInputStream(CONFIG_PATH)) {
            config.load(input);
        }
    }

    private void loadKeys() throws Exception {
        String keystorePath = config.getProperty("server.keystore.path");
        String keystorePass = config.getProperty("server.keystore.password");
        String keystoreType = config.getProperty("server.keystore.type", "PKCS12");

        KeyStore ks = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }

        // On prend le premier alias qui existe dans le keystore
        String alias = ks.aliases().nextElement();
        serverPrivateKey = (PrivateKey) ks.getKey(alias, keystorePass.toCharArray());
        Certificate cert = ks.getCertificate(alias);
        serverPublicKey = cert.getPublicKey();
        
        logger.info("Clés RSA (Privée/Publique) chargées depuis le Keystore : {}", keystorePath);
    }

    public static void main(String[] args) {
        new ServeurTCP().start();
    }
}
