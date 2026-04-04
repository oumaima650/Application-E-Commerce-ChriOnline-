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
        try {
            Session session = getEmailSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.getEmailUsername()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setText(messageContent);

            Transport.send(message);
            System.out.println("[EmailService] Email envoyé avec succès à " + toAddress);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EmailService] Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
