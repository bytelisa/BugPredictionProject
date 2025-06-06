package org.example.controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ReleaseInfoExtractor {

    //class responsibility: extract data and write it onto csv files for it to be later analyzed by classifier
    //ignore the last 66% of releases
    //uses Jira Rest API to extract data regarding releases

    private static HashMap<LocalDateTime, String> releaseNames;
    private static HashMap<LocalDateTime, String> releaseID;
    private static ArrayList<LocalDateTime> releases;

    public static void extractReleases() throws IOException, JSONException {

        String projName ="OPENJPA";

        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        releases = new ArrayList<>();
        releaseNames = new HashMap<>();
        releaseID = new HashMap<>();

        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;

        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        for (i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
                        name,id);
            }
        }

        // order releases by date
        releases.sort(new Comparator<>() {
            //@Override
            public int compare(LocalDateTime o1, LocalDateTime o2) {
                return o1.compareTo(o2);
            }
        });

        if (releases.size() < 6)
            return;

        FileWriter fileWriter= null;

        try {

            //output file and its directory
            String outname = projName + "VersionInfo.csv";
            String dir = "src/main/outputFiles";

            fileWriter = new FileWriter(new File (dir, outname));

            //csv file columns
            fileWriter.append("Index,Version ID,Version Name,Date");
            fileWriter.append("\n");

            //do not consider the last 66% of releases
            for ( i = 0; i <= ((releases.size() *33)/100); i++) {
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


    public static void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
    }


    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
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
