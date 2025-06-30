package org.example.controller;

import org.example.entity.JiraTicket;
import org.example.entity.Release;
import org.example.util.ConfigurationManager;
import org.example.util.Printer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JiraController {

    private String projName;
    private static final Logger LOGGER = Logger.getLogger(ProportionController.class.getName());

    public JiraController() {
        this.projName = ConfigurationManager.getInstance().getProperty("project.name");
    }

    public List<JiraTicket> extractTicketList(List<Release> allReleases) throws IOException, JSONException {

        //release list is needed to map ticket dates to releases

        List<JiraTicket> tickets = new ArrayList<>();
        int startPage = 0;
        int total = 0;
        int maxResults = 100;

        do {
            String url = String.format(
                    "https://issues.apache.org/jira/rest/api/2/search?jql=project=%s" +
                            "%%20AND%%20issuetype=Bug%%20AND%%20status%%20in(Resolved,Closed)%%20AND%%20resolution=Fixed" +
                            "&fields=id,key,resolution,created,versions,fixVersions,comment" +
                            "&startAt=%d&maxResults=%d",
                    projName, startPage, maxResults
            );

            Printer.println("Fetching URL: " + url);

            JSONObject json = readJsonFromUrl(url);
            if (json == null) break; //safe handling

            JSONArray jiraIssues = json.getJSONArray("issues");
            if (startPage == 0) { //only read total once
                total = json.getInt("total");
            }

            parseIssues(jiraIssues, tickets, allReleases);

            startPage += jiraIssues.length();

        } while (startPage < total);

        Printer.println("Total tickets fetched: " + tickets.size() + " (out of " + total + " reported by JIRA)");
        printTicketsToCSV(tickets);
        return tickets;
    }

    public void printTicketsToCSV(List<JiraTicket> tickets){

        String outname = projName + "Tickets.csv";
        String dir = "src/main/outputFiles/" + projName;
        new File(dir).mkdirs();

        try (FileWriter fileWriter = new FileWriter(new File(dir, outname))) {
            fileWriter.append("Index,IssueID,Name,InjectVersion,OpeningVersion,AffectedVersions,FixVersions\n");

            for (int i = 0; i < tickets.size(); i++) {

                JiraTicket ticket = tickets.get(i);

                fileWriter.append(String.valueOf(i + 1));
                fileWriter.append(",").append(ticket.getIssueId());
                fileWriter.append(",").append(ticket.getName());

                //handle possibly not knowing inject/opening version
                fileWriter.append(",").append(ticket.getInjectVersion() != null ? ticket.getInjectVersion().getName() : "N/A");
                fileWriter.append(",").append(ticket.getOpeningVersion() != null ? ticket.getOpeningVersion().getName() : "N/A");


                // uses stream().map().collect() to create a valid string for the release list
                String affectedVersionNames = ticket.getAffectedVersions().stream()
                        .map(Release::getName)
                        .collect(Collectors.joining(";"));
                fileWriter.append(",\"").append(affectedVersionNames).append("\"");

                String fixVersionNames = ticket.getFixVersions().stream()
                        .map(Release::getName)
                        .collect(Collectors.joining(";"));
                fileWriter.append(",\"").append(fixVersionNames).append("\"\n");
            }
        } catch (Exception e) {
            Printer.println("Error in csv writer - JiraController");
        }
    }

    private void parseIssues(JSONArray issues, List<JiraTicket> tickets, List<Release> allReleases) {

        //uses releases for the mapping logic

        //needed to parse Jira's specific time format
        DateTimeFormatter jiraDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            JSONObject fields = issue.getJSONObject("fields");

            if (issue.has("key")) {
                String issueId = issue.getString("id");
                String name = issue.getString("key");
                String resolution = fields.optString("resolution.name", "");


                List<Release> affectedReleases = parseReleasesFromJsonArray(fields.optJSONArray("versions"), allReleases);
                List<Release> fixReleases = parseReleasesFromJsonArray(fields.optJSONArray("fixVersions"), allReleases);

                Instant creationDate = Instant.from(jiraDateFormatter.parse(fields.getString("created")));

                Release openingVersion = ReleaseController.findReleaseByDate(creationDate, allReleases);

                Release injectedVersion = null;

                if (!affectedReleases.isEmpty()) {
                    // IV is the first one of the affected versions if they are in order
                    injectedVersion = affectedReleases.getFirst();
                }

                String comment = "";
                if (fields.has("comment") && fields.getJSONObject("comment").has("comments")) {
                    JSONArray comments = fields.getJSONObject("comment").getJSONArray("comments");
                    if (!comments.isEmpty()) {
                        comment = comments.getJSONObject(0).optString("body", ""); // Get first comment's body
                    }
                }
                tickets.add(new JiraTicket(issueId, name, resolution, comment, openingVersion, injectedVersion, fixReleases, affectedReleases));
            }
        }
    }

    private List<Release> parseReleasesFromJsonArray(JSONArray versionsArray, List<Release> allReleases) {
        //turns JSON Array of versions into a List<Release>

        List<Release> foundReleases = new ArrayList<>();
        if (versionsArray == null) return foundReleases;

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            String versionName = versionObj.optString("name");

            //finds object corresponding to name
            allReleases.stream()
                    .filter(r -> r.getName().equals(versionName))
                    .findFirst()
                    .ifPresent(foundReleases::add);
        }
        //sorting
        foundReleases.sort(java.util.Comparator.comparing(Release::getDate));
        return foundReleases;
    }

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try {
            return new JSONObject(readAll(new BufferedReader(new InputStreamReader(new URI(url).toURL().openStream(), StandardCharsets.UTF_8))));
        } catch (URISyntaxException e) {
            Printer.errorPrint("Invalid url: " + url);
            return null;
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) sb.append((char) cp);
        return sb.toString();
    }
}