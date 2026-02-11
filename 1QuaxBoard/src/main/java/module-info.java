module com.example._quaxboard {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example._quaxboard to javafx.fxml;
    exports com.example._quaxboard;
}