module com.example.quiz_app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    opens com.example.quiz_app to javafx.fxml;
    exports com.example.quiz_app;
}
