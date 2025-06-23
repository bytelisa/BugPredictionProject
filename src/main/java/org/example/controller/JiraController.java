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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JiraController {

    /* Extracts info about issues using the Jira Rest API

    * bugs to be considered for the analysis:
    *       Jira tickets like [issuetype = Bug AND status in (Resolved, Closed) AND resolution = Fixed]
    * bugs to be excluded: bugs without an affect version (pre-release), bugs devoid of related fix commit on git
    */

    private static List<JiraTicket> tickets;
    private static String projName;


    public static List<JiraTicket> extractTicketList() throws IOException {
        //extracts all tickets regarding the release identified by releaseID

        projName = ConfigurationManager.getInstance().getProperty("project.name");
        tickets = new ArrayList<>();
        int i;


        //url directly filters tickets
        String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project="    //basic rest api search
                + projName +
                "%20AND%20issuetype=Bug%20AND%20status%20in(Resolved,Closed)%20AND%20resolution=Fixed";   //required filters on tickets

        JSONObject json = readJsonFromUrl(url);
        JSONArray jiraIssues = json.getJSONArray("issues");

        for (i = 0; i < jiraIssues.length(); i++ ) {

            String name = "";
            String issueId = "";
            String resolution = "";
            String comment = "";
            List<String> affectVersions = new ArrayList<>();
            List<String> fixVersions = new ArrayList<>();
            JSONObject issue = jiraIssues.getJSONObject(i);
            JSONObject fields = issue.getJSONObject("fields");

            //only consider bugs that have affect versions
            if(jiraIssues.getJSONObject(i).has("key") && (fields.has("versions"))  && !fields.getJSONArray("versions").isEmpty()) {

                //get basic Jira Ticket fields
                if (jiraIssues.getJSONObject(i).has("id"))
                    issueId = jiraIssues.getJSONObject(i).get("id").toString();
                if (jiraIssues.getJSONObject(i).has("key"))
                    name = jiraIssues.getJSONObject(i).get("key").toString();
                if (fields.has("resolution") && !fields.isNull("resolution")) {
                    resolution = fields.getJSONObject("resolution").getString("name");
                }

                /*creation and resolution date of the issue
                if (jiraIssues.getJSONObject(i).has("created")){
                    String created = jiraIssues.getJSONObject(i).get("created").toString();
                }

                if (jiraIssues.getJSONObject(i).has("resolutiondate")){
                    String resolved = jiraIssues.getJSONObject(i).get("resolutiondate").toString();
                }

                 */

                // Affect versions
                JSONArray versions = fields.getJSONArray("versions");
                for (int j = 0; j < versions.length(); j++) {
                    affectVersions.add(versions.getJSONObject(j).getString("name"));
                }

                // Fix versions
                JSONArray fVersions = fields.getJSONArray("fixVersions");
                for (int j = 0; j < fVersions.length(); j++) {
                    fixVersions.add(fVersions.getJSONObject(j).getString("name"));
                }

                //comment
                if (fields.has("comment")) {
                    JSONArray comments = fields.getJSONObject("comment").getJSONArray("comments");
                    if (!comments.isEmpty()) {
                        comment = comments.getJSONObject(0).getString("body"); // solo primo commento
                    }
                }

                tickets.add(new JiraTicket(issueId, name, resolution, fixVersions, affectVersions, comment));

            }
        }
        printTicketsToCSV();
        return tickets;
    }


    public static void printTicketsToCSV(){

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


    /* extract json object from url*/
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
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
