module Client {
    requires Protocol;
    requires javafx.controls;
    requires javafx.fxml;

    opens org.vismutFO.klavogonki.client to javafx.fxml;

    exports org.vismutFO.klavogonki.client;
}