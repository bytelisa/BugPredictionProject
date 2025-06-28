package org.example.entity;

import org.eclipse.jgit.lib.ObjectId;
import java.time.Instant;

public class GitTag {
    private final String name;
    private final ObjectId commitId;
    private final Instant commitDate; // The date of the commit the tag points to

    public GitTag(String name, ObjectId commitId, Instant commitDate) {
        this.name = name;
        this.commitId = commitId;
        this.commitDate = commitDate;
    }

    // Add getters for all fields...
    public String getName() { return name; }
    public ObjectId getCommitId() { return commitId; }
    public Instant getCommitDate() { return commitDate; }
}