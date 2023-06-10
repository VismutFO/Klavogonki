package org.vismutFO.klavogonki.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public class Resource {
    private final String name;

    public Resource(String name) {
        this.name = name;
    }

    public String getContent() throws IOException {
        try (InputStream inputStream = Resource
                .class.getResourceAsStream("/" + name);
             ) {
            Objects.requireNonNull(inputStream);
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return text;
        }
    }
}
