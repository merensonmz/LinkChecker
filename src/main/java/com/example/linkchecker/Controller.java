        package com.example.linkchecker;

        import java.awt.Desktop;

        import javafx.application.Platform;
        import javafx.concurrent.Task;
        import javafx.scene.control.ListCell;
        import javafx.scene.input.MouseButton;
        import javafx.scene.layout.VBox;
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
        import java.time.Duration;
        import java.time.LocalTime;
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

        import java.util.concurrent.ScheduledExecutorService;
        import java.util.concurrent.TimeUnit;

        public class Controller {
            private boolean emailSent = false;
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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
            private final Set<String> visitedUrlsInTime = new HashSet<>();

            private final List<String> okLinks = new ArrayList<>();
            private final List<String> notOkLinks = new ArrayList<>();

            private final List<String> okLinksInTime = new ArrayList<>();

            private final List<String> notOkLinksInTime = new ArrayList<>();

            private final ExecutorService executorService = Executors.newFixedThreadPool(10);

            private void showPopup() {
                VBox root = new VBox();
                root.setStyle("-fx-background-color: #E1D7D7;");
                root.setPrefSize(300, 200);

            }


            @FXML
            private void initialize() {


                // Set up the ListView cell factory to change the background color of the cell
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

                // Set up the ListView cell factory to change the background color of the cell
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

                // Handle clicks on the button
                button.setOnAction(event -> {
                    if (textField.getText().isEmpty()) {
                        return;
                    }
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

                startScheduledTask();
            }



            private long calculateInitialDelay() {
                long now = System.currentTimeMillis();
                long targetTime = getTargetTime();

                if (now < targetTime) {
                    return targetTime - now;
                } else {
                    return (24 * 60 * 60 * 1000) - (now - targetTime);
                }
            }
            private long getTargetTime() {
                LocalTime targetTime = LocalTime.of(10, 49);
                LocalTime now = LocalTime.now();
                System.out.println("now: " + now);
                if (now.isAfter(targetTime)) {
                    targetTime = targetTime.plusNanos(1); // Move to the same time on the next day
                }

                long millisUntilTarget = TimeUnit.HOURS.toMillis(targetTime.getHour()) +
                        TimeUnit.MINUTES.toMillis(targetTime.getMinute()) -
                        TimeUnit.SECONDS.toMillis(targetTime.getSecond()) -
                        TimeUnit.MILLISECONDS.toMillis(targetTime.getNano() / 1000000);

                return millisUntilTarget;
            }


            private void runScheduledTask() {
                LocalTime targetTime = LocalTime.of(10, 49);
                LocalTime now = LocalTime.now();
                EmailSender emailSender = new EmailSender();

                if (now.isAfter(targetTime)) {
                    targetTime = targetTime.plusHours(24); // Move to the same time on the next day
                }

                long delayMillis = Duration.between(now, targetTime).toMillis();

                scheduler.schedule(() -> {
                    List<String> urlsInTime = new ArrayList<String>();


                    for (String url : urlsInTime) {
                        System.out.println("Checking links in background thread...");
                        checkLinkswithTime(url);
                    }

                    if (!notOkLinksInTime.isEmpty()) {
                        try {
                            emailSender.send404ErrorEmail(notOkLinksInTime, "rapor@aryomyazilim.com.tr");
                            emailSent = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, delayMillis, TimeUnit.MILLISECONDS);
            }


            private void startScheduledTask() {
                long initialDelayMillis = calculateInitialDelay();

                if (!emailSent) {
                    scheduler.scheduleWithFixedDelay(this::runScheduledTask, initialDelayMillis, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
                    System.out.println("Scheduled task started.");
                }
            }
            //check links in background thread and add them to the listviews in the main thread
            private void checkLinksInBackground(String url) {
                EmailSender emailSender = new EmailSender();
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

                            if (!notOkLinks.isEmpty()) {
                                try {
                                    // Send notOkLinks to email
                                    emailSender.send404ErrorEmail(notOkLinks, "rapor@aryomyazilim.com.tr");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };

                executorService.submit(task);
            }
            //check links and add them to the listviews
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
            //
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

            //Check links if they are ok or not ok and add them to the listviews and database table
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
            private void checkLinkswithTime(String url) {
                url = addHttpIfNeeded(url);
                url = normalizeURL(url); //
                try {
                    URL normalizedUrl = new URL(url);
                    String normalizedUrlString = normalizedUrl.toString();

                    if (!visitedUrlsInTime.contains(normalizedUrlString)) {
                        visitedUrlsInTime.add(normalizedUrlString);

                        Connection connection = Jsoup.connect(normalizedUrlString);
                        Document document = connection.get();
                        int statusCode = connection.response().statusCode();
                        if (!okLinksInTime.contains(normalizedUrlString) && !notOkLinksInTime.contains(normalizedUrlString)) {
                            if (statusCode >= 200 && statusCode < 300) {
                                okLinksInTime.add(normalizedUrlString);
                            } else {
                                notOkLinksInTime.add(normalizedUrlString);
                            }
                        }

                        Elements links = document.select("a[href]");
                        for (org.jsoup.nodes.Element link : links) {
                            String innerUrl = link.absUrl("href");
                            if (!innerUrl.isEmpty() && !innerUrl.equals("javascript:void(0)")) {
                                boolean isValidLink = checkLinkValidity(innerUrl);
                                if (isValidLink) {
                                    okLinksInTime.add(innerUrl);
                                } else {
                                    notOkLinksInTime.add(innerUrl);
                                }
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    String statusLink = url + " (Invalid URL)";
                    notOkLinksInTime.add(statusLink);
                } catch (IOException e) {
                    notOkLinksInTime.add(url);
                }
            }

            //check link validity if it is valid or not valid and return boolean value in order to status code

            private boolean checkLinkValidity(String url) {
                try {
                    URL link = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) link.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int statusCode = connection.getResponseCode();
                    return statusCode >= 200 && statusCode < 404; // Consider all 2xx and 3xx codes as valid
                } catch (IOException e) {
                    return false;
                }
            }



            //add http if it is not added
            private String addHttpIfNeeded(String url) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return "http://" + url;
                } else {
                    return url;
                }
            }

        }

