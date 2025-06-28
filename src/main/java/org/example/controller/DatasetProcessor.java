package org.example.controller;

import org.example.util.Printer;
import org.json.JSONException;
import java.io.IOException;

public class DatasetProcessor {

    /** Class responsibility:
     * processing data received by the other controllers
     * printing the final csv file
     */

    public void start() throws IOException, JSONException {
        try {
            ReleaseInfoExtractor releaseInfoExtractor = new ReleaseInfoExtractor();
            releaseInfoExtractor.extractReleases();

            JiraController jiraController = new JiraController();
            jiraController.extractTicketList();

            GitController gitController = new GitController();
            gitController.commitExtractor();

        } catch (IOException | JSONException e) {
            Printer.errorPrint("Somethimg went wrong while extracting data.");
        }
    }


}
