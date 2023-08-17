package com.example.linkchecker;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    private final String host = "smtp.gmail.com";
    private final int port = 587;
    private final String username = "erensonmez27@gmail.com"; // Email address of the sender
    private final String password = "noxwqipitdmnzlxo"; // Email password of the sender
    private final String fromEmail = "erensonmez27@gmail.com"; // Email address of the sender

    //@aryomyazilim.com.tr
    // Send email to the user with the list of 404 error links as the message body of the email
    public void send404ErrorEmail(List<String> notOkLinks, String toEmail) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("404 Error Links");

            StringBuilder messageText = new StringBuilder();
            messageText.append("List of 404 Error Links:\n\n");

            for (String link : notOkLinks) {
                messageText.append(link).append("\n");
            }

            message.setText(messageText.toString());

            Transport.send(message);

            System.out.println("Email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
