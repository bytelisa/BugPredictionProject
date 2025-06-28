package org.example.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.example.util.ConfigurationManager;
import org.example.util.Printer;
import org.example.entity.Commit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GitController {

    /* class responsibility: uses JGit to access and manage Git repositories */
    private static String projName;


    public static void commitExtractor() {

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

            //access to git log to read commits
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().call();

                for (RevCommit commit : commits) {

                    //create new commit and add it to the list of commits
                    commitList.add(new Commit(commit.getName(), commit.getAuthorIdent().getName(),
                            commit.getAuthorIdent().getWhenAsInstant(), commit.getShortMessage()));
                    /*
                    System.out.println("Commit: " + commit.getName());
                    System.out.println("Author: " + commit.getAuthorIdent().getName());
                    System.out.println("Date: " + commit.getAuthorIdent().getWhen());
                    System.out.println("Message: " + commit.getFullMessage());
                    System.out.println("----------------------------------");

                     */
                }

                printCommitsToCSV(commitList);
            }
        } catch (IOException | GitAPIException e) {
            Printer.errorPrint("Error while extracting commits.");
        }
    }

    public static void printCommitsToCSV(List<Commit> commits ){

        int i;
        String outname = projName + "Commits.csv";
        String dir = "src/main/outputFiles";

        try (FileWriter fileWriter = new FileWriter(new File (dir, outname))) {

            //csv file columns
            fileWriter.append("Commit,Author,Date,Message");
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
                fileWriter.append(commits.get(i).getMessage());
                fileWriter.append(",");
                fileWriter.append("\n");
            }

        } catch (IOException e) {
            Printer.println("Error in csv writer");
        }
    }


}
