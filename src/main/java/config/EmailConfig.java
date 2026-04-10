package config;

import java.io.InputStream;
import java.util.Properties;

public class EmailConfig {
    
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = EmailConfig.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (input == null) {
                System.err.println("[EmailConfig] Impossible de trouver le fichier mail.properties");
            } else {
                properties.load(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getEmailUsername() {
        return properties.getProperty("mail.user", "");
    }

    public static String getEmailPassword() {
        return properties.getProperty("mail.password", "");
    }

    public static Properties getSmtpProperties() {
        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth", properties.getProperty("mail.smtp.auth", "true"));
        smtpProps.put("mail.smtp.starttls.enable", properties.getProperty("mail.smtp.starttls.enable", "true"));
        smtpProps.put("mail.smtp.host", properties.getProperty("mail.smtp.host", "smtp.gmail.com"));
        smtpProps.put("mail.smtp.port", properties.getProperty("mail.smtp.port", "587"));
        smtpProps.put("mail.smtp.ssl.trust", properties.getProperty("mail.smtp.ssl.trust", "smtp.gmail.com"));
        
        // Timeouts to prevent hanging
        smtpProps.put("mail.smtp.connectiontimeout", "5000"); // 5s to connect
        smtpProps.put("mail.smtp.timeout", "5000");           // 5s to read
        smtpProps.put("mail.smtp.writetimeout", "5000");      // 5s to write
        
        return smtpProps;
    }
}
