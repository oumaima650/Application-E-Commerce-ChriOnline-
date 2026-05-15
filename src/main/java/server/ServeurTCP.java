package server;

import service.CleanupService;
//pour tls
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
/*
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
*/
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
    /* 
    */
    private SSLServerSocket serverSocket;
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
            
            SSLServerSocketFactory ssf = getSSLServerSocketFactory();
            int port = Integer.parseInt(config.getProperty("server.port", "8443"));
            serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
            
            // Forcer l'utilisation de TLS 1.3
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

            logger.info("Serveur sécurisé (TLS 1.3) lancé sur le port {}", port);

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

                    logger.info("ACCEPTED Nouvelle connexion TLS de {}", clientIp);

                    // hand off to Virtual Thread
                    virtualThreadExecutor.submit(() -> {
                        try {
                            new ClientHandler(clientSocket, securityManager).run();
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

    private SSLServerSocketFactory getSSLServerSocketFactory() throws Exception {
        String keystorePath = config.getProperty("server.keystore.path");
        String keystorePass = config.getProperty("server.keystore.password");
        String keystoreType = config.getProperty("server.keystore.type", "PKCS12");

        KeyStore ks = KeyStore.getInstance(keystoreType);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keystorePass.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext.getServerSocketFactory();
    }

    public static void main(String[] args) {
        new ServeurTCP().start();
    }
}

