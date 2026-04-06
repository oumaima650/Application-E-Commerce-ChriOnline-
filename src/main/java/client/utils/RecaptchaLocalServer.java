package client.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Lightweight embedded HTTP server that serves recaptcha.html on localhost
 * so Google reCAPTCHA's domain validation passes.
 */
public class RecaptchaLocalServer {

    private static HttpServer server;
    private static int port = -1;

    public static void start() {
        if (server != null) return; // already running

        try {
            port = findFreePort();
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);

            server.createContext("/recaptcha.html", RecaptchaLocalServer::handleRecaptcha);
            server.setExecutor(null);
            server.start();

            System.out.println("[RecaptchaLocalServer] Listening on http://localhost:" + port + "/recaptcha.html");

            // Shut down cleanly when JVM exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (server != null) server.stop(0);
            }));

        } catch (IOException e) {
            System.err.println("[RecaptchaLocalServer] Failed to start: " + e.getMessage());
        }
    }

    public static String getUrl() {
        if (port == -1) return null;
        return "http://localhost:" + port + "/recaptcha.html";
    }

    private static void handleRecaptcha(HttpExchange exchange) throws IOException {
        InputStream is = RecaptchaLocalServer.class.getResourceAsStream("/html/recaptcha.html");
        if (is == null) {
            String err = "recaptcha.html not found";
            exchange.sendResponseHeaders(404, err.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err.getBytes());
            }
            return;
        }

        byte[] bytes = is.readAllBytes();
        is.close();

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        // Allow CORS for WebView
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
