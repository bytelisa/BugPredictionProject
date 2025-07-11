package org.example;

import org.example.controller.DatasetProcessor;
import org.example.util.Printer;
import org.json.JSONException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        try {
            DatasetProcessor datasetProcessor = new DatasetProcessor();
            datasetProcessor.extractData();
        } catch (IOException | JSONException e) {
            Printer.errorPrint("Cannot extract release data.");
        }
    }
}