module com.quaxboard.app {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.quaxboard.app to javafx.fxml;
    exports com.quaxboard.app;
}