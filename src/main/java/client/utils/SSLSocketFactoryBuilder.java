package client.utils;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;

/**
 * Utility to create secure SSLSockets for the client using TLS 1.3.
 */
public class SSLSocketFactoryBuilder {

    private static final String CONFIG_PATH = "src/main/resources/config.properties";

    public static SSLSocketFactory build() throws Exception {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_PATH)) {
            config.load(input);
        }

        String truststorePath = config.getProperty("server.keystore.path");
        String truststorePass = config.getProperty("server.keystore.password");
        String truststoreType = config.getProperty("server.keystore.type", "PKCS12");

        // For development, we trust the server's certificate by loading its keystore as a truststore
        KeyStore ts = KeyStore.getInstance(truststoreType);
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            ts.load(fis, truststorePass.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }
}
