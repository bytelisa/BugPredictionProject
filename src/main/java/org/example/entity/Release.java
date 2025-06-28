// src/main/java/org/example/entity/Release.java
package org.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Release {
    private final String id;
    private final String name;
    private final LocalDate date;

    public Release(String id, String name, LocalDate date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return "Release{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", date=" + date +
                '}';
    }
}