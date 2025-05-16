package org.example.controller;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
public class GitController {

    /* class responsibility: uses JGit to access and manage Git repositories */

    public static void commitExtractor() {
        // Path to the local Git repository
        String repoPath = "C:\\Users\\elisa\\Desktop\\Magistrale\\ISW2\\openjpa\\.git";

        try {
            // Open the repository
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(new File(repoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            // Access the Git log
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().call();

                // Iterate over the commits
                for (RevCommit commit : commits) {
                    System.out.println("Commit: " + commit.getName());
                    System.out.println("Author: " + commit.getAuthorIdent().getName());
                    System.out.println("Date: " + commit.getAuthorIdent().getWhen());
                    System.out.println("Message: " + commit.getFullMessage());
                    System.out.println("----------------------------------");
                }
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }


}
