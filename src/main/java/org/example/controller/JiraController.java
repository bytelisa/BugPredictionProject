package org.example.controller;

import org.example.entity.JiraTicket;
import org.example.util.ConfigurationManager;
import org.example.util.Printer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JiraController {

    /* Extracts info about issues using the Jira Rest API

    * bugs to be considered for the analysis:
    *       Jira tickets like [issuetype = Bug AND status in (Resolved, Closed) AND resolution = Fixed]
    * bugs to be excluded: bugs without an affect version (pre-release), bugs devoid of related fix commit on git
    */

    private static String projName;


    public static List<JiraTicket> extractTicketList() throws IOException, JSONException {
        //extracts all tickets regarding the release identified by releaseID

        projName = ConfigurationManager.getInstance().getProperty("project.name");
        List<JiraTicket> tickets = new ArrayList<>();
        int i = 0;
        int total;
        int maxResults = 100; // Impostiamo il massimo per ridurre il numero di chiamate

        do {

            //url directly filters tickets using jira rest api
            String url = String.format(
                    "https://issues.apache.org/jira/rest/api/2/search?jql=project=%s" +
                            "%%20AND%%20issuetype=Bug%%20AND%%20status%%20in(Resolved,Closed)%%20AND%%20resolution=Fixed" +
                            "&startAt=%d&maxResults=%d",
                    projName, i, maxResults
            );

            Printer.println("Fetching URL: " + url); // log for debug


            JSONObject json = readJsonFromUrl(url);
            JSONArray jiraIssues = json.getJSONArray("issues");
            total = json.getInt("total"); //total number of tickets that satisfy query

            parseIssues(jiraIssues, tickets); //auxiliary function for data extraction, reduces cognitive complexity of this method

        } while (i < total);

        Printer.println("Total tickets fetched: " + tickets.size() + " (out of " + total + " reported by JIRA)");
        printTicketsToCSV(tickets);
        return tickets;
    }



    private static void printTicketsToCSV(List<JiraTicket> tickets){

        int i;
        String outname = projName + "Tickets.csv";
        String dir = "src/main/outputFiles";


        /* todo order tickets by date
        tickets.sort(new Comparator<>() {
            //@Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
        });

         */

        FileWriter fileWriter = null;

        try {

            //Name of CSV for output, directory where it will be saved
            fileWriter = new FileWriter(new File (dir, outname));

            //csv file columns
            fileWriter.append("Index,IssueID,Name,ResolutionStatus,AffectVersions,FixVersions");
            fileWriter.append("\n");


            for ( i = 0; i < tickets.size(); i++) {
                int index = i + 1;

                //todo check why comment is empty

                fileWriter.append(Integer.toString(index));
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getIssueId());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getName());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getResolution());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getAffectVersions().toString());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getFixVersions().toString());
                fileWriter.append("\n");
            }

        } catch (Exception e) {
            Printer.println("Error in csv writer");
            e.printStackTrace();
        } finally {
            try {
                assert fileWriter != null;
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                Printer.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }



    private static void parseIssues(JSONArray issues, List<JiraTicket> tickets) {

        //parses a JSONArray of JIRA issues and turns them into JiraTicket objects

        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            JSONObject fields = issue.getJSONObject("fields");

            //todo controlla affect version successivamente, per ora stai estraendo e basta

            if (issue.has("key")) {
                String issueId = issue.getString("id");
                String name = issue.getString("key");
                String resolution = fields.has("resolution") && !fields.isNull("resolution")
                        ? fields.getJSONObject("resolution").getString("name")
                        : "";

                List<String> affectVersions = parseVersions(fields, "versions");
                List<String> fixVersions = parseVersions(fields, "fixVersions");

                String comment = "";
                if (fields.has("comment") && fields.getJSONObject("comment").has("comments")) {
                    JSONArray comments = fields.getJSONObject("comment").getJSONArray("comments");
                    if (!comments.isEmpty()) {
                        comment = comments.getJSONObject(0).optString("body", ""); // Get first comment's body
                    }
                }

                tickets.add(new JiraTicket(issueId, name, resolution, fixVersions, affectVersions, comment));
            }
        }
    }

    private static List<String> parseVersions(JSONObject fields, String versionType) {

        //Helper method to parse version arrays from JIRA fields
        List<String> parsedVersions = new ArrayList<>();

        if (fields.has(versionType)) {
            JSONArray versions = fields.getJSONArray(versionType);

            for (int j = 0; j < versions.length(); j++) {
                JSONObject version = versions.getJSONObject(j);

                if (version.has("name")) {
                    parsedVersions.add(version.getString("name"));
                }
            }
        }
        return parsedVersions;
    }

    public void printTicketsToCSV(List<JiraTicket> ticketsToPrint, String projName) {

        String outname = projName + "Tickets.csv";
        String dir = "outputFiles";

        try (FileWriter fileWriter = new FileWriter(new File(dir, outname))) {

            // CSV header
            fileWriter.append("IssueID,Name,ResolutionStatus,AffectVersions,FixVersions\n");

            for (JiraTicket ticket : ticketsToPrint) {
                fileWriter.append(ticket.getIssueId());
                fileWriter.append(",");
                fileWriter.append(ticket.getName());
                fileWriter.append(",");
                fileWriter.append(ticket.getResolution());
                fileWriter.append(",");
                fileWriter.append("\"").append(String.join(";", ticket.getAffectVersions())).append("\"");
                fileWriter.append(",");
                fileWriter.append("\"").append(String.join(";", ticket.getFixVersions())).append("\"");
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            Printer.println("Error writing tickets to CSV file: " + e.getMessage());
        }
    }


    private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


}
