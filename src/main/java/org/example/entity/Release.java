package org.example.entity;

import java.time.Instant;

public class Release {
    private final String id;
    private final String name;
    private final Instant date;

    public Release(String id, String name, Instant date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Instant getDate() { return date; }

    @Override
    public String toString() {
        return "Release{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", date=" + date +
                '}';
    }
}