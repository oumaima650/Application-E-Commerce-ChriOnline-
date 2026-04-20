package server;

import service.CleanupService;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
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
            SSLContext sslContext = createSSLContext();
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

            int port = Integer.parseInt(config.getProperty("server.port", "8443"));
            serverSocket = (SSLServerSocket) factory.createServerSocket(port);

            // ENFORCE TLS 1.3 ONLY
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            
            logger.info("Serveur sécurisé lancé sur le port {}", port);
            logger.info("Protocole actif : TLSv1.3 uniquement");

            // Start the background cleanup task
            cleanupService.start();


            while (!serverSocket.isClosed()) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    
                    // Enforce TLS 1.3 on the socket level as well
                    clientSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    
                    if (!securityManager.canAcceptTcpConnection(clientIp)) {
                        clientSocket.close();
                        continue;
                    }

                    logger.info("ACCEPTED Nouvelle connexion SSL de {}", clientIp);

                    // hand off to Virtual Thread
                    virtualThreadExecutor.submit(() -> {
                        try {
                            // TP3 - Simulate connection logic holdup for SYN Flood DoS
                            Thread.sleep(10000);
                            new ClientHandler(clientSocket, securityManager).run();
                        } catch (Exception e) {
                            logger.error("Erreur dans le Thread du client : {}", e.getMessage(), e);
                        } finally {
                            securityManager.releaseTcpConnection(clientIp);
                        }
                    });


                } catch (SSLHandshakeException e) {
                    logger.warn("Échec du Handshake SSL : {}", e.getMessage());
                    // Log and continue, don't crash
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

    private SSLContext createSSLContext() throws Exception {
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

        return sslContext;
    }

    public static void main(String[] args) {
        new ServeurTCP().start();
    }
}
