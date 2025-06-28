package org.example.controller;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ReleaseInfoExtractor {

    /** Class responsibility: extract data and write it onto csv files for it to be later analyzed by classifier
    * ignores the last 66% of releases
    * uses Jira Rest API to extract data regarding releases
    */

    private HashMap<LocalDateTime, String> releaseNames;
    private HashMap<LocalDateTime, String> releaseID;
    private ArrayList<LocalDateTime> releases;
    private List<Release> releaseList;

    public List<Release> extractReleases() throws IOException, JSONException {

        String projName = ConfigurationManager.getInstance().getProperty("project.name");

        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        releases = new ArrayList<>();
        releaseNames = new HashMap<>();
        releaseID = new HashMap<>();
        releaseList = new ArrayList<>();

        //todo eliminare queste tre liste inutili per sostituirle definitivamente con la lista di release releaselist

        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions;
        versions = json.getJSONArray("versions");

        for (i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            LocalDate date;

            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                date = LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString());
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
                        name,id);
                releaseList.add(new Release(id, name, date));
            }
        }

        // order releases by date
        releases.sort(Comparator.naturalOrder());


        //output file and its directory
        String outname = projName + "VersionInfo.csv";
        String dir = "src/main/outputFiles";

        try (FileWriter fileWriter = new FileWriter(new File(dir, outname))) {

            int releasesToKeep = (int) Math.round(releases.size() * 0.34);

            if (releasesToKeep == 0) {
                Printer.println("Not enough releases to process.");
                return null;
            }

            Printer.println("Total releases found: " + releases.size());
            Printer.println("Keeping the first " + releasesToKeep + " releases (34%).");

            //csv file columns
            fileWriter.append("Index,Version ID,Version Name,Date");
            fileWriter.append("\n");

            //do not consider the last 66% of releases
            for ( i = 0; i < releasesToKeep; i++) {
                int index = i + 1;
                fileWriter.append(Integer.toString(index));
                fileWriter.append(",");
                fileWriter.append(releaseID.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releaseNames.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releases.get(i).toString());
                fileWriter.append("\n");
            }

        } catch (Exception e) {
            Printer.println("Error in csv writer");
        }
        return releaseList;
    }


    public void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
    }


    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        JSONObject jsonObj = null;
        try (InputStream is = new URI(url).toURL().openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            jsonObj = new JSONObject(jsonText);
        } catch (URISyntaxException e) {
            Printer.errorPrint("Invalid url.");
        }
        return jsonObj;
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

}
