
package com.example.linkchecker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 833, 557);
        stage.setResizable(false);
        stage.setTitle("Link Checker!");
        stage.setScene(scene);
        stage.show();

        Controller controller = fxmlLoader.getController(); // Get a reference to the controller
        controller.setMainStage(stage); // Pass the main stage reference to the controller
    }

    public static void main(String[] args) {
        launch();
    }
}