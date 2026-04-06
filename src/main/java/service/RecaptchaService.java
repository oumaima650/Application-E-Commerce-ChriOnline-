package service;

import server.utils.ConfigLoader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.google.gson.Gson;

public class RecaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private final String secretKey;
    private final Gson gson = new Gson();

    public RecaptchaService() {
        this.secretKey = ConfigLoader.getProperty("recaptcha.secret_key");
    }

    /**
     * Vérifie le jeton reCAPTCHA auprès de Google.
     * @param responseToken Le jeton reçu du client.
     * @return true si la vérification réussit, false sinon.
     */
    public boolean verify(String responseToken) {
        if (responseToken == null || responseToken.trim().isEmpty()) {
            System.err.println("[RecaptchaService] Jeton reCAPTCHA vide ou nul.");
            return false;
        }

        try {
            URL url = java.net.URI.create(VERIFY_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String postParams = "secret=" + java.net.URLEncoder.encode(secretKey, "UTF-8") 
                              + "&response=" + java.net.URLEncoder.encode(responseToken, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postParams.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("[RecaptchaService] Erreur de réponse Google : " + responseCode);
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                Map<String, Object> result = gson.fromJson(response.toString(), Map.class);
                Boolean success = (Boolean) result.get("success");
                
                if (success != null && success) {
                    System.out.println("[RecaptchaService] Vérification réussie.");
                    return true;
                } else {
                    System.err.println("[RecaptchaService] Échec de la vérification : " + result.get("error-codes"));
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("[RecaptchaService] Erreur lors de la vérification : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
