package com.quaxboard.app;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class QuaxBoardApp extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("QuaxBoard");
        Scene scene = new Scene(new StackPane(label), 800, 600);
        stage.setScene(scene);
        stage.setTitle("QuaxBoard");
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}
