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

    public DatasetProcessor() {
        this.projName = ConfigurationManager.getInstance().getProperty("project.name");
    }

    public void start() throws IOException, JSONException {
        try {

            //extract data
            ReleaseInfoExtractor releaseInfoExtractor = new ReleaseInfoExtractor();
            List<Release> releases = releaseInfoExtractor.extractReleases();

            JiraController jiraController = new JiraController();
            List<JiraTicket> tickets = jiraController.extractTicketList();

            GitController gitController = new GitController();
            List<Commit> commits = gitController.extractCommits();

            // link data
            Map<String, List<Commit>> ticketToCommitsMap = linkCommitsToJiraTickets(commits, tickets);

            Printer.println(String.format("Linking complete: Found %d commits associated with %d unique bug tickets.",
                    ticketToCommitsMap.values().stream().mapToLong(List::size).sum(),
                    ticketToCommitsMap.size()
            ));

            Map<Release, List<Commit>> releaseCommits = partitionCommitsByRelease(releases, gitController);

            // Now we have a map where each release is associated with the list of commits
            // that were made during that release's development cycle.
            // The next step would be to iterate over this map to perform the analysis.

            List<GitTag> tagList = gitController.extractTags();

        } catch (IOException | JSONException | GitAPIException e) {
            Printer.errorPrint("Somethimg went wrong while extracting data.");
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
        String dir = "src/main/outputFiles";

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


    /**
     * Associates commits with their respective releases by matching JIRA releases to Git tags
     * and partitioning the commit history.
     *
     * @param jiraReleases The list of releases from JIRA.
     * @param gitController An instance of the GitController to interact with the repository.
     * @return A map where each Release is a key for a list of Commits in that release cycle.
     */
    private Map<Release, List<Commit>> partitionCommitsByRelease(List<Release> jiraReleases, GitController gitController) throws IOException, GitAPIException {

        //matches Git tags to releases extracted by Jira

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

}
