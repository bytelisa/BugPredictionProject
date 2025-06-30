package org.example.controller;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.example.entity.*;
import org.example.util.ConfigurationManager;
import org.example.util.Pair;
import org.example.util.Printer;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatasetProcessor {

    /**
     * Class responsibility:
     * processing data received by the other controllers
     * printing the final csv file
     */
    private final String projName;
    private static final Pattern JIRA_ID_PATTERN = Pattern.compile("([A-Z]+-\\d+)");
    private static final Logger LOGGER = Logger.getLogger(ProportionController.class.getName());


    public DatasetProcessor() {
        this.projName = ConfigurationManager.getInstance().getProperty("project.name");
    }

    public void extractData() throws IOException, JSONException {
        GitController gitController = null;
        try {

            //extract data
            ReleaseController releaseController = new ReleaseController();
            List<Release> releases = releaseController.extractReleases();

            JiraController jiraController = new JiraController();
            List<JiraTicket> tickets = jiraController.extractTicketList(releases);

            gitController = new GitController();
            List<Commit> commits = gitController.extractCommits();

            // link data
            Map<String, List<Commit>> ticketToCommitsMap = linkCommitsToJiraTickets(commits, tickets);

            Printer.println(String.format("Linking complete: Found %d commits associated with %d unique bug tickets.",
                    ticketToCommitsMap.values().stream().mapToLong(List::size).sum(),
                    ticketToCommitsMap.size()
            ));

            Map<Release, List<Commit>> releaseCommits = partitionCommitsByRelease(releases, gitController);
            Printer.println(String.format("Partitioning complete: Assigned commits to %d releases.", releaseCommits.size()));

            dumpPartitioningResults(releaseCommits, this.projName);

            // Now we have a map where each release is associated with the list of commit that were made during that release's development cycle.

            List<GitTag> tagList = gitController.extractTags();

            // Proportion technique to estimate IV and AVs
            ProportionController proportion = new ProportionController();
            proportion.applyProportion(tickets, releases);
            jiraController.printTicketsToCSV(tickets);  //update tickets and versions

        } catch (IOException | JSONException | GitAPIException e) {
            Printer.errorPrint("Somethimg went wrong while extracting data.");
        } finally {
            if (gitController != null) {
                gitController.close();
            }
        }
    }



    public Map<String, List<Commit>> linkCommitsToJiraTickets(List<Commit> commits, List<JiraTicket> tickets) {

        //hashset to lookup ticket id faster
        Set<String> bugTicketKeys = tickets.stream()
                .map(JiraTicket::getName)
                .collect(Collectors.toSet());

        Map<String, List<Commit>> ticketCommitsMap = new HashMap<>();

        for (Commit commit : commits) {

            //matching for a JIRA ticket id pattern
            Matcher matcher = JIRA_ID_PATTERN.matcher(commit.getMessage());

            while (matcher.find()) {
                String ticketId = matcher.group(1);

                // only care about this commit if it references one of the bug tickets we fetched
                if (bugTicketKeys.contains(ticketId)) {

                    // If the key `ticketId` is not in the map, it creates a new ArrayList,
                    // puts it in the map, and then `add(commit)` is called on that new list.
                    // If the key is already there, it simply gets the existing list and adds the commit.
                    ticketCommitsMap.computeIfAbsent(ticketId, k -> new ArrayList<>()).add(commit);
                }
            }
        }
        dumpLinkingResults(ticketCommitsMap, this.projName);

        return ticketCommitsMap;
    }


    private void dumpLinkingResults(Map<String, List<Commit>> ticketToCommitsMap, String projName) {

        String outname = projName + "LinkingValidation.csv";
        String dir = "src/main/outputFiles/" + projName;

        try (FileWriter writer = new FileWriter(new File(dir, outname))) {
            writer.append("TicketID,CommitHash,CommitMessage\n");

            // Iteration through the hash map: for each ticket ID, select all commits that are associated to it and print them
            for (Map.Entry<String, List<Commit>> entry : ticketToCommitsMap.entrySet()) {
                String ticketId = entry.getKey();
                List<Commit> commits = entry.getValue();

                for (Commit commit : commits) {
                    writer.append(ticketId);
                    writer.append(",");
                    writer.append(commit.getCommitID());
                    writer.append(",");

                    // Cleans the commit message for CSV compatibility
                    String cleanMessage = commit.getMessage().replace("\n", " ").replace("\"", "\"\"");
                    writer.append("\"").append(cleanMessage).append("\"");
                    writer.append("\n");
                }
            }
        } catch (IOException e) {
            Printer.errorPrint("Failed to write linking validation file: " + e.getMessage());
        }
        Printer.println("Linking validation results saved to: " + dir + "/" + outname);
    }



    private Map<Release, List<Commit>> partitionCommitsByRelease(List<Release> jiraReleases, GitController gitController) throws IOException, GitAPIException {
        //Associates commits with their respective releases by matching JIRA releases to Git tags
        //       returns a map where each Release is a key for a list of Commits in that release cycle

        List<GitTag> allTags = gitController.extractTags();
        Map<Release, List<Commit>> releaseCommits = new LinkedHashMap<>(); // LinkedHashMap preserves insertion order

        // Match JIRA Releases to Git Tags
        List<Pair<Release, GitTag>> matchedPairs = new ArrayList<>();
        for (Release release : jiraReleases) {

            //finds a tag whose name is contained within the JIRA release name, or vice versa
            // e.g., JIRA "Version 2.1.0" matches Git tag "2.1.0".

            allTags.stream()
                    .filter(tag -> release.getName().contains(tag.getName()) || tag.getName().contains(release.getName()))
                    .findFirst()
                    .ifPresent(tag -> matchedPairs.add(new Pair<>(release, tag)));
        }

        // Sort by the commit date for chronological order
        matchedPairs.sort(Comparator.comparing(p -> p.getRight().getCommitDate()));

        // Partition Commits using git log ranges
        ObjectId previousReleaseCommitId = null;
        for (Pair<Release, GitTag> pair : matchedPairs) {
            Release currentRelease = pair.getLeft();
            ObjectId currentReleaseCommitId = pair.getRight().getCommitId();

            List<Commit> commitsInReleaseCycle;
            if (previousReleaseCommitId == null) {
                commitsInReleaseCycle = gitController.getCommitsInRange(null, currentReleaseCommitId);
            } else {
                commitsInReleaseCycle = gitController.getCommitsInRange(previousReleaseCommitId, currentReleaseCommitId);
            }

            releaseCommits.put(currentRelease, commitsInReleaseCycle);
            previousReleaseCommitId = currentReleaseCommitId;
        }

        return releaseCommits;
    }

    private void dumpPartitioningResults(Map<Release, List<Commit>> releaseCommits, String projName) {
        String outname = projName + "_PartitioningValidation.csv";
        String dir = "src/main/outputFiles/" + projName;


        try (FileWriter writer = new FileWriter(new File(dir, outname))) {
            // Write the header of the CSV file
            writer.append("ReleaseName,ReleaseDate,CommitHash,CommitDate,CommitMessage\n");

            // ITERATE OVER THE MAP'S ENTRY SET for efficiency.
            for (Map.Entry<Release, List<Commit>> entry : releaseCommits.entrySet()) {
                Release release = entry.getKey();
                List<Commit> commits = entry.getValue();

                // If a release has no commits, you might still want to log it to see it was processed
                if (commits.isEmpty()) {
                    writer.append(release.getName());
                    writer.append(",");
                    writer.append(release.getDate().toString());
                    writer.append(",NO_COMMITS,N/A,N/A\n");
                    continue; // Go to the next release
                }

                for (Commit commit : commits) {
                    writer.append(release.getName());
                    writer.append(",");
                    writer.append(release.getDate().toString());
                    writer.append(",");
                    writer.append(commit.getCommitID());
                    writer.append(",");
                    writer.append(commit.getDate().toString());
                    writer.append(",");

                    // Clean the commit message for CSV compatibility
                    String cleanMessage = commit.getMessage().replace("\n", " ").replace("\"", "\"\"");
                    writer.append("\"").append(cleanMessage).append("\"");
                    writer.append("\n");
                }
            }
        } catch (IOException e) {
            Printer.println("Failed to write partitioning validation file: " + e.getMessage());
        }
        Printer.println("Partitioning validation results saved to: " + dir + "/" + outname);
    }

}
