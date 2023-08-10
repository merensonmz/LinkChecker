package com.example.linkchecker;

import java.awt.Desktop;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class Controller {
    DatabaseConnection databaseConnection = new DatabaseConnection();

    @FXML
    private Button button = new Button();

    @FXML
    private TextField textField = new TextField();

    @FXML
    private ListView<String> okListView = new ListView<>();

    @FXML
    private ListView<String> notOkListView = new ListView<>();

    private final Set<String> visitedUrls = new HashSet<>();

    private final List<String> okLinks = new ArrayList<>();
    private final List<String> notOkLinks = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);



    @FXML
    private void initialize() {


        notOkListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setBackground(Color.web("#E1D7D7"));
                } else {
                    setText(item);

                    setBackground(Color.web("#E1D7D7"));
                }

            }

            private void setBackground(Color color) {
                String hexColor = String.format("#%02X%02X%02X",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));

                setStyle("-fx-background-color: " + hexColor + ";");
            }
        });

        okListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setBackground(Color.web("#E1D7D7"));
                } else {
                    setText(item);

                    setBackground(Color.web("#E1D7D7"));
                }
            }

            private void setBackground(Color color) {
                String hexColor = String.format("#%02X%02X%02X",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));

                setStyle("-fx-background-color: " + hexColor + ";");
            }
        });

        button.setOnAction(event -> {
            visitedUrls.clear();
            okListView.getItems().clear();
            notOkListView.getItems().clear();
            okLinks.clear();
            notOkLinks.clear();
            String urlToCheck = textField.getText();
            checkLinksInBackground(urlToCheck);
        });

        // Handle clicks on the OK links in the ListView
        okListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String url = okListView.getSelectionModel().getSelectedItem();
                openLinkInBrowser(url);
            }
        });

        // Handle clicks on the not-OK links in the ListView
        notOkListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String url = notOkListView.getSelectionModel().getSelectedItem();
                openLinkInBrowser(url);
            }
        });


    }



    // ... (previous code remains the same)
    private void checkLinksInBackground(String url) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                checkLinks(url);
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Platform.runLater(() -> {
                    okListView.getItems().addAll(okLinks);
                    notOkListView.getItems().addAll(notOkLinks);
                });
            }
        };

        executorService.submit(task);
    }

    private void openLinkInBrowser(String url) {
        //if listviews item is not null then open it in the browser.
        if (url != null) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                try {
                    URL link = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) link.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    connection.disconnect();

                    URI uri = new URI(link.getProtocol(), link.getUserInfo(), link.getHost(), link.getPort(), link.getPath(), link.getQuery(), link.getRef());
                    Desktop.getDesktop().browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Not a web URL: " + url);
            }
        }
    }

    private String normalizeURL(String urlString) {
        String normalizedUrlString = urlString;
        try {
            URL url = new URL(urlString);
            normalizedUrlString = url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return normalizedUrlString;
    }

    private void checkLinks(String url) {
        url = addHttpIfNeeded(url);
        url = normalizeURL(url); //
        try {
            URL normalizedUrl = new URL(url);
            String normalizedUrlString = normalizedUrl.toString();

            if (!visitedUrls.contains(normalizedUrlString)) {
                visitedUrls.add(normalizedUrlString);

                Connection connection = Jsoup.connect(normalizedUrlString);
                Document document = connection.get();
                int statusCode = connection.response().statusCode();
                if (!okLinks.contains(normalizedUrlString) && !notOkLinks.contains(normalizedUrlString)) {
                    if (statusCode >= 200 && statusCode < 300) {
                        okLinks.add(normalizedUrlString);
                    } else {
                        notOkLinks.add(normalizedUrlString);
                    }
                    databaseConnection.insertLinkResult(normalizedUrlString, statusCode);
                }


                Elements links = document.select("a[href]");
                for (org.jsoup.nodes.Element link : links) {
                    String innerUrl = link.absUrl("href");
                    if (!innerUrl.isEmpty() && !innerUrl.equals("javascript:void(0)")) {
                        boolean isValidLink = checkLinkValidity(innerUrl);
                        if (isValidLink) {
                            okLinks.add(innerUrl);
                        } else {
                            notOkLinks.add(innerUrl);
                        }

                        int innerStatusCode = isValidLink ? 200 : 404;
                        databaseConnection.insertLinkResult(innerUrl, innerStatusCode);
                    }
                }
            }
        } catch (MalformedURLException e) {

            String statusLink = url + " (Invalid URL)";
            notOkLinks.add(statusLink);
            databaseConnection.insertLinkResult(statusLink, 404);
        } catch (IOException e) {
            notOkLinks.add(url);
            databaseConnection.insertLinkResult(url, 404);

        }
    }

    private boolean checkLinkValidity(String url) {
        try {
            URL link = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) link.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int statusCode = connection.getResponseCode();
            connection.disconnect();
            return statusCode >= 200 && statusCode < 400; // Consider all 2xx and 3xx codes as valid
        } catch (IOException e) {
            return false;
        }
    }


    private String addHttpIfNeeded(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        } else {
            return url;
        }
    }

}




  /*  private void send404ErrorEmail(List<String> notOkLinks) {
        String host = "smtp.gmail.com";
        int port = 587;
        String username = "slmcnm273gmail.com";
        String password = "747800eren";
        String fromEmail = "slmcnm273gmail.com";
        String toEmail = "erensonmez27@gmail.com";

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
*/