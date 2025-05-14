package org.example;

import org.example.controller.ReleaseInfoExtractor;
import org.json.JSONException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        try {
            ReleaseInfoExtractor.extractReleases();
        } catch (IOException | JSONException e) {
            System.out.println("Cannot extract release data.");
        }
    }
}