package service;

import config.EmailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private Session getEmailSession() {
        Properties props = EmailConfig.getSmtpProperties();
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailConfig.getEmailUsername(), EmailConfig.getEmailPassword());
            }
        });
    }

    public boolean sendEmail(String toAddress, String subject, String messageContent) {
        return send(toAddress, subject, messageContent, false);
    }

    public boolean sendHtmlEmail(String toAddress, String subject, String htmlContent) {
        return send(toAddress, subject, htmlContent, true);
    }

    private boolean send(String toAddress, String subject, String content, boolean isHtml) {
        int maxAttempts = 3;
        int attempt = 0;
        Exception lastEx = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                Session session = getEmailSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EmailConfig.getEmailUsername()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
                message.setSubject(subject);
                
                if (isHtml) {
                    message.setContent(content, "text/html; charset=utf-8");
                } else {
                    message.setText(content);
                }

                Transport.send(message);
                System.out.println("[EmailService] Email " + (isHtml ? "HTML" : "TEXT") + " envoyé avec succès à " + toAddress + " (Tentative " + attempt + ")");
                return true;
            } catch (MessagingException e) {
                lastEx = e;
                System.err.println("[EmailService] Échec tentative " + attempt + "/" + maxAttempts + " pour " + toAddress + " : " + e.getMessage());
                
                // Only retry for temporary errors (busy, throttled, timeout)
                String error = e.getMessage().toLowerCase();
                if (error.contains("busy") || error.contains("421") || error.contains("try again later") || error.contains("timeout")) {
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(2000); // 2s delay before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
                break; // Permanent error, stop retrying
            }
        }
        
        System.err.println("[EmailService] Échec définitif de l'envoi à " + toAddress + " après " + attempt + " tentatives.");
        if (lastEx != null) lastEx.printStackTrace();
        return false;
    }
}
