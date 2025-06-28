package org.example.entity;

import java.time.Instant;

public class Commit {

    String commitID;
    String author;
    Instant date;
    String message;

    public Commit(String id, String name, Instant date, String mess){
        this.commitID=id;
        this.author = name;
        this.date = date;
        this.message = mess;
    }

    public String getCommitID() {
        return commitID;
    }

    public void setCommitID(String commitID) {
        this.commitID = commitID;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date.toString();
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
