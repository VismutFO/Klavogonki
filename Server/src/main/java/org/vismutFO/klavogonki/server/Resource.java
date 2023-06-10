package org.vismutFO.klavogonki.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class Resource {
    private final String name;

    public Resource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getLength() throws IOException {
        URL url = Resource.class.getResource("/" + name);
        URLConnection connection = Objects.requireNonNull(url)
                .openConnection();
        return connection.getContentLengthLong();
    }

    public InputStream getInputStream() throws IOException {
        InputStream inputStream = Resource
                .class.getResourceAsStream("/" + name);
        Objects.requireNonNull(inputStream);

        return inputStream;
    }
}
