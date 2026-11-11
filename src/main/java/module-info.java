module com.example.financeapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;


    opens com.example.financeapp to javafx.fxml;
    exports com.example.financeapp;
}
