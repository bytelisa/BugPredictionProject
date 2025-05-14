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

    private List<JiraTicket> tickets;

    public List<JiraTicket> extractTicketsByRelease(int releaseID) throws IOException {
        //extracts all tickets regarding the release identified by releaseID

        String projName ="OPENJPA";

        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        tickets = new ArrayList<>();
        Integer i;
        //String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName; // old url, less specific

        //new url directly filters tickets
        String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=" + projName +
                "AND issuetype=Bug AND status in (Resolved, Closed) AND resolution=Fixed&maxResults=1000\n";
        JSONObject json = readJsonFromUrl(url);
        JSONArray jiraIssues = json.getJSONArray("issue");

        for (i = 0; i < jiraIssues.length(); i++ ) {
            String name = "";
            String issueId = "";
            JiraTicket ticket = new JiraTicket();
            if(jiraIssues.getJSONObject(i).has("key")) {
                /*todo  FINIRE QUI   potrei fare direttamente qui il controllo su affect version (e poi successivamente su commit hash) in modo da escludere quelli che non mi interessano
                todo setta tutto con ticket.setid(jiraIssues...)*/

                if (jiraIssues.getJSONObject(i).has("key"))
                    issueId = jiraIssues.getJSONObject(i).get("key").toString();
                if (jiraIssues.getJSONObject(i).has("fields.status.name"))
                    name = jiraIssues.getJSONObject(i).get("name").toString();//id of the Jira issue
                if (jiraIssues.getJSONObject(i).has("name"))
                    name = jiraIssues.getJSONObject(i).get("name").toString();
                if (jiraIssues.getJSONObject(i).has("name"))
                    name = jiraIssues.getJSONObject(i).get("name").toString();
                if (jiraIssues.getJSONObject(i).has("name"))
                    name = jiraIssues.getJSONObject(i).get("name").toString();
                tickets.add(new JiraTicket());
            }
        }


        return null;
    }


    /* extract json object from url*/
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
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
