module Protocol {
    requires org.json;

    opens org.vismutFO.klavogonki.protocol to Server, Client;
    exports org.vismutFO.klavogonki.protocol;
}