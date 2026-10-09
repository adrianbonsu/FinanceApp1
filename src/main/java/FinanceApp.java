module com.example.financeapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.financeapp to javafx.fxml;
    exports com.example.financeapp;
}