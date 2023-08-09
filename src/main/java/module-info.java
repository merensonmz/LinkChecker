module com.example.linkchecker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.jsoup;
    requires java.desktop;
    requires org.xerial.sqlitejdbc;


    opens com.example.linkchecker to javafx.fxml;
    exports com.example.linkchecker;
}