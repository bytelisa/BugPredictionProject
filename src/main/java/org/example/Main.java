package org.example;

import org.example.controller.GitController;
import org.example.controller.JiraController;
import org.example.controller.ReleaseInfoExtractor;
import org.json.JSONException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        try {
            ReleaseInfoExtractor.extractReleases();
            JiraController.extractTicketList();
            GitController.commitExtractor();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            System.out.println("Cannot extract release data.");
        }
    }
}