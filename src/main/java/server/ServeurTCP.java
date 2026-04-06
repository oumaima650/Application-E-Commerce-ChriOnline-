package server;

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


public class ServeurTCP {

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private final Properties config = new Properties();
    private SSLServerSocket serverSocket;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void start() {
        try {
            loadConfig();
            SSLContext sslContext = createSSLContext();
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

            int port = Integer.parseInt(config.getProperty("server.port", "8443"));
            serverSocket = (SSLServerSocket) factory.createServerSocket(port);

            // ENFORCE TLS 1.3 ONLY
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            
            System.out.println("[TLSServer] Serveur sécurisé lancé sur le port " + port);
            System.out.println("[TLSServer] Protocole actif : TLSv1.3 uniquement");

            while (!serverSocket.isClosed()) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    
                    // Enforce TLS 1.3 on the socket level as well
                    clientSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    
                    System.out.println("[" + timestamp + "] [TLSServer] Nouvelle connexion SSL de " + clientIp);

                    // hand off to Virtual Thread
                    virtualThreadExecutor.submit(new ClientHandler(clientSocket));

                } catch (SSLHandshakeException e) {
                    System.err.println("[TLSServer] Échec du Handshake SSL : " + e.getMessage());
                    // Log and continue, don't crash
                } catch (Exception e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("[TLSServer] Erreur lors de l'accept : " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TLSServer] Erreur critique au démarrage : " + e.getMessage());
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
