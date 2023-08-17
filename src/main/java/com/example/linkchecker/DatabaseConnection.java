package com.example.linkchecker;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DatabaseConnection {

    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:mydb.db";
    private final Set<String> urlsInDatabase = new HashSet<>();

    public DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection established.");
            createTable();
            loadUrlsFromDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection : " + e.getMessage());
        }
    }

    // Create a set of URLs in the database to avoid duplicate URLs in the database
    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS links (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT NOT NULL,
                status INTEGER NOT NULL
                );""";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();

        } catch (SQLException ignored) {
        }
    }
    // Get the root URL of a URL to check if the URL is already in the database or not (e.g. https://www.google.com/search?q=java -> https://www.google.com)
    private String getRootUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            String fragment = uri.getFragment();

            // Combine the components to form a normalized URL
            StringBuilder normalizedUrl = new StringBuilder();
            normalizedUrl.append(scheme).append("://").append(host);
            if (port != -1) {
                normalizedUrl.append(":").append(port);
            }
            if (path != null) {
                normalizedUrl.append(path);
            }
            if (query != null) {
                normalizedUrl.append("?").append(query);
            }
            if (fragment != null) {
                normalizedUrl.append("#").append(fragment);
            }

            return normalizedUrl.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    // Insert the URL and its status into the database if the URL is not already in the database (based on the root URL) to avoid duplicate URLs in the database
    public void insertLinkResult(String url, int status) {
        String rootUrl = getRootUrl(url);
        if (urlsInDatabase.contains(rootUrl)) {

            return;
        }

        String sql = "INSERT INTO links (url, status) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, url);
            stmt.setInt(2, status);
            stmt.executeUpdate();


        } catch (SQLException ignored) {
            throw new RuntimeException(ignored);
        }
    }
    // Load all the URLs from the database into a set to check if a URL is already in the database
    private void loadUrlsFromDatabase() {
        String sql = "SELECT url FROM links";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String url = rs.getString("url");
                String rootUrl = getRootUrl(url);
                urlsInDatabase.add(rootUrl);
            }
        } catch (SQLException ignored) {

        }
    }



}
