package org.example.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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


    public List<Commit> extractCommits() {

        projName = ConfigurationManager.getInstance().getProperty("project.name");
        String gitPath = ConfigurationManager.getInstance().getProperty("git.path");

        List<Commit> commitList = new ArrayList<>();

        try {

            //opens the repository
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(new File(gitPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            this.git = new Git(repository);

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

    public List<GitTag> extractTags()throws GitAPIException, IOException {
        List<GitTag> tags = new ArrayList<>();
        List<Ref> call = this.git.tagList().call();

        //we use a revwalk to get the commit date from the tag's id
        try (RevWalk walk = new RevWalk(this.git.getRepository())) {

            for (Ref ref : call) {

                RevCommit commit = walk.parseCommit(ref.getObjectId()); //commit associated with the tag
                String tagName = ref.getName().replace("refs/tags/", ""); //tag name cleanup

                tags.add(new GitTag(tagName, commit.getId(), commit.getAuthorIdent().getWhenAsInstant()));
            }
        }
        return tags;
    }

}
