package org.example.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.example.entity.GitTag;
import org.example.util.ConfigurationManager;
import org.example.util.Printer;
import org.example.entity.Commit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;

public class GitController {

    /** Class responsibility: use JGit lib to access and manage Git repositories. Thus:
     *      - extract commits
     *      - extract tags
     *      */


    private String projName;
    private Git git;
    private Repository repository;


    public List<Commit> extractCommits() {

        projName = ConfigurationManager.getInstance().getProperty("project.name");
        String gitPath = ConfigurationManager.getInstance().getProperty("git.path");

        List<Commit> commitList = new ArrayList<>();

        try {

            //opens the repository
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
             this.repository = builder.setGitDir(new File(gitPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            this.git = new Git(this.repository);

            //access to git log to read commits
            Iterable<RevCommit> commits = git.log().call();

            for (RevCommit commit : commits) {

                //create new Commit and add it to the list of commits
                commitList.add(new Commit(commit.getName(), commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getWhenAsInstant(), commit.getFullMessage()));
            }

            printCommitsToCSV(commitList);

        } catch (IOException | GitAPIException e) {
            Printer.errorPrint("Error while extracting commits.");
        }
        return commitList;
    }

    public void printCommitsToCSV(List<Commit> commits ){

        int i;
        String outname = projName + "Commits.csv";
        String dir = "src/main/outputFiles";

        try (FileWriter fileWriter = new FileWriter(new File (dir, outname))) {

            //csv file columns
            fileWriter.append("Index,CommitID,Author,Date,Message");
            fileWriter.append("\n");

            for ( i = 0; i < commits.size(); i++){
                int index = i+1;

                fileWriter.append(Integer.toString(index));
                fileWriter.append(",");
                fileWriter.append(commits.get(i).getCommitID());
                fileWriter.append(",");
                fileWriter.append(commits.get(i).getAuthor());
                fileWriter.append(",");
                fileWriter.append(commits.get(i).getDate());
                fileWriter.append(",");
                String message = commits.get(i).getMessage().replace("\n", " ").replace(",", ";"); // comment cleanup for csv
                fileWriter.append("\"").append(message).append("\"");
                fileWriter.append(",");
                fileWriter.append("\n");
                fileWriter.flush();

            }

        } catch (IOException e) {
            Printer.println("Error in csv writer");
        }
    }

    public List<GitTag> extractTags() {
        List<GitTag> tags = new ArrayList<>();

        try {
            List<Ref> call = this.git.tagList().call();

            //we use a revwalk to get the commit date from the tag's id
            RevWalk walk = new RevWalk(this.git.getRepository());
            for (Ref ref : call) {

                RevCommit commit = walk.parseCommit(ref.getObjectId()); //commit associated with the tag
                String tagName = ref.getName().replace("refs/tags/", ""); //tag name cleanup

                tags.add(new GitTag(tagName, commit.getId(), commit.getAuthorIdent().getWhenAsInstant()));
            }
        } catch (GitAPIException|IOException e) {
            Printer.errorPrint("Error while extracting Tags from Git.");
        }
        printTagsToCSV(tags);
        return tags;
    }

    private void printTagsToCSV(List<GitTag> tagList){

        String outname = projName + "Tags.csv";  //output file
        String dir = "src/main/outputFiles";    //output directory

        try (FileWriter fileWriter = new FileWriter(new File (dir, outname))) {
            for (GitTag tag: tagList) {
                fileWriter.append(tag.getName());
                fileWriter.append(",");
                fileWriter.append(tag.getCommitId().toString());
                fileWriter.append(",");
                fileWriter.append(tag.getCommitDate().toString());
                fileWriter.append(",");
                fileWriter.append("\n");

            }
        } catch (IOException e){
            Printer.errorPrint("Error printing Tags to CSV.");
        }

    }

    public List<Commit> getCommitsInRange(ObjectId startCommitId, ObjectId endCommitId) throws GitAPIException, IOException {

        List<Commit> commitsInRange = new ArrayList<>();
        LogCommand logCommand = this.git.log();

        if (startCommitId == null) {
            //sets the starting point of the history walk
            logCommand.add(endCommitId);
        } else {
            // .addRange() is the JGit equivalent of 'git log <start>..<end>'
            logCommand.addRange(startCommitId, endCommitId);
        }

        Iterable<RevCommit> commits = logCommand.call();

        // like getAllCommits(). this could be extracted to a private helper method
        for (RevCommit revCommit : commits) {
            commitsInRange.add(new Commit(revCommit.getName(), revCommit.getAuthorIdent().getName(),
                    revCommit.getAuthorIdent().getWhenAsInstant(), revCommit.getFullMessage()));
        }
        return commitsInRange;
    }

    /**
     * Closes the underlying Git and Repository objects to release resources.
     * Should be called when the controller is no longer needed.
     */
    public void close() {
        this.git.close();
        this.repository.close();
    }
}
