package org.example.controller;

import org.example.entity.JiraTicket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class JiraController {

    /* Extracts info about issues using the Jira Rest API

    * bugs to be considered for the analysis:
    *       Jira tickets like [issuetype = Bug AND status in (Resolved, Closed) AND resolution = Fixed]
    * bugs to be excluded: bugs without an affect version (pre-release), bugs devoid of related fix commit on git
    */

    private static List<JiraTicket> tickets;
    private static String projName ="OPENJPA";


    public static List<JiraTicket> extractTicketList() throws IOException {
        //extracts all tickets regarding the release identified by releaseID


        tickets = new ArrayList<>();
        Integer i;

        //new url directly filters tickets

        String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project="    //basic rest api search
                + projName +
                "%20AND%20issuetype=Bug%20AND%20status%20in(Resolved,Closed)%20AND%20resolution=Fixed\n";   //required filters on tickets

        JSONObject json = readJsonFromUrl(url);
        JSONArray jiraIssues = json.getJSONArray("issues");

        for (i = 0; i < jiraIssues.length(); i++ ) {
            String name = "";
            String issueId = "";
            String resolution = "";
            String comment = "";
            List<String> affectVersions = new ArrayList<>();

            JiraTicket ticket = new JiraTicket();
            if(jiraIssues.getJSONObject(i).has("key")) {


                //basic Jira Ticket fields
                if (jiraIssues.getJSONObject(i).has("key"))
                    issueId = jiraIssues.getJSONObject(i).get("key").toString();
                if (jiraIssues.getJSONObject(i).has("fields.status.name"))
                    name = jiraIssues.getJSONObject(i).getJSONObject("status").getString("name");
                if (jiraIssues.getJSONObject(i).has("resolution") && !jiraIssues.getJSONObject(i).isNull("resolution")) {
                    resolution = jiraIssues.getJSONObject(i).getJSONObject("resolution").getString("name");
                }

                //creation and resolution date of the issue
                String created = jiraIssues.getJSONObject(i).getString("created");
                String resolved = jiraIssues.getJSONObject(i).optString("resolutiondate", "N/A");

                // Affect versions
                JSONArray versions = jiraIssues.getJSONObject(i).getJSONArray("versions");
                for (int j = 0; j < versions.length(); j++) {
                    affectVersions.add(versions.getJSONObject(j).getString("name"));
                }

                //comment
                JSONArray comments = jiraIssues.getJSONObject(i).getJSONObject("comment").getJSONArray("comments");
                for (int c = 0; c < comments.length(); c++) {
                    comment = comments.getJSONObject(c).getString("body");
                }

                //only consider bugs that have affect versions
                if (!affectVersions.isEmpty()){
                    tickets.add(new JiraTicket(issueId, name, resolution,
                            created, resolved, affectVersions, comment));
                }
            }
        }

        return null;
    }


    public void printTicketsToCSV(){

        int numTickets = tickets.size();
        int i;
        String outname = projName + "VersionInfo.csv";
        String dir = "src/main/outputFiles";


        /* todo order tickets by date
        tickets.sort(new Comparator<>() {
            //@Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
        });

         */

        if (tickets.size() < 6)
            return;
        FileWriter fileWriter = null;

        try {
            fileWriter = null;

            //Name of CSV for output, directory where it will be saved
            fileWriter = new FileWriter(new File (dir, outname));

            //csv file columns
            fileWriter.append("Index,Name,ResolutionStatus,CreationDate,ResolutionDate,Comment,AffectVersions");
            fileWriter.append("\n");


            for ( i = 0; i <= tickets.size(); i++) {

                Integer index = i + 1;
                fileWriter.append(index.toString());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getIssueId());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getName());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getResolution());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getCreationDate());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getResolutionDate());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getDescription());
                fileWriter.append(",");
                fileWriter.append(tickets.get(i).getAffectVersions().toString());
                fileWriter.append(",");
            }

        } catch (Exception e) {
            System.out.println("Error in csv writer");
            e.printStackTrace();
        } finally {
            try {
                assert fileWriter != null;
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
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
